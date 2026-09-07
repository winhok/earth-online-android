# 官方参考与版本固定

核对日期：2026-09-08。下面是实现与构建配置使用的主要官方资料；选定版本是可复现基线，不声称它们都是当前最新版本。依赖解析与完整 Android 集成构建尚未在本次环境验证。

| 用途 | 主要来源 |
| --- | --- |
| AGP 8.13.2、Gradle 8.13、JDK 17、SDK 边界 | https://developer.android.com/build/releases/agp-8-13-0-release-notes |
| Kotlin 2.3.10 对 Gradle / AGP 的支持范围 | https://kotlinlang.org/docs/gradle-configure-project.html |
| Compose BOM 固定方式 | https://developer.android.com/develop/ui/compose/bom |
| Compose 2025.12.00 | https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html |
| Room 2.8.4 与迁移 | https://developer.android.com/jetpack/androidx/releases/room |
| KSP 2.3.4 | https://developer.android.com/build/migrate-to-ksp |
| KSP release | https://github.com/google/ksp/releases/tag/2.3.4 |
| Activity 1.12.2 | https://developer.android.com/jetpack/androidx/releases/activity |
| Lifecycle 2.9.4 | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| WorkManager 2.11.2 | https://developer.android.com/jetpack/androidx/releases/work |
| Gradle ZIP 与 Wrapper JAR 官方摘要 | https://gradle.org/release-checksums/ |
| GitHub CLI 创建私有仓库 | https://cli.github.com/manual/gh_repo_create |

Kotlin 2.3.10 的官方兼容范围包含本项目选定的 AGP 8.13.2 与 Gradle 8.13；这不代替本项目实际编译验证。WorkManager 采用 2.11.2 的修复版本，不使用预发布版本。

源码没有复制社区 Earth Online / Habitica 项目实现。图形为原生矢量和 Compose Canvas 的基础几何，未分发外部字体、商业插画或付费 UI 模板。
