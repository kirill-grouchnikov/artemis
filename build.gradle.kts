import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    idea
}

group = "org.pushing-pixels.artemis"
version = "1.0.0"

buildscript {
    repositories {
        mavenLocal()
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
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.aurora.theming)
    implementation(libs.aurora.component)
    implementation(libs.aurora.window)
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
        }
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
