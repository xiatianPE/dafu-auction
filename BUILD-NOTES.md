# 构建说明 / 本次工作记录

## 1. 怎么编译

电脑上（JDK 17 + Android SDK 34）：

```bash
# 用 Android Studio 打开该目录，或命令行：
./gradlew :app:assembleDebug      # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest       # 运行领域层单元测试
```

第一次需要生成 wrapper（仓库里没有 `gradle-wrapper.jar`）：

```bash
gradle wrapper --gradle-version 8.2
```

`local.properties` 没有打包（里面是本机 SDK 路径），Android Studio 会自动生成：

```
sdk.dir=/你的/Android/Sdk
```

## 2. 代码里验证过的部分

在 ARM64 Linux 上用 `kotlinc 1.9.20` 单独编译了领域层并跑了单测：

- 编译文件：`domain/model/Models.kt`、`domain/model/CatalogSeed.kt`、
  `domain/usecase/EvaluateUseCase.kt`、`domain/usecase/InferEngine.kt`、
  `domain/repository/CatalogRepository.kt`
- 结果：**0 error**
- 单测：`InferEngineTest` + `EvaluateUseCaseTest` 共 **8 个用例全部通过**
  （估价 = 金红数×倍率+加成、七档出价、大金不计加成、反推零误差组合、已见大金固定项等）

## 3. 为什么没能在本机出 APK

当前开发环境是 **aarch64 的 Alpine（musl）**，而 Android 官方 build-tools 里的
`aapt2` 只有 x86_64 版本，直接运行会报
`cannot execute: required file not found`。

尝试的绕过方案（仅 ARM64 Linux 需要，PC 上不需要）：

1. 从 Termux 仓库取 aarch64 版 `aapt2` 及其依赖（libc++/protobuf/abseil/fmt/expat/png/zopfli/zlib）；
2. 用一个 shell 包装脚本只给 aapt2 注入 bionic 库路径，避免污染 musl 的 java：

```sh
#!/bin/sh
LD_LIBRARY_PATH=/path/to/termux-libs
export LD_LIBRARY_PATH
exec /path/to/termux/aapt2 "$@"
```

3. 在 `gradle.properties` 里指定：

```properties
android.aapt2FromMavenOverride=/path/to/wrapper/aapt2
```

校验结果：`aapt2 version` 与 `aapt2 daemon` 都能在设备上正常输出
（`Android Asset Packaging Tool (aapt) 2.20-android-16.0.0_r4` / `Ready`）。
Gradle 打包过程因设备内存/耗时原因中止，需要完整跑一次 `assembleDebug`
（PC 上直接 `./gradlew :app:assembleDebug` 即可，不需要上述 hack）。

## 4. 目录结构

```
.
├── build.gradle.kts          # 根构建脚本（插件版本）
├── settings.gradle.kts       # 仓库与模块声明
├── gradle.properties         # 构建参数（本机额外加了 aapt2 override，可删）
├── app/
│   ├── build.gradle.kts      # 应用模块（Compose/Room/DataStore/Koin）
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # 无任何权限
│       │   ├── java/com/dafu/auction/       # 24 个 Kotlin 文件
│       │   └── res/                         # 主题/图标/备份规则
│       └── test/java/...                    # 领域层单元测试
└── README.md
```
