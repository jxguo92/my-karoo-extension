# AGENTS.md

本文件是仓库内 coding agent 的首要项目指南。项目尚处于初始化阶段；新增代码时遵循这里的目标结构与边界，并以实际 Gradle 配置为命令和依赖版本的最终依据。

## 项目目标

这是一个 Hammerhead Karoo 扩展。第一阶段交付定制 Data Field，后续可按真实需求增加地图层、提醒和设备接入等能力。

保持单一 `app` 模块，并按功能垂直切片。优先选择可定位、可测试、低认知负担的实现：Kotlin、Coroutines/Flow、Compose 设置页、DataStore，以及仅用于图形 Data Field 的 Glance。

## 工作顺序

1. 修改前阅读相邻实现、测试和资源声明；涉及项目结构决策时再阅读 `docs/research/karoo-extension-project-structure.md`。
2. 将业务变更放进对应 `feature/` 切片；将 Karoo/Android 接口适配留在边界层。
3. 先为纯计算、状态转换和注册契约补测试，再连接 SDK stream 或 UI。
4. 执行与改动匹配的验证；交付时说明已运行的命令，以及无法完成的真机检查。

## 目标目录结构

正式创建项目时，将 `com.example.mykaroo` 一次性替换为确定的 namespace/applicationId。按需创建目录；未来能力尚未使用时不放空壳文件。

```text
.
├── AGENTS.md
├── README.md
├── docs/
│   ├── data-fields.md                 # typeId、单位、预览和数据来源登记
│   └── research/
│       └── karoo-extension-project-structure.md
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/example/mykaroo/
│       │   │   ├── MainActivity.kt
│       │   │   ├── MyKarooApplication.kt       # 仅需要全局初始化/容器时创建
│       │   │   ├── extension/
│       │   │   │   ├── MyKarooExtension.kt     # composition root，只做编排
│       │   │   │   └── DataFieldRegistry.kt    # Kotlin 侧唯一字段注册入口
│       │   │   ├── feature/
│       │   │   │   ├── datafield/
│       │   │   │   │   └── <field_name>/
│       │   │   │   │       ├── <Name>DataField.kt
│       │   │   │   │       ├── <Name>Calculator.kt
│       │   │   │   │       ├── <Name>State.kt
│       │   │   │   │       ├── <Name>View.kt       # 仅图形字段
│       │   │   │   │       └── <Name>Preview.kt    # 仅需要预览时
│       │   │   │   ├── settings/
│       │   │   │   └── map/                        # 真正需要地图能力时创建
│       │   │   ├── core/
│       │   │   │   ├── karoo/                      # SDK adapter 与 Flow helper
│       │   │   │   ├── storage/                    # DataStore 与 serializer
│       │   │   │   ├── ui/                         # 少量跨功能 UI token
│       │   │   │   └── util/                       # 仅放明确复用的工具
│       │   │   └── di/
│       │   │       └── AppContainer.kt             # 显式构造函数装配
│       │   └── res/
│       │       ├── xml/extension_info.xml
│       │       ├── values/strings.xml
│       │       └── drawable/
│       └── test/kotlin/com/example/mykaroo/
│           ├── extension/DataFieldRegistryTest.kt
│           └── feature/datafield/<field_name>/
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── .github/workflows/
    ├── build.yml
    └── release.yml
```

## 依赖与职责边界

- 依赖方向为 `feature -> core`。不同功能切片通过小型接口或共享状态协作，不直接引用彼此的具体实现。
- `MyKarooExtension` 和 `DataFieldRegistry` 负责构造、注册与生命周期编排；业务计算留在切片内。
- `*DataField.kt` 只做 SDK 适配：订阅 Flow、转换 `StreamState`、绑定 emitter cancellable。
- `*Calculator.kt` 与 `*Formatter.kt` 保持纯 Kotlin，不依赖 Android 或 Karoo SDK。
- 状态使用不可变 `data class`/`sealed interface` 与 `StateFlow`。持久化设置使用 DataStore；Activity 不是 Extension service 的状态所有者。
- `KarooSystemService` 通过薄的 gateway/helper 集中连接、断开和类型转换。Extension 销毁时取消其 `SupervisorJob`，字段取消时终止自己的子任务。
- Compose 用于 Activity/设置页；Glance 仅用于 `graphical="true"` 的 Data Field，并与 Compose screen 分文件。
- 使用构造函数注入和小型 `AppContainer`。只有多个 Android 入口共享大量长生命周期对象、手工装配已造成重复或生命周期错误时，才评估 Koin。
- 共享实现出现至少两个真实调用者后再移入 `core/`。优先具体代码，避免预设抽象和宽泛的 `util`。
- 依赖及插件版本统一维护在 `gradle/libs.versions.toml`；凭据放 `local.properties` 或 CI secrets。

## Data Field 契约

新增或修改 Data Field 时，以下位置必须保持一致：

- `res/xml/extension_info.xml` 中声明的 `typeId`；
- `DataFieldRegistry` 返回的实现及其 `typeId`；
- `docs/data-fields.md` 中的字段说明；
- registry 契约测试中的集合相等、ID 唯一性与 Extension ID 格式检查。

每个字段明确处理正常输入、缺失值、断线、`NotAvailable`、暂停/重置、单位制与取消。预览数据固定且可复现，不依赖随机数、时钟或网络。字段专用资源使用字段前缀，例如 `power_balance_icon`。

## 验证

仓库拥有 Gradle wrapper 后，在 Windows 使用以下命令：

```powershell
# 快速反馈：纯 JVM 测试
.\gradlew.bat testDebugUnitTest

# 提交前完整验证
.\gradlew.bat lint test assembleDebug
```

若项目提供格式化任务，修改 Kotlin/Gradle 文件后也运行该任务；以 `gradlew.bat tasks` 和 CI workflow 中的实际任务名为准，不臆造命令。当前尚无 wrapper 或 `app` 模块时，文档检查是唯一可执行验证，并在交付说明中明确这一点。

涉及设备行为时，在 Karoo 真机上至少确认：扩展可被发现、字段可添加、实时值与 `NotAvailable` 正常、图形字段各尺寸与点击行为正常、退出/重新进入及断开后无残留订阅。真机 smoke test 不能由 JVM 测试替代。

## 完成定义

一次变更完成时应满足：

- 改动位于正确切片，入口类只保留编排逻辑；
- 新增/修改计算、状态与序列化行为有正常值和边界测试；
- Data Field 的 XML、registry、文档和契约测试同步；
- 图形字段包含 deterministic preview；新增设置包含默认值与迁移/兼容策略；
- 相关格式化、lint、测试和 debug build 均通过，或清楚记录无法运行的原因；
- 需要真机验证的改动附带 smoke test 结果或待验证项；
- 未提交 token、keystore、`local.properties` 或其他凭据。

保持单模块，直到出现独立发布物、稳定且被多处复用的纯 Kotlin 边界、可测量的构建瓶颈或确实需要隔离的重量级依赖。拆模块前先记录问题、收益和回退方式。
