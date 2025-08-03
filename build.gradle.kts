import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.izzel.taboolib") version "2.0.25"
}

taboolib {

    version {
        taboolib = "6.2.3-ac49c9a"
    }

    env {
        // 镜像中央仓库
        repoCentral = "https://repo.huaweicloud.com/repository/maven/"
        // 安装模块
        install(
            Basic,
            Bukkit,
            Kether,
            BukkitHook,
            BukkitNMS,
            BukkitNMSUtil,
            DatabasePlayer,
            Metrics,
            I18n,
            BukkitUI,
            JavaScript,
        )
        install("platform-bukkit-impl")
    }

    description {
        name(rootProject.name)

        desc("灵活强大的多功能容器 GUI 解决方案")

        links {
            name("homepage").url("https://invero.8aka.org/")
        }

    }

    // 重定向
    relocate("kotlinx.serialization.", "kotlinx.serialization180.")
    relocate("org.slf4j", "cc.trixey.invero.libs.slf4j")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.momirealms.net/releases/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    // Kotlin 序列化
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.0")

    // Adventure API - 使用更兼容的版本以支持更广泛的 MC 版本
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.4")

    // Minecraft Core
    compileOnly("ink.ptms.core:v12101:12101:mapped")
    compileOnly("ink.ptms.core:v12101:12101:universal")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("io.netty:netty-all:4.1.106.Final")
    compileOnly("com.google.code.gson:gson:2.8.9")
    compileOnly("com.google.guava:guava:32.0.0-android")
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("com.mojang:authlib:5.0.51")

    // 添加 SLF4J 依赖
    taboo("org.slf4j:slf4j-api:1.7.36")
    taboo("org.slf4j:slf4j-simple:1.7.36")

    // Compatible Plugins
    compileOnly("ink.ptms.adyeshach:all:2.0.0-snapshot-36")
    compileOnly("org.black_ixx:playerpoints:3.1.1")
    compileOnly("io.th0rgal:oraxen:1.189.0")
    compileOnly("ink.ptms:Zaphkiel:2.0.14")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.3-beta-14")

    // CraftEngine
    compileOnly("net.momirealms:craft-engine-core:0.0.22") { isTransitive = false }
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.22") { isTransitive = false }

    // MMOItems
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT") { isTransitive = false } // Required by MMOItems API
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT") { isTransitive = false }

    // EcoItems
    compileOnly("com.willfp:eco:6.71.3") { isTransitive = false }
    compileOnly("com.willfp:libreforge:4.60.0") { isTransitive = false }
    compileOnly("com.willfp:EcoItems:5.49.1") { isTransitive = false }

    compileOnly(fileTree("libs"))
}

// 资源处理
tasks.processResources {
    filesMatching("**/*.json") {
        expand(
            "serialization" to "1.8.0",
            "adventureApi" to "4.17.0",
            "adventurePlatform" to "4.3.4",
            "kr" to "210", // Kotlin Version Escaped
            "krx" to "180", // Kotlin Serialization Version Escaped
        )
    }
}

// Kotlin 构建设置
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xextended-compiler-checks",
            "-Xskip-prerelease-check",
            "-Xallow-unstable-dependencies"
        )
    }
}

// Java 构建设置
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// 编码设置
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}