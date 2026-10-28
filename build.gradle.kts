// 根构建脚本：只负责声明插件版本，具体模块在 app/build.gradle.kts 中应用
plugins {
    // Android Gradle Plugin
    id("com.android.application") version "8.1.4" apply false
    // Kotlin（Compose 编译器 1.5.4 对应 Kotlin 1.9.20）
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    // KSP：用于 Room 注解处理（取代 kapt，编译更快）
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
