# 验证与交付状态

更新日期：2026-09-08。**1.0.0 Android 离线单人版已通过自动验收并在 GitHub 发布。** 这不是应用商店上架或实体设备兼容性认证。

## 可追溯交付

- 仓库：`winhok/earth-online-android`。
- 发布标签：`v1.0.0`，非草稿、非 prerelease。
- 实际构建及受测源码：`cd3fc2162e819d3a3f5b2513a82b4bbd47e12d94`。
- 发布工作流：`34195496596`，package、API 26 / 29 / 35 / 36 acceptance、publish 全部 success。
- 发布时间：2026-09-08 06:49:07 UTC。
- 发布页：https://github.com/winhok/earth-online-android/releases/tag/v1.0.0
- 工作流：https://github.com/winhok/earth-online-android/actions/runs/34195496596

后续文档 / Room schema 基线归档提交不改变已发布 APK；main 保留完整祖先历史，不强推。

## 二进制身份

| 项目 | 值 |
| --- | --- |
| applicationId | `xyz.winhok.earthonline` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk / targetSdk | `26` / `36` |
| APK 大小 | 1,759,998 字节 |
| APK SHA-256 | `e48fb969889aec5df9e80af93564b1bcf0a335f1b3a3c3fbd1146295651563a4` |
| AAB 大小 | 4,228,640 字节 |
| AAB SHA-256 | `98bb4fdbdf37303af22db8175044a70069b582c2b559446303f6ec92ba2e7069` |
| 签名证书 SHA-256 | `447baf2063785d0e45d1f267c8b8a3fb1904f7b33d88bef6cf0f922aa0b017bd` |

APK 通过 apksigner 验证（v2 / RSA 3072），AAB 通过 jarsigner 校验。AAB 使用自签名证书；此校验不替代渠道内部测试。合并后的 APK Manifest 不包含 INTERNET 权限。公开 Release 资产摘要与本地交付文件已比对一致。

签名恢复材料已实际解密，恢复 keystore 的证书与受测 APK 证书匹配。私钥、keystore 和口令只交付维护者，不在公开仓库 / Release 中。账号级 GitHub production 签名 Secrets 尚未配置；不影响安装这个已经签好的 APK。

## 已执行测试

| 平台 | 仪器测试 | 同一签名 Release APK 黑盒检查 | 失败 / 跳过 |
| --- | --- | --- | --- |
| Android 8.0 / API 26 | 20 / 20 | 9 / 9 | 0 / 0 |
| Android 10 / API 29 | 20 / 20 | 9 / 9 | 0 / 0 |
| Android 15 / API 35 | 20 / 20 | 9 / 9 | 0 / 0 |
| Android 16 / API 36 | 20 / 20 | 9 / 9 | 0 / 0 |

这是 20 项用例在四个平台执行 80 次，以及 9 项签名包检查执行 36 次，**不是 116 个彼此不同的测试用例**。测试设备为 GitHub Actions 的 x86_64 Android 模拟器。

领域测试的一个 JUnit 入口执行了全部 41 组 ScenarioSuite 场景，含 12,001 个 XP 输入的性质检查。不能把一个 JUnit 入口误报成 41 个独立 JUnit 用例。

仪器测试覆盖 Room 并发结算、撤销重做奖励快照、跨午夜日常轮次、无效 / 超限存档拒绝、事务回滚、持久化重开、1,000 任务往返与推荐、页面和主题、编辑草稿 Activity 重建等。20 项测试的原始 JUnit XML 均保存在设备证据中。

签名 APK 黑盒检查实际经过系统界面：安装、断网创建角色、创建任务、完成、撤销、强制停止后重新打开保留数据、系统文件选择器导出 JSON、确认后覆盖恢复、会话内无本应用崩溃记录。导入前额外创建的任务在覆盖后消失，原存档任务保留。

`verify-release-evidence.py` 在发布前要求每个平台恰好 20 条无失败 / 无跳过测试和全部 9 个检查点，并核对各平台 APK SHA-256 与待发布包相同。本地重新解析四个平台的 XML / JSON 后，重建的 acceptance-summary.json 与公开 Release 同名文件 SHA-256 一致：`ea1defdef9117f58178f975f4eca6d363ebc36f7cf212096bfe88cd810577fce`。

## 构建与诊断

Kotlin / Compose、Room KSP、R8 混淆 Release APK / AAB 构建成功。Debug 和 Release Lint 均为 0 errors、4 warnings：OldTargetApi、ObsoleteSdkInt、MonochromeLauncherIcon、UseKtx。没有把这些警告删除或报告为零警告。

本次实际修复了跨模块 smart cast、通知 R 引用、编辑窗口反馈、FAB 可访问名称、旧 Android 全屏编辑器 IME Insets，以及 UI 测试中的输入法 / 懒加载竞态。没有删除失败用例或降低验收断言。

Room v1 schema 基线来自该运行的 release-build-evidence 原始构建产物：数据库版本 1，identityHash `33a5ff2908984032f2b24765c9d62fc8`。后续实体变更须递增版本并提供迁移测试。

## 证据位置

公开 Release 附带 `acceptance-summary.json`、`device-acceptance-evidence.tar.gz`、`SHA256SUMS.txt`、签名 / Manifest 摘要、源代码提交文件以及 APK / AAB。Actions artifacts 另保留 release-build-evidence 及四个 release-device-api-* 原始报告。Actions 产物有保留期限，公开 Release 保存设备证据；维护者另收到完整验证 ZIP。

## 明确未验证的范围

实体小米 / 华为等 OEM 的后台和通知行为；完整 TalkBack、200% 字体、平板 / 分屏覆盖；10,000 任务长时间性能；系统云文件提供商；Google Play / 国内渠道内部测试和合规审核；依赖全量锁定、Actions 全 SHA 固定、分支保护与 production 审批。上述没有伪装成完成，见发布清单。

提醒为 WorkManager 尽力调度，不保证准点；存档为本机数据 / 明文 JSON，无云端恢复，卸载前须导出。GitHub 直接分发 1.0 已完成，不等于上述渠道和运营事项已完成。

## 历史记录说明

原始 rc.1 源码包、core-tests.txt、kotlin-syntax.txt、static-checks.txt、delivery.json 属于首次交付时的历史记录。首次源码导入 `ca2da8281fee4ea57ab33449d3369bcaeeee204d` 和后续修复均保留在 Git 历史中。旧记录中的未构建 / 未推送状态已被本页的真实发布结果取代。
