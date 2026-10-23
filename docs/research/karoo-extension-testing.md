# Karoo Extension 测试与调试说明

> 更新：2026-10-19。完整的真机连接、断点、Logcat 采集与离线日志方案见
> [Karoo 真机调试与日志调研](karoo-device-debugging-and-logs.md)。
> 室外实际使用应采用指南中的设备端文件记录方案；USB 只用于安装和事后导出。
> 文件日志能力仍待实施，不能仅凭当前 APK 的 Logcat 保证保存整次骑行证据。

## 结论

Karoo Extension 不是普通的 Android Activity 应用。Extension 的入口是注册在
`AndroidManifest.xml` 中、带有 `io.hammerhead.karooext.KAROO_EXTENSION` intent
filter 的 service；Karoo OS 读取同一 service 上的 `EXTENSION_INFO` 元数据，随后按
`extension_info.xml` 中声明的能力发现扩展。

因此，Android Studio 的 Run 按钮不能单独证明 Extension 已经工作：它通常只会尝试
启动 launcher Activity，而本项目目前没有 Activity。Extension 的端到端验证需要把
debug APK 安装到支持 Extensions 的 Karoo/Karoo 2 设备，再从 Karoo 的扩展入口或数据页
配置中观察结果。

## 当前仓库的具体情况

- `app/src/main/AndroidManifest.xml` 只声明了 `MyKarooExtension` service，没有
  launcher Activity。
- `app/src/main/res/xml/extension_info.xml` 与 `DataFieldRegistry` 已注册四个图形字段：
  `rear-cog-teeth`、`climber-field-1`、`climber-field-2`、`concentric-dashboard`。
- `MyKarooExtension` 已负责连接 `KarooSystemService`、注册字段、销毁时取消任务和断开；
  没有可由 Run 直接启动的页面。安装后应由 Karoo 宿主发现和使用字段。
- applicationId 为 `com.jxguo92.mykarooextension`，Extension ID 为
  `my-karoo-extension`；附加调试器和按包过滤时使用前者。
- `scansDevices="false"` 表示它也不会作为设备扫描扩展参与传感器发现。

## 推荐的测试分层

### 1. JVM 单元测试

对 Calculator、Formatter、State 转换和 registry 契约写普通 unit test。这一层不需要
Karoo 设备，重点覆盖正常值、缺失值、`NotAvailable`、暂停/重置和取消行为。

### 2. 构建与 APK 检查

至少执行 `./gradlew :app:assembleDebug`，确认 manifest、资源和 SDK 依赖可以打包。
构建产物是 `app/build/outputs/apk/debug/app-debug.apk`。

### 3. Android 渲染检查

仓库已有 `ConcentricRenderInstrumentation`，用于检查真实 Android Canvas、RemoteViews
和固定预览位图。执行方法见
[Concentric 渲染验证记录](../../demo/dashboard/concentric/validation/README.md)。
模拟器可以验证这些 Android 渲染行为，但不能替代 Karoo 宿主、真实数据流及设备生命周期验收。

### 4. Karoo 真机安装与发现

官方 SDK 示例使用 `./gradlew app:installDebug` 安装 sample app；对本项目可使用
`adb install -r app/build/outputs/apk/debug/app-debug.apk`，或 Android Studio 选择已
连接的 Karoo 设备安装 app；本项目无 Activity，可将 Run 配置的 Launch 设为 Nothing。
安装后在 Karoo 上启用扩展、添加字段；若没有发现再检查日志和扩展重新加载，必要时重启设备。
检查：

1. 扩展是否出现在 Karoo 的 Extensions/应用发现入口；
2. 若声明了 `DataType`，是否能在 Profiles → Data Pages → Add Data Field →
   More Data/Extensions 中看到；
3. 添加字段后，正常数据、无数据/断线、暂停和重新进入页面是否正确；
4. 从配置页退出、重新进入以及重启设备后，service 和订阅是否仍然正常。

### 5. Logcat 与断点调试

连接 Karoo 后使用 Android Studio Logcat，或通过 `adb logcat` 查看应用进程和 Karoo
系统进程的错误。重点搜索 applicationId、扩展 ID、`KarooExtension`、`ExtensionInfo`
以及 XML/resource 解析异常。若扩展完全不出现，先查 APK 是否安装、service 是否导出、
intent action 和 `EXTENSION_INFO` 是否拼写一致；若扩展出现但字段不出现，先查
`extension_info.xml` 的 `DataType` 声明。

断点调试时，安装 debug APK，让 Karoo 启动扩展，然后在 Android Studio 使用
`Attach debugger to Android process` 选择 `com.jxguo92.mykarooextension`。
具体步骤、启动时等待调试器及清理方法见上述真机调试指南。

## 当前真机验收重点

四个字段的具体行为见 [Data Fields](../data-fields.md)。至少确认扩展可发现、字段可添加、
正常值与 `NotAvailable`、断线、暂停/结束、各布局尺寸、退出重进后订阅取消；
Concentric Dashboard 还需确认整页渲染、日夜主题传播、Wi-Fi/手机天气链路。
将 Karoo OS 版本、APK 版本、复现步骤、日志和截图一起记录；不能把 JVM 或模拟器结果当作真机通过。

## 来源

- Hammerhead 官方 `karoo-ext` README：Extension service、`EXTENSION_INFO`、
  `extension_info.xml` 示例，以及 `app:installDebug` 安装方式：
  https://github.com/hammerheadnav/karoo-ext/blob/master/README.md
- Hammerhead 官方 `karoo-ext` API 文档入口：
  https://hammerheadnav.github.io/karoo-ext/
- Hammerhead 官方 Extension 模板：
  https://github.com/hammerheadnav/karoo-ext-template
- Android 官方附加调试器说明：
  https://developer.android.com/studio/debug#attach-debugger
- Android 官方 Run 配置说明（Launch → Nothing）：
  https://developer.android.com/studio/run/rundebugconfig

