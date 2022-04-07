import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
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
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.1.1")
        classpath(kotlin("gradle-plugin", version = "1.6.10"))
        classpath("org.pushing-pixels:aurora-tools-svg-transcoder-gradle-plugin:1.1.0")
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
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("org.pushing-pixels:aurora-theming:1.2-SNAPSHOT")
    implementation("org.pushing-pixels:aurora-component:1.2-SNAPSHOT")
    implementation("org.pushing-pixels:aurora-window:1.2-SNAPSHOT")
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
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.skiko") {
                useVersion("0.7.18")
                because("Pin to version that has shader bindings")
            }
        }
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