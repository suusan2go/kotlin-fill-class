import org.jetbrains.intellij.platform.gradle.TestFrameworkType
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
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val pluginVersion = "1.0.23"

intellijPlatform {
    buildSearchableOptions = true
    instrumentCode = true
    projectName = project.name
    pluginConfiguration {
        id = "com.suusan2go.kotlin-fill-class"
        version = pluginVersion
        name = "kotlin-fill-class"
        ideaVersion {
            sinceBuild = "242.21829.142"
            untilBuild = null
        }
    }
    publishing {
        token = System.getenv("TOKEN")
    }
}

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
        intellijIdeaCommunity("2024.2.4")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
//    compileOnly("org.jetbrains.kotlin:high-level-api-for-ide::2.0.20-ij242-38") {
//        exclude(group = "org.jetbrains.kotlin", module = "analysis-api")
//    }
//    compileOnly("org.jetbrains.kotlin:high-level-api-fir-for-ide::2.0.20-ij242-38") {
//        exclude(group = "org.jetbrains.kotlin", module = "analysis-api-fir")
//    }
//    compileOnly("org.jetbrains.kotlin:high-level-api-fe10-for-ide:2.0.20-ij242-38") {
//        exclude(group = "org.jetbrains.kotlin", module = "analysis-api-fe10")
//    }
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
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

tasks.named<Test>("test") {
    useJUnitPlatform()
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
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}
