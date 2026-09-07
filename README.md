# 地球 Online · Android

**把现实行动变成任务，把成长留在自己的存档里。**

原生 Kotlin + Jetpack Compose 的离线任务应用：选择当下时间与精力，获得可解释的下一步推荐；完成现实行动后结算经验、技能与成就。

> **当前状态：完整源码已经推送；版本仍为 `1.0.0-rc.1`，不是验收通过的正式 1.0。**
> 2026-09-08，原交付包的全部 72 个文件已提交到 `winhok/earth-online-android` 的 `main`，源码导入提交为 `ca2da8281fee4ea57ab33449d3369bcaeeee204d`，Git 文件树与原始源码包一致。
> Android CI 已由这次推送触发，但触发不代表构建通过。当前没有已验收的 APK/AAB；Android 编译、数据库集成和设备验收必须以真实运行结果为准。
> 最新交付记录见 [验证状态](verification/STATUS.md)。原交付文档和脚本中关于“创建仓库”的内容属于首次打包时的历史说明，不代表当前仓库状态；**不要为此仓库重新运行 `publish-github.*`**。

## 已实现的产品范围

| 入口 | 源码中的功能 |
| --- | --- |
| 指挥台 | 等级、今日完成数、时间/精力筛选、可解释的 Next Quest 推荐、反复暂缓任务重审 |
| 任务 | 支线 / 日常 / Boss；新建、编辑、搜索、筛选、关联主线、完成、撤销、暂缓、暂停、归档和重新接取 |
| 主线 | 创建方向与完成标准、关联任务、一次性任务进度、日常贡献数、编辑和归档 |
| 角色 | 总 XP、等级、五项技能、连续行动天数和七项成就 |
| 冒险日志 | 自动操作记录、经验变动与手写复盘 |
| 设置与存档 | 主题、玩家资料、可选每日提醒、JSON 导出、校验预览后确认导入、二次确认删除 |

没有账号、服务器、广告、统计 SDK、AI 调用或付费墙。推荐是本地规则，不伪装成 AI；不因拖延扣血。这些是源码实现范围，不是已经完成设备验证的声明。

## 获取与构建

仓库已经存在，直接克隆：

```bash
git clone https://github.com/winhok/earth-online-android.git
cd earth-online-android
```

用兼容项目 AGP 配置的 Android Studio 打开根目录，Gradle JDK 设为 **17**，安装 SDK Platform **36** 和 Build Tools **35.0.0**。

Windows PowerShell：

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
```

macOS / Linux：

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug
```

首次运行需要联网下载官方 Gradle Wrapper JAR，并验证固定 SHA-256；源码包未附带 JAR。之后仍需要 SDK 和 Gradle 依赖，只有 Wrapper 不足以离线构建。

**构建成功后**调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，测试包名为 `xyz.winhok.earthonline.debug`。这个路径不是已经生成的安装包下载地址。

构建参数与签名配置见 [构建指南](docs/BUILD.md)，其中首次创建仓库章节无需再执行。自动构建状态见 [GitHub Actions](https://github.com/winhok/earth-online-android/actions)。生产签名和应用商店发布没有在本次推送中执行。

## 工程结构

```text
app/                   Compose UI、ViewModel、Room、存档编解码、WorkManager
  src/androidTest/     数据库/存档和 UI 流程测试源码
core/                  纯 Kotlin 任务规则、经验算法、推荐和导入校验
.github/workflows/     Android CI 与手动签名工作流
scripts/               Wrapper 引导、离线领域测试、历史建仓脚本
verification/          验证记录；明确区分已执行与未验证
```

完成记录与日志在同一 Room 事务中写入。经验从有效完成记录推导，同一任务轮次只能结算一次；撤销和重做保留原奖励快照。完整设计见 [架构说明](docs/ARCHITECTURE.md)。

## 正式 1.0 的边界

完成 [发布清单](docs/RELEASE_CHECKLIST.md) 的工程、平台、数据和设备验收后，才能改为正式 `1.0.0`。源码推送、语法检查、测试代码存在和 CI 被触发，均不能代替这些验收。

本版不含 AI 规划、云同步、账号、公会、商城、DAG 依赖、地图或支付。维护者尚未选择对外开源许可证，源码可访问不等于授予任意再分发许可。

其他文档：[产品定义](docs/PRODUCT.md) · [隐私说明](docs/PRIVACY.md) · [依赖来源](docs/SOURCES.md) · [变更记录](CHANGELOG.md)
