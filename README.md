# My Karoo Extension

面向 Hammerhead Karoo 的自定义扩展项目。项目将先从定制 Data Field 开始，并为后续加入地图层、骑行提醒和设备接入保留清晰的扩展路径。

> 当前状态：项目正在初始化，仓库中暂时只有结构调研与开发约定，尚无可安装 APK。

## 设计原则

- 单一 `app` 模块，按功能垂直切片；每个 Data Field 的接入、计算、状态、视图和测试彼此就近。
- `KarooExtension` 只负责注册与生命周期编排，业务计算保持为可独立测试的纯 Kotlin。
- 实时数据与运行时状态使用 Coroutines、Flow 和不可变状态；需要跨重启保留的设置使用 DataStore。
- 设置页使用 Jetpack Compose；只有图形 Data Field 才引入 Glance/RemoteViews。
- 初期使用构造函数注入和小型 `AppContainer`，不预先引入大型 DI 框架或多模块结构。
- `extension_info.xml` 与 Kotlin registry 通过契约测试保持同步，减少字段在设备上无法发现的配置错误。

完整的调研依据与结构决策见 [Karoo Extension 项目结构调研](docs/research/karoo-extension-project-structure.md)。Agent 的目录约定、边界和完成标准见 [AGENTS.md](AGENTS.md)。

## 计划中的技术栈

- Kotlin
- Hammerhead [`karoo-ext`](https://github.com/hammerheadnav/karoo-ext) SDK
- Kotlin Coroutines / Flow
- Jetpack Compose（应用与设置界面）
- Jetpack DataStore（持久化设置）
- Jetpack Glance（仅图形 Data Field）
- Gradle Kotlin DSL + Version Catalog

## 开发

项目初始化将以官方 [`karoo-ext-template`](https://github.com/hammerheadnav/karoo-ext-template) 为骨架，使用正式 namespace/applicationId 替换文档中的 `com.example.mykaroo`，并保留单一 `app` 模块。

完成 Gradle 初始化后，Windows 下的常用验证命令为：

```powershell
# 快速运行 JVM 单元测试
.\gradlew.bat testDebugUnitTest

# 提交前检查
.\gradlew.bat lint test assembleDebug
```

这些命令在 Gradle wrapper 和 `app` 模块加入仓库后才可运行。依赖 GitHub Packages 的本地认证信息应放在 Gradle 属性或环境变量中，具体属性名以采用的 SDK 版本和官方说明为准；不要把 token、keystore 或 `local.properties` 提交到仓库。

新增 Data Field 时，需要同时完成：

1. 在独立的 `feature/datafield/<field_name>/` 切片中实现字段和纯计算逻辑。
2. 在 `extension_info.xml` 声明字段，并在 `DataFieldRegistry` 注册相同的 `typeId`。
3. 为计算边界、Flow 状态与 XML/registry 一致性添加测试。
4. 在 `docs/data-fields.md` 记录数据来源、单位、不可用状态和预览行为。
5. 若为图形字段，提供固定、可复现的预览，并在 Karoo 真机检查尺寸与交互。

## 构建与安装

源码骨架加入后，可通过以下命令生成 debug APK：

```powershell
.\gradlew.bat assembleDebug
```

APK 通常位于 `app/build/outputs/apk/debug/`。安装与设备调试步骤将在首个可运行版本完成后补充；当前仓库没有可供安装的构建产物。

## 测试策略

- Calculator/Formatter 表驱动测试：正常值、缺失值、边界、单位制和非法输入。
- Data Field 契约测试：XML 与 Kotlin `typeId` 集合相等且无重复。
- Flow/state 测试：输入顺序、断线、`NotAvailable` 和取消行为。
- DataStore 测试：默认值、旧数据兼容与迁移。
- 图形字段逻辑测试与 Karoo 真机 smoke test。

CI 计划在 push 和 pull request 时执行 lint、单元测试和 debug build；tag 发布流程负责签名 release APK，并从同一构建配置生成扩展 `manifest.json`，避免手工同步包名、版本和下载地址。

## 发布计划

首个发布版本将通过 Git tag 触发自动构建，并发布签名 APK、`manifest.json`、图标和截图。签名文件与密码只存放在 CI secrets 中。正式发布流程建立前，不应手工维护多个相互独立的版本号或下载 URL。
