import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
}

group = "io.alice.platform"
version = "1.0-SNAPSHOT"

allprojects {
    apply {
        plugin("kotlin")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    api(projects.aliceApi)
    api(libs.bundles.root)
    api(libs.bundles.spring)
    api(libs.bundles.jackson)

//    implementation(libs.bundles.clients)

    api(kotlin("reflect"))
    api(kotlin("scripting-jvm"))
    api(kotlin("scripting-common"))
    api(kotlin("scripting-jvm-host"))
    api(kotlin("scripting-dependencies"))
    api(kotlin("scripting-dependencies-maven"))

    implementation(libs.bundles.common.reactive)

    testImplementation(kotlin("test"))
}