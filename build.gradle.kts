import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0-alpha04"
}

group = "org.pushing-pixels.artemis"
version = "1.0.0"

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    dependencies {
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.1.0-alpha04")
        classpath(kotlin("gradle-plugin", version = "1.6.10"))
        classpath("org.pushing-pixels:aurora-tools-svg-transcoder-gradle-plugin:1.0.1")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("org.pushing-pixels:aurora-theming:1.0.1")
    implementation("org.pushing-pixels:aurora-component:1.0.1")
    implementation("org.pushing-pixels:aurora-window:1.0.1")
}

tasks.register<org.pushingpixels.aurora.tools.svgtranscoder.gradle.TranscodeTask>("transcodeSingle") {
    inputDirectory = file("src/main/resources/svg")
    outputDirectory = file("src/gen/kotlin/org/pushingpixels/artemis/svg")
    outputPackageName = "org.pushingpixels.artemis.svg"
    transcode()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    dependsOn("transcodeSingle")
}

kotlin {
    sourceSets {
        kotlin {
            sourceSets["main"].apply {
                kotlin.srcDir("$rootDir/src/main/kotlin")
                kotlin.srcDir("$rootDir/src/gen/kotlin")
            }
        }
    }
}
