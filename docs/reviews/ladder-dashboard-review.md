# Ladder Dashboard 实现评审

日期：2026-10-01。基线：`116a1303366cf1fc6b5f11d853b5b56c293e1d52`。实现前工作区为空；审查比较当前暂存改动与该基线。使用 implement、TDD 和 code-review 技能，规范与计划符合性由独立子代理审查，修正后分别复审。规范依据 AGENTS.md、项目结构研究；需求依据原 Ladder 计划、原型 D、demo/dashboard/context.md，以及用户“命名为 Ladder Dashboard、先查看 Concentric 并复用”的指示。没有 issue-tracker 配置，本任务以用户明确指定的计划文件为需求来源。

## Standards

未发现明确规范违规。单模块、feature → core、纯计算/状态、SDK 边界、字段取消、固定预览和 XML/registry/文档契约一致。公制例外和 Canvas 路径由计划明确授权。

一项 P3 判断性建议已处理：两个 DataField 的天气轮询重复。现已提取 `core/weather/DashboardWeatherUpdates.kt`，两个字段真实复用；供给的 scope 保持视图取消所有权。新增虚拟时间测试验证等待位置、失败两分钟重试、取消后不再请求以及在途请求取消。修正复审无剩余建议。

## Spec

未发现需要修复的偏差或额外范围。名称为 Ladder Dashboard，契约 ID 保留 ladder-full；整页资格、固定功率核心 D、公制/深色、速度/HRR/功率填充、踏频灯珠、缺失值、Idle、朝向及天气规则符合计划。

复用 Concentric 的归约、功率/风向计算、Karoo订阅、LOCATION转换、天气缓存/策略/循环、方向图形和配色。Ladder 的指示条/灯珠/绘制和显示模型留在专属切片。一秒读取最新快照、去重成功帧并重试失败提交，代替直接 distinctUntilChanged + sample；计划记录了该等价节流实现，预览/非整页不启动订阅。增量复审无回归。

实际 Canvas/RemoteViews、Karoo gridSize/LOCATION单位、Binder大位图和重复进出订阅取消仍未在设备运行，不能由编译和JVM测试代替。

## 验证

- `gradlew.bat tasks --all`：通过，无格式化任务。
- 定向 red → green：LadderCalculatorTest、LadderStateTest、LadderRuntimeTest、DataFieldRegistryTest、DashboardWeatherUpdatesTest。注册测试先消除缺少名称资源的编译错误，再确认字段缺少注册导致断言失败，随后同步 Kotlin/XML 后通过。
- Concentric 计算、状态、LOCATION、天气、运行时回归测试通过。
- `gradlew.bat lint test assembleDebug assembleDebugAndroidTest`：通过；81 个 JVM 测试，80 通过、1 个 QWeather live test 因未配置跳过，0失败。仪器 APK 编译成功，尚未运行。
- Lint：0 errors、7 warnings；包括既有 targetSdk/Manifest/Rear Cog 文本及 Log 警告，新增 Ladder 两处 Log 提示。保留 Android Log 以符合计划要求并沿用 Concentric 的可诊断日志行为。
- `git diff --cached --check`：清理共享配色文件末尾空行后通过。
- Gradle 使用 `GRADLE_USER_HOME=C:\Users\jguo\.gradle` 访问缓存。JDK 25 对 Java 8 编译目标的既有警告未阻止构建。
- `adb devices` 无连接设备，emulator 没有可用 AVD。未安装新的 SDK/系统镜像；实际渲染、真机 smoke test 和截图待验。复现说明在 `demo/dashboard/ladder/validation/README.md`，真机清单在计划和字段文档。

规范轴：0 项违规，1 项 P3 建议已处理；计划轴：0 项发现。修正复审两轴均无剩余行动项。
