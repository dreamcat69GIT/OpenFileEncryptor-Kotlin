import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.lang.IllegalArgumentException
import java.lang.Exception

object Cryptography {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 256
    private const val ITERATIONS = 65536
    private const val PROGRAM_ID = "OpenFileEncryptor"

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun generateIv(): ByteArray {
        val random = SecureRandom()
        val iv = ByteArray(16)
        random.nextBytes(iv)
        return iv
    }

    private fun createSecretKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE)
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)
    }


    fun generateKey(): String {
        val random = SecureRandom()
        val keyBytes = ByteArray(KEY_SIZE / 8)
        random.nextBytes(keyBytes)
        return keyBytes.joinToString("") { "%02x".format(it) }
    }

    fun encryptString(text: String, password: String): String {
        val salt = generateSalt()
        val iv = generateIv()
        val key = createSecretKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        val combinedBytes = ByteArray(salt.size + iv.size + encryptedBytes.size)
        System.arraycopy(salt, 0, combinedBytes, 0, salt.size)
        System.arraycopy(iv, 0, combinedBytes, salt.size, iv.size)
        System.arraycopy(encryptedBytes, 0, combinedBytes, salt.size + iv.size, encryptedBytes.size)
        return Base64.getEncoder().encodeToString(combinedBytes)
    }

    fun decryptString(encryptedText: String, password: String): String {
        val combinedBytes = Base64.getDecoder().decode(encryptedText)
        val saltSize = 16
        val ivSize = 16
        if (combinedBytes.size < saltSize + ivSize) {
            throw IllegalArgumentException("Invalid encrypted data format.")
        }
        val salt = combinedBytes.copyOfRange(0, saltSize)
        val iv = combinedBytes.copyOfRange(saltSize, saltSize + ivSize)
        val encryptedBytes = combinedBytes.copyOfRange(saltSize + ivSize, combinedBytes.size)

        val key = createSecretKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid password.", e)
        }

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }

    fun encryptFile(file: File, password: String) {
        val salt = generateSalt()
        val iv = generateIv()
        val key = createSecretKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encryptedFileName = encryptString(file.name, password)
        val encryptedFileNameBytes = encryptedFileName.toByteArray()
        val tempFile = File(file.parent, "${UUID.randomUUID()}.encrypted.tmp")
        val finalFile = File(file.parent, tempFile.name.removeSuffix(".tmp"))

        try {
            FileInputStream(file).use { fis ->
                FileOutputStream(tempFile).use { fos ->
                    // Записываем заголовок
                    val programIdBytes = PROGRAM_ID.toByteArray()
                    val keyHashBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())

                    fos.write(programIdBytes.size)
                    fos.write(programIdBytes)
                    fos.write(keyHashBytes.size)
                    fos.write(keyHashBytes)
                    fos.write(salt.size)
                    fos.write(salt)
                    fos.write(iv.size)
                    fos.write(iv)
                    fos.write(encryptedFileNameBytes.size)
                    fos.write(encryptedFileNameBytes)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) {
                            fos.write(output)
                        }
                    }
                    val output = cipher.doFinal()
                    if (output != null) {
                        fos.write(output)
                    }
                }
            }

            if (tempFile.renameTo(finalFile)) {
                file.delete()
            } else {
                throw Exception("Failed to rename temporary encrypted file.")
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun decryptFile(file: File, password: String) {
        val tempFile = File(file.parent, "${UUID.randomUUID()}.tmp")

        try {
            FileInputStream(file).use { fis ->
                val programIdSize = fis.read()
                val programIdBytes = ByteArray(programIdSize)
                fis.read(programIdBytes)
                val programId = String(programIdBytes)

                if (programId != PROGRAM_ID) {
                    throw IllegalArgumentException("The file was not encrypted with this program.")
                }

                val keyHashSize = fis.read()
                val storedKeyHash = ByteArray(keyHashSize)
                fis.read(storedKeyHash)

                val saltSize = fis.read()
                val salt = ByteArray(saltSize)
                fis.read(salt)

                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val encryptedFileNameSize = fis.read()
                val encryptedFileNameBytes = ByteArray(encryptedFileNameSize)
                fis.read(encryptedFileNameBytes)

                val providedKeyHash = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
                if (!storedKeyHash.contentEquals(providedKeyHash)) {
                    throw IllegalArgumentException("Invalid key. The key hash does not match.")
                }


                val encryptedFileName = String(encryptedFileNameBytes)
                val outputFileName = decryptString(encryptedFileName, password)
                val finalFile = File(file.parent, outputFileName)

                val key = createSecretKey(password, salt)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

                FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) {
                            fos.write(output)
                        }
                    }
                    val output = cipher.doFinal()
                    if (output != null) {
                        fos.write(output)
                    }
                }

                if (tempFile.renameTo(finalFile)) {
                    file.delete()
                } else {
                    throw Exception("Failed to rename temporary decrypted file.")
                }
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
