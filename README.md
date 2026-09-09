# 地球 Online · Android

**把现实行动变成任务，把成长留在自己的存档里。**

原生 Kotlin + Jetpack Compose 的离线任务应用。选择当下时间与精力，获得可解释的下一步推荐；完成现实行动后结算经验、技能与成就。

## 1.0.0 已发布

[下载 Android 安装包](https://github.com/winhok/earth-online-android/releases/download/v1.0.0/earth-online-1.0.0.apk) · [完整发布页](https://github.com/winhok/earth-online-android/releases/tag/v1.0.0) · [通过的发布工作流](https://github.com/winhok/earth-online-android/actions/runs/34195496596)

这是 **Android 离线单人版 1.0.0 的 GitHub 直接分发版本**，不是 Google Play / 国内应用商店上架声明。Android 8.0 及以上；无需账号、服务器或 API Key。APK 可直接安装，AAB 供后续渠道投递，不能直接安装。

发布源码：`cd3fc2162e819d3a3f5b2513a82b4bbd47e12d94`（标签 `v1.0.0`）。后续 main 文档和数据库 schema 基线提交不替换这个已经测试、签名和发布的二进制。

**实际验收：41 组领域场景；API 26 / 29 / 35 / 36 每组 20 项仪器测试及 9 项签名 APK 黑盒检查，全部通过。** 四组使用同一个 APK SHA-256；设备为 Android 模拟器，不是实体手机。Debug / Release Lint 均无错误，各保留 4 个已记录警告。详细记录见 [验证状态](verification/STATUS.md)。

## 产品功能

| 入口 | 功能 |
| --- | --- |
| 指挥台 | 等级、今日完成数、时间 / 精力筛选、可解释的 Next Quest 推荐、反复暂缓任务重审 |
| 任务 | 支线 / 日常 / Boss；新建、编辑、搜索、筛选、关联主线、完成、撤销、暂缓、暂停、归档和重新接取 |
| 主线 | 创建方向与完成标准、关联任务、一次性任务进度、日常贡献数、编辑和归档 |
| 角色 | 总 XP、等级、五项技能、连续行动天数和七项成就 |
| 冒险日志 | 自动操作记录、经验变动与手写复盘 |
| 设置与存档 | 主题、玩家资料、可选每日提醒、JSON 导出、校验预览后确认导入、二次确认删除 |

推荐是本地规则，不伪装成 AI；没有广告、统计 SDK、账号或付费墙，不因拖延扣血。技能值仅表示应用内进度，不评价个人现实能力。

## 数据和使用边界

任务保存在本机。**卸载或清除数据前务必导出存档。** JSON 备份为明文，大小上限 8 MiB，写入前有保守容量保护；导入先校验和预览，再由用户确认后事务覆盖。没有云端恢复。

每日提醒使用 WorkManager 尽力调度，不保证准点。小米 / 华为等实体设备后台策略、完整 TalkBack / 200% 字体覆盖、系统云文件提供商、10,000 条任务性能和应用商店审核尚未完成，不把这些列作已验证能力。发布与后续加固分开记录在 [验收清单](docs/RELEASE_CHECKLIST.md)。

## 源码与构建

```bash
git clone https://github.com/winhok/earth-online-android.git
cd earth-online-android
# 重建当前已发布版本时：git checkout v1.0.0
./gradlew :core:test :app:lintDebug :app:lintRelease :app:assembleDebug
```

Windows 使用 `gradlew.bat`。Gradle JDK 为 17，SDK Platform 36，Build Tools 35.0.0。仓库包含已按固定 SHA-256 验证的官方 Wrapper JAR；首次构建仍需联网下载 Gradle、SDK / 构建依赖。不要再次运行历史 `publish-github.*` 建仓脚本。

Debug 包名是 `xyz.winhok.earthonline.debug`。正式包名是 `xyz.winhok.earthonline`，versionCode 为 1。后续覆盖升级须使用同一签名密钥并递增 versionCode。维护者的密钥和口令以私有压缩包交付，**不在源码或公开 Release 中**；不能用一次性 bootstrap 再生成一个不同密钥来发布升级。后续签名工作流读取 production 签名 Secrets，这些账号级 Secrets 尚未配置。

## 工程结构

```text
app/                   Compose UI、ViewModel、Room、存档编解码、WorkManager
  src/androidTest/     20 项数据库、存档、页面与生命周期测试
  schemas/             实际构建导出的 Room v1 schema 基线
core/                  纯 Kotlin 任务规则、经验算法、推荐和导入校验
.github/workflows/     Android CI、签名构建与首次发布验收流程
scripts/               Wrapper 引导、领域测试、签名 APK 黑盒验收
verification/          版本化证据、校验和和明确的验证边界
```

完成记录和日志在同一 Room 事务中写入。经验从有效完成记录推导；同一轮次只能结算一次，撤销重做保留奖励快照。[架构说明](docs/ARCHITECTURE.md)

本版不含 AI 规划、云同步、公会、商城、DAG 依赖、地图或支付。对外开源许可策略尚未选择，源码公开可读不等于授予任意再分发许可。

[产品定义](docs/PRODUCT.md) · [隐私说明](docs/PRIVACY.md) · [构建指南](docs/BUILD.md) · [发布说明](docs/RELEASE_1.0.0.md) · [变更记录](CHANGELOG.md)
