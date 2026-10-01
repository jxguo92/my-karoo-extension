# Concentric Dashboard 评审与修复

日期：2026-10-01。评审提交：`3dcfa36447f1920997d9c6816ceb41870a1f28d6`；基线：其父提交 `5428d144a5bea6496ba67d83e9b0190dcc346a3f`。依据：`AGENTS.md`、`docs/plans/concentric-dashboard.md` 及其原型/context。规范与计划符合性由独立子代理评审，修复后再次复审。

## Standards

未发现可确认的项目规范违规。计算和状态保持纯 Kotlin，SDK 转换在功能边界，注册契约同步，固定预览可复现。Canvas 路径由计划明确授权。

取消边界候选：`UrlConnectionHttpGet` 的阻塞 IO worker 与异步 disconnect 不保证调用方 `cancelAndJoin()` 返回时底层连接已释放。现有测试验证调用方及时取消，不能证明所有底层竞态均已排除。未复现具体失败，因此本轮不进行推测性传输层改写；真机需继续检查重复进出页面时请求释放。

## Spec

1. **P2，已修复：非整页空白帧首次提交失败后不重试。** 计划要求“更新失败……保留后续刷新能力”。原 `ConcentricRuntime.start()` 在初次提交后无条件返回，失败时无法恢复。现在以 1 Hz 重试空白帧，成功即停止，全程不读取实时状态、不订阅数据或请求天气。回归测试在原实现失败，修复后通过。
2. **兼容性缺口，已加固：LOCATION 速度只识别字面键 `LOC_SPEED`。** 计划要求同一样本速度优先于最近有效 SPEED。本地 SDK 1.1.9 源码及 [官方 DataType 源码](https://github.com/hammerheadnav/karoo-ext/blob/master/lib/src/main/kotlin/io/hammerhead/karooext/models/DataType.kt) 的其他 LOCATION 键使用 `FIELD_LOC_*_ID`，但缺少速度常量。因此 `FIELD_LOC_SPEED_ID` 只是按命名推断，不能宣称已验证。现在同时支持该键与原字面别名，前者优先；实际键及 m/s 单位仍需真机确认。合成 SDK 样本回归测试证明两种键的速度优先级、低速保持、缺失回退、NotAvailable 和 Idle 行为。

## 验证

- `gradlew.bat tasks --all`：通过；无格式化任务。
- `gradlew.bat testDebugUnitTest`：原提交基线通过。
- `gradlew.bat testDebugUnitTest --tests '*ConcentricRuntimeTest'`：新增测试先失败，修复后通过。
- `gradlew.bat testDebugUnitTest --tests '*ConcentricLocationTest'`：新增 wire-ID 样本测试先失败，兼容后通过。
- `gradlew.bat lint test assembleDebug`：通过；70 个 JVM 测试，69 通过，1 个和风 live test 跳过。
- `git diff --check`：通过。

所有 Gradle 命令使用 `GRADLE_USER_HOME=C:\Users\jguo\.gradle` 以访问已有缓存。存在 SDK XML/JDK 25 兼容警告和既有测试的 coroutine opt-in 警告，没有阻止验证。

本轮未修改渲染器，未重跑模拟器渲染。Karoo 真机的字段发现/添加、整页布局、主题传播、传感器断线、暂停/结束、Wi-Fi/手机天气、订阅与实际请求释放、Binder 传输，以及 LOCATION 速度键/单位仍待验证。

规范轴：0 项确认违规，1 项未确认的取消候选；计划轴：1 项 P2 已修复、1 项推断的兼容性缺口已加固。修复复审未发现新增具体问题。
