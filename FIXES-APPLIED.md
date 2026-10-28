# 修复记录

本次针对 GitHub Actions 最近一次构建日志中的 Material 3 实验 API 编译错误进行了修复：

- `SettingsDialog.kt`：为 `FilterChip` 所在的 `SettingsDialog` 添加 `ExperimentalMaterial3Api` opt-in。
- `MainScreen.kt`：为 `FilterChip` 所在的 `MultiplierSection` 添加 opt-in。
- `ResultSheet.kt`：为 `FilterChip` 所在的 `InferTab` 添加 opt-in。

并检查了 Kotlin、Gradle Kotlin DSL 与 GitHub Actions YAML 文件，没有发现 Git 合并冲突标记。

注意：当前执行环境没有安装 Gradle，项目也没有 `gradle-wrapper.jar`，因此无法在此环境中完成完整 Android APK 构建验证。请在 GitHub Actions 或装有 JDK 17 和 Android SDK 34 的环境中重新构建。GitHub Actions 工作流会在构建成功后上传 `app-debug.apk` artifact。


## Follow-up fix (2026-10-28)
The GitHub repository workflow had literal Git merge-conflict separator lines, so GitHub Actions could not parse the workflow and created no jobs. The `.github/workflows/build-apk.yml` in this archive has been replaced with a clean workflow. After copying the project into the repository, make sure this exact workflow file overwrites the existing one. The APK build still needs to be rerun to verify compilation.
