import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "2.3.0"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("jvm-test-suite")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val pluginVersion = "2.1.0"

group = "com.github.suusan2go.kotlin-fill-class"
version = pluginVersion

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }

    // Lorem : An extremely useful Lorem Ipsum generator for Java!
    implementation("com.thedeanda:lorem:2.1")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.intellij.platform:kotlinx-coroutines-core-jvm:1.10.1-intellij-5")
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
    }
}

intellijPlatform {
    buildSearchableOptions = true
    instrumentCode = true
    projectName = project.name
    pluginConfiguration {
        id = "com.suusan2go.kotlin-fill-class"
        version = pluginVersion
        name = "kotlin-fill-class"
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdea)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "2025.3.1"
            }
        }
    }
    publishing {
        token = System.getenv("TOKEN")
    }
}
