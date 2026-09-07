# 地球 Online · Android

**把现实行动变成任务，把成长留在自己的存档里。**

原生 Kotlin + Jetpack Compose 的离线任务应用。任务不是无限积压的清单：玩家选择当下时间与精力，指挥台给出可解释的下一步；完成现实行动后结算经验、技能与成就。

> **交付状态：1.0.0-rc.1 源码候选版，不是已经通过验收的正式版。**
> 已执行 41 组纯 Kotlin 领域测试（含 12,001 个 XP 输入的性质检查）及源码语法/配置检查。当前交付环境没有 Android SDK、Gradle 依赖缓存，也无法下载构建依赖，因此 **没有完成 Android 编译、Lint、模拟器/真机测试，没有 APK/AAB**。
> GitHub 账户 `winhok` 已确认连接，但本次工具不提供创建仓库动作，**远程仓库尚未创建，也没有推送**。下文脚本由仓库所有者在本机执行后才会创建私有仓库。

## 已实现的产品范围

| 入口 | 功能 |
| --- | --- |
| 指挥台 | 玩家等级、今日完成数、时间/精力筛选、可解释 Next Quest 推荐、反复暂缓任务重审 |
| 任务 | 支线 / 日常 / Boss；新建、编辑、搜索、筛选、关联主线、完成、撤销、暂缓、暂停、归档和重新接取 |
| 主线 | 创建方向与完成标准、关联任务、一次性任务完成进度、日常贡献数、编辑和归档 |
| 角色 | 总 XP、等级进度、知识/活力/创造/连接/自律五项技能、连续行动天数、七项成就 |
| 冒险日志 | 自动记录关键任务行为、查看奖励变动、手写反思、展开长手记 |
| 设置与存档 | 明暗/跟随系统主题、玩家资料、可选每日提醒、JSON 导出、校验并预览后覆盖导入、二次确认删除 |

没有账号、服务器、广告、统计 SDK、AI 调用或付费墙。没有用假数据填充首页，也不会因拖延扣血。系统推荐是本地规则，不伪装成 AI。

## 快速开始

用支持 AGP 8.13 的 Android Studio 打开**项目根目录**。将 Gradle JDK 设为 **17**，安装 Android SDK Platform **36** 和 Build Tools **35.0.0**。

Windows PowerShell：

```powershell
# 首次需要联网：自动下载官方 Wrapper JAR，并校验固定 SHA-256。
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
```

macOS / Linux：

```bash
chmod +x gradlew
./gradlew :core:test :app:assembleDebug :app:lintDebug
```

**构建成功后**调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。源码包不附带 APK。测试包的包名为 `xyz.winhok.earthonline.debug`，与正式包隔离。

本次环境无法下载二进制，因此源码包中没有伪造或损坏的 `gradle-wrapper.jar`。首次运行由脚本下载官方文件并校验，发布脚本会将校验后的 JAR 一并提交。Gradle ZIP 也固定 SHA-256。详细说明见 [构建指南](docs/BUILD.md)。

## 创建你的私有 GitHub 仓库

安装 Git 和 GitHub CLI，解压源码并进入根目录，在本机完成认证。**不要把 Token 或签名密码发到聊天里。**

```powershell
gh auth login
powershell -ExecutionPolicy Bypass -File .\scripts\publish-github.ps1
```

macOS / Linux 对应：

```bash
gh auth login
bash scripts/publish-github.sh
```

脚本默认只接受登录账户 `winhok`，创建私有 `earth-online-android`，初始化 `main`、提交源码并推送。已有同名仓库或本地 `.git` 时停止，不覆盖、不强推。脚本会进行真实远程写入，请先检查代码及工作区没有自己的密钥文件。

GitHub Actions 工作流已写入，但**不是已经运行成功**。首次推送后检查 Actions：构建、Lint、领域测试和 Android 仪器测试分别产出报告。私有仓库 Actions 可能消耗账户配额，请先检查账户用量设置。

## 技术结构

```text
app/                         Android 应用与平台能力
  src/main/.../ui/           Compose 界面、ViewModel、输入/错误/空状态
  src/main/.../data/         Room、事务仓库、带完整性校验的存档编解码
  src/main/.../reminder/     WorkManager 每日提醒
  src/androidTest/          9 项数据库/存档测试 + 2 项 UI 流程测试（待执行）
core/                        与 Android 无关的纯 Kotlin 规则
  src/main/                  任务、XP、连续天数、推荐、导入校验
  src/test/                  41 组可离线运行的领域测试 + JUnit 入口
.github/workflows/           构建验收与手动签名候选版工作流
scripts/                     Wrapper 引导、私有仓库初始化、离线测试
verification/                本次实际检查报告与未验证项
```

核心设计是 **Room 事务 + 唯一完成记录 + 奖励快照**，而不是点一下按钮直接给玩家余额加分。领域规则经过实际测试；Android 集成仍必须通过平台测试。完整说明见 [架构设计](docs/ARCHITECTURE.md)。

## 1.0 发布门槛

完成 [发布清单](docs/RELEASE_CHECKLIST.md) 后才能把版本号改为 `1.0.0`。尤其必须通过 Android 构建、混淆后回归、数据库事务与 UI 测试、真实设备权限/提醒/备份恢复测试，并提交首次构建生成的 Room schema。

这是一个边界明确的**离线单人 1.0 候选实现**，不是“AI、云同步、社交、公会、商城”全功能平台。上述能力均不在当前实现中，界面也没有伪按钮。

其他文档：[产品定义](docs/PRODUCT.md) · [隐私说明](docs/PRIVACY.md) · [实测状态](verification/STATUS.md) · [依赖来源](docs/SOURCES.md) · [变更记录](CHANGELOG.md)

维护者尚未选择对外开源许可证；创建私有仓库不代表已经授权公开分发。本项目未复制其他地球 Online 或 Habitica 项目代码。
