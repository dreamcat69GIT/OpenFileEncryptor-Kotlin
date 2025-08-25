import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")

    kotlin("plugin.serialization") version "2.1.20"
}

group = "org.DreamCat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}


dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("io.ktor:ktor-client-cio:3.0.0-rc-1")
    implementation(kotlin("reflect"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.AppImage)
            packageName = "OpenFileEncryptor"
            packageVersion = "1.0.0"

            buildTypes {
                release {

                    proguard {
                        isEnabled.set(false)
                    }
                }
            }
        }
        jvmArgs += "-Dfile.encoding=windows-1251"
    }
}