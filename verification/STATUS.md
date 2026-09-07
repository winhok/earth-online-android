# 验证与交付状态

更新日期：2026-09-08。源码版本：`1.0.0-rc.1`。本次更新记录实际源码推送，不把候选源码提升为已验收的正式产品。

## GitHub 推送：已完成

目标仓库：`winhok/earth-online-android`；分支：`main`。

完整源码导入提交：`ca2da8281fee4ea57ab33449d3369bcaeeee204d`。

父提交：`e003415099288ce0c8bae7880b3b493305b55252`（原仓库初始化提交）。

完整导入的 Git tree：`e469222eaf7731b25bae04a8e8656a3f58f2f381`。它与原始 ZIP 解压后的全部 72 个文件及可执行权限对应的 Git tree 完全一致。本次通过 GitHub Git Data 接口创建 tree/commit，并以非强制方式更新 main；已读取远端 main ref 确认生效。

原始 72 文件快照保留在上述导入提交；后续文档更新不改变应用实现。原始交付包里“未创建仓库/未推送”的陈述是历史状态，不再是当前事实。仓库由用户提前创建，本次没有创建新仓库或更改仓库可见性。

Android CI 首次运行编号：`34153823405`，对应上述源码提交。核对时为 `in_progress`，尚无完成结论；实时状态以 GitHub Actions 为准：

https://github.com/winhok/earth-online-android/actions/runs/34153823405

## 原源码包已有的验证记录

原交付报告记录：使用 Kotlin CLI 1.9.0 与 OpenJDK 21 执行 41 组纯 Kotlin 领域场景，通过结果见 `core-tests.txt`。性质测试覆盖 12,001 个 XP 输入；实际循环为 0 至 120,000、步长 10。这不是 Android 全量构建。

`kotlin-syntax.txt` 记录 28 个 Kotlin/Kotlin DSL 文件语法解析通过；`static-checks.txt` 记录 XML、TOML、YAML 与 Shell 静态检查。这些报告来自原交付验证，本次推送没有重新执行它们；保留记录不代表新的平台测试通过。

`delivery.json` 是首次打包时的历史摘要，不是实时仓库状态接口；其中当时的仓库创建状态已被上面的实际推送记录取代。

## 仍未完成的产品验收

尚无通过的完整 Android 构建结果、Lint/Room/KSP 集成结果或已验收的 APK/AAB。9 项数据库/存档与 2 项 Compose UI 仪器测试在源码中存在，不能据此认定已经运行通过。Room schema 仍需真实构建生成；没有真机 UI、备份恢复、通知或无障碍验收结果。

没有配置生产签名或进行应用商店发布。大存档 8 MiB 限制与全量快照性能、中文文案资源化、设备兼容性及发布主体等仍是发布门槛。

## 复核命令

```bash
bash scripts/test-core-offline.sh
./gradlew :core:test :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

第一个命令需要 Kotlin CLI；后两个需要真实 Android 工具链、依赖和模拟器/测试机。报告必须与实际提交版本对应，不能用旧报告证明后续改动正确。发布清单见 `docs/RELEASE_CHECKLIST.md`；其中历史建仓步骤已经完成，其余条目仍须用实际证据关闭。
