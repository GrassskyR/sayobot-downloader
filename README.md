# Sayobot Downloader

一个基于 Android Jetpack Compose 的 Sayobot 谱面浏览与下载工具。应用通过 Sayobot API 获取 osu! 谱面信息，支持查看最新、热门和搜索结果，并可进入详情页试听与下载 `.osz` 文件。

## 功能特性

- 浏览 Sayobot 最新谱面和热门谱面
- 按关键词搜索谱面
- 查看谱面封面、标题、作者、状态、BPM、标签和难度信息
- 播放谱面预览音频
- 下载谱面包，支持完整包、无视频包和 mini 包
- 下载进度通知和完成通知
- 通过 GitHub Actions 自动构建 debug APK

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Lifecycle ViewModel
- Navigation 3
- OkHttp
- kotlinx.serialization
- Coil
- JUnit4 与 kotlinx-coroutines-test

## 项目结构

```text
app/src/main/java/com/example/sayobotdownloader/
  data/        谱面数据仓库
  network/     Sayobot API 请求
  model/       API 数据模型
  ui/search/   搜索与列表页面
  ui/detail/   详情、试听与下载页面
  download/    下载与通知逻辑
  theme/       Compose 主题

app/src/test/          JVM 单元测试
app/src/androidTest/   Android/Compose 仪器测试
.github/workflows/    GitHub Actions CI
```

## 构建与运行

请使用 Android Studio 打开项目，或在仓库根目录使用 Gradle Wrapper。

```powershell
.\gradlew.bat :app:assembleDebug
```

生成的 debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

本项目需要联网访问 Sayobot API，并在 Android 13 及以上系统请求通知权限以显示下载进度。

## 测试与检查

运行当前 CI 同等的本地检查：

```powershell
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

如需在本地设备或模拟器上运行仪器测试：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

当前 GitHub Actions 仅执行 lint、单元测试和 debug 构建，不运行模拟器测试。

## CI 产物

推送到 `main` 或 `master` 后，GitHub Actions 会运行 Android CI。构建成功后，可在 workflow run 的 Artifacts 中下载 `app-debug`。

## 许可证

本项目使用 MIT License，详见 [LICENSE](LICENSE)。
