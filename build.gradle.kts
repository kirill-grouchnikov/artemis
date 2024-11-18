import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.10"
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    idea
}

group = "org.pushing-pixels.artemis"
version = "1.0.0"

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        classpath(libs.compose.desktop)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.aurora.svgtranscoder.gradlePlugin)
        classpath(libs.versionchecker.gradlePlugin)
    }
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
val targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val skikoVersion = "0.8.18"
val skikoTarget = "${targetOs}-${targetArch}"

dependencies {
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.aurora.theming)
    implementation(libs.aurora.component)
    implementation(libs.aurora.window)
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:$skikoVersion")
    implementation(compose.components.resources)
}

tasks.register<org.pushingpixels.aurora.tools.svgtranscoder.gradle.TranscodeTask>("transcodeSingle") {
    inputDirectory = file("src/main/resources/svg")
    outputDirectory = file("src/gen/kotlin/org/pushingpixels/artemis/svg")
    outputPackageName = "org.pushingpixels.artemis.svg"
    transcode()
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    dependsOn("transcodeSingle")
}

configurations {
    all {
        exclude(group = "org.jetbrains.compose.material", module = "material")
        exclude(group = "org.jetbrains.compose.material3", module = "material3")
    }
}

kotlin {
    sourceSets {
        kotlin {
            sourceSets["main"].apply {
                kotlin.srcDir("$rootDir/src/main/kotlin")
                kotlin.srcDir("$rootDir/src/gen/kotlin")
            }
            dependencies {
                implementation(compose.components.resources)
            }
        }
    }
}

compose {
    resources {
        publicResClass = false
        packageOfResClass = "org.pushingpixels.artemis.resources"
        generateResClass = auto
    }
}

idea {
    module {
        generatedSourceDirs.add(file("$rootDir/src/gen/kotlin"))
    }
}

// To generate report about available dependency updates, run
// ./gradlew dependencyUpdates
apply(plugin = "com.github.ben-manes.versions")
