// :domain — 순수 Kotlin 모듈. Android·Firebase 의존성을 넣으면 안 된다 (기술명세 2장).
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// 툴체인 대신 타깃만 17로 고정한다 (Android Studio 내장 JDK 21로도 빌드되게).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed", "skipped") }
}
