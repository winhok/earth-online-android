# 构建、创建仓库与签名

## 环境

固定工具链，不等同于声称“当前最新”：

| 项目 | 版本 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin / Compose compiler plugin | 2.3.10 |
| KSP | 2.3.4 |
| Compose BOM | 2025.12.00 |
| Room | 2.8.4 |
| WorkManager | 2.11.2 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |
| Android Build Tools | 35.0.0（AGP 8.13 默认值） |

其余精确依赖见 `gradle/libs.versions.toml`。Android Studio 使用兼容 AGP 8.13 的稳定版本，Gradle JDK 设置为 17。不要因为机器全局 Java 是 8 就降级源码。项目需要访问 Google Maven、Maven Central、Gradle 和 GitHub 官方下载地址。

首次打开时由 Android Studio 配置 SDK；命令行可配置 `ANDROID_HOME` 或在不提交的 `local.properties` 设置 `sdk.dir`。Windows 路径须使用 Java properties 合法转义，建议让 Android Studio 自动生成。

## Wrapper 首次引导

源码包未包含 `gradle-wrapper.jar`，因为本次生成环境无法获取二进制；这不是可运行的假 JAR。`gradlew` / `gradlew.bat` 调用 `scripts/bootstrap-wrapper.*` 下载官方 Gradle 8.13 Wrapper JAR，**校验通过才执行**。这是一个固定版本、固定来源、带摘要校验的首次引导步骤，之后可离线复用已缓存文件和依赖。

Android Studio 首次导入前若提示 Wrapper 缺失，先执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-wrapper.ps1
```

或：

```bash
sh scripts/bootstrap-wrapper.sh
```

Wrapper JAR SHA-256：`81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`

Gradle binary ZIP SHA-256：`20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`

若 SHA 不匹配，停止并核对官方来源，不删除校验逻辑。离线机器可在可信联网机器按官方摘要验证后复制对应 JAR/Gradle 和已缓存依赖；只复制 Wrapper 并不能解决 SDK 和依赖缺失。

## 本机构建

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
# 检查混淆后的 release（没有签名配置时只是未签名产物）
.\gradlew.bat :app:lintRelease :app:assembleRelease
# 已启动模拟器或 adb 连接测试机时：
.\gradlew.bat :app:connectedDebugAndroidTest
```

macOS/Linux 将 `gradlew.bat` 换成 `./gradlew`。仪器测试会清空**调试测试包**的本机数据，不能把它当成生产环境无损健康检查。

常见产物：

- 调试 APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/`，是否已签名取决于 signing 环境变量
- Release AAB：执行 `:app:bundleRelease` 后位于 `app/build/outputs/bundle/release/`
- 领域报告：`core/build/reports/tests/test/index.html`
- Lint 与仪器报告：`app/build/reports/`
- Room schema：`app/schemas/`（首次构建后提交）

本交付只有源码与离线测试报告，不能把以上路径当作已经存在的 APK 下载地址。

## 创建私有 GitHub 仓库

本次已读到 GitHub 账户 `winhok`；连接器没有创建仓库动作，所以远程仓库未创建。创建脚本使用你**本机已认证**的 GitHub CLI，不内嵌凭证。

```powershell
gh auth login
powershell -ExecutionPolicy Bypass -File .\scripts\publish-github.ps1
```

脚本会校验账户、拒绝同名仓库和已有 `.git`、下载并验证 Wrapper、创建 main、提交全部未忽略文件、创建私有仓库并推送。要改名称：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\publish-github.ps1 -Repository earth-online-app
```

首次推送包含 `.github/workflows`。如果 GitHub 明确提示 OAuth 缺少 workflow scope，在本机执行 `gh auth refresh -h github.com -s workflow` 并按浏览器授权。若本地提交已经建立，脚本会安全停止而不是重做；运行 `git status`、`git remote -v`、`gh repo view winhok/earth-online-android` 检查状态，然后只补缺失步骤。

例如远程未创建时，用已经初始化的本地仓库执行：

```powershell
gh repo create winhok/earth-online-android --private --source . --remote origin --push
```

远程已创建但 push 失败时，核对 origin 确实是你刚创建的目标，再用 `git push -u origin main`。不要 force push，不要把已有其他仓库当成目标。

## CI

`android-ci.yml` 在 main 推送、PR 和手动触发时运行领域测试、Debug/Release Lint、Debug 和混淆 Release 构建，并在 API 29/35 模拟器上运行 11 项仪器测试。成功构建上传调试 APK，始终尽可能上传检查报告。

这些是待运行的自动化定义，当前交付没有远程 run URL 或成功记录。CI 下载 SDK、模拟器和 Gradle 插件需要网络；私有仓库可能消耗 Actions 配额。

正式发布前应检查工作流依赖，将第三方 Actions 从 major 标签升级为经过审核的完整 commit SHA，并设置 main 分支保护、required checks。仓库创建脚本不会替你更改账户或组织管理策略。

## 正式签名

签名密钥由维护者自行生成和保管，不能由聊天中的占位密码代替。不要提交 `.jks`、`.keystore`、密码或 Token。

本地 release 读取以下四个环境变量，全部有值才启用生产签名，部分配置会直接失败：

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

配置后运行 `:app:bundleRelease :app:assembleRelease`。可通过 `VERSION_NAME` 和 `VERSION_CODE` 指定版本，versionCode 必须正数并按发布顺序递增。

`release.yml` 是手动签名构建工作流，使用名为 `production` 的 GitHub Environment。维护者应先创建该环境、设置审批保护和下面四个 secrets：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

密钥仅解码到 runner 临时目录，构建后清除，不输出秘密。生成 APK/AAB 后仍需检查签名证书、真机安装升级和所有 release gates。工作流不会自动上传应用商店、收费、公开源码或创建 GitHub Release。
