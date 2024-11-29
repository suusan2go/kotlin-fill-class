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
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

val ktlint by configurations.creating

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.4")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        instrumentationTools()
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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

    testImplementation("junit:junit:4.13.1")
    testImplementation("io.mockk:mockk:1.13.4")
    ktlint("com.pinterest:ktlint:0.42.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling.EXTERNAL))
        }
    }

}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

val ktlintTask = tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
    // to generate report in checkstyle format prepend following args:
    // "--reporter=plain", "--reporter=checkstyle,output=${buildDir}/ktlint.xml"
    // to add a baseline to check against prepend following args:
    // "--baseline=ktlint-baseline.xml"
    // see https://github.com/pinterest/ktlint#usage for more
}
tasks.check {
    dependsOn.add(ktlintTask)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt")
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