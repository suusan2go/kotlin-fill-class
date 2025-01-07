import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "2.0.20"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("jvm-test-suite")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val pluginVersion = "2.0.1"

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
        intellijIdeaCommunity("2024.2.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }

    // Lorem : An extremely useful Lorem Ipsum generator for Java!
    implementation("com.thedeanda:lorem:2.1")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.intellij.platform:kotlinx-coroutines-core-jvm:1.8.0-intellij-9")
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    if (System.getProperty("idea.kotlin.plugin.use.k2") == "true") {
        include("com/github/suusan2go/kotlinfillclass/inspections/k2/**/*")
        systemProperty("idea.kotlin.plugin.use.k2", true)
    } else {
        exclude("com/github/suusan2go/kotlinfillclass/inspections/k2/**/*")
    }
}

val runK2 by intellijPlatformTesting.runIde.creating {
    task {
        systemProperty("idea.kotlin.plugin.use.k2", true)
    }
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
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "2024.2.1"
            }
        }
    }
    publishing {
        token = System.getenv("TOKEN")
    }
}
