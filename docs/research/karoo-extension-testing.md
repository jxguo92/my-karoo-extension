# Karoo Extension 测试与调试说明

## 结论

Karoo Extension 不是普通的 Android Activity 应用。Extension 的入口是注册在
`AndroidManifest.xml` 中、带有 `io.hammerhead.karooext.KAROO_EXTENSION` intent
filter 的 service；Karoo OS 读取同一 service 上的 `EXTENSION_INFO` 元数据，随后按
`extension_info.xml` 中声明的能力发现扩展。

因此，Android Studio 的 Run 按钮不能单独证明 Extension 已经工作：它通常只会尝试
启动 launcher Activity，而本项目目前没有 Activity。Extension 的端到端验证需要把
debug APK 安装到支持 Extensions 的 Karoo/Karoo 设备，再从 Karoo 的扩展入口或数据页
配置中观察结果。

## 当前仓库的具体情况

- `app/src/main/AndroidManifest.xml` 只声明了 `MyKarooExtension` service，没有
  launcher Activity。
- `app/src/main/res/xml/extension_info.xml` 目前只有扩展级元数据，没有 `DataType`，
  所以不会产生可添加的数据字段。
- `MyKarooExtension` 目前只是空的 `KarooExtension` 子类，适合作为“能被发现的空扩展”
  骨架，不会在 Android 模拟器中显示页面。
- `scansDevices="false"` 表示它也不会作为设备扫描扩展参与传感器发现。

## 推荐的测试分层

### 1. JVM 单元测试

对 Calculator、Formatter、State 转换和 registry 契约写普通 unit test。这一层不需要
Karoo 设备，重点覆盖正常值、缺失值、`NotAvailable`、暂停/重置和取消行为。

### 2. 构建与 APK 检查

至少执行 `./gradlew :app:assembleDebug`，确认 manifest、资源和 SDK 依赖可以打包。
构建产物是 `app/build/outputs/apk/debug/app-debug.apk`。

### 3. Karoo 真机安装与发现

官方 SDK 示例使用 `./gradlew app:installDebug` 安装 sample app；对本项目可使用
`adb install -r app/build/outputs/apk/debug/app-debug.apk`，或 Android Studio 选择已
连接的 Karoo 设备运行 app configuration。安装后应重启或至少让 Karoo 重新加载扩展，
然后检查：

1. 扩展是否出现在 Karoo 的 Extensions/应用发现入口；
2. 若声明了 `DataType`，是否能在 Profiles → Data Pages → Add Data Field →
   More Data/Extensions 中看到；
3. 添加字段后，正常数据、无数据/断线、暂停和重新进入页面是否正确；
4. 从配置页退出、重新进入以及重启设备后，service 和订阅是否仍然正常。

### 4. Logcat 调试

连接 Karoo 后使用 Android Studio Logcat，或通过 `adb logcat` 查看应用进程和 Karoo
系统进程的错误。重点搜索 applicationId、扩展 ID、`KarooExtension`、`ExtensionInfo`
以及 XML/resource 解析异常。若扩展完全不出现，先查 APK 是否安装、service 是否导出、
intent action 和 `EXTENSION_INFO` 是否拼写一致；若扩展出现但字段不出现，先查
`extension_info.xml` 的 `DataType` 声明。

## 对本项目的下一步建议

当前阶段可把“空扩展能否安装和被发现”作为第一条 smoke test；它不会有可见 UI，这是
预期行为。开始实现 Data Field 后，再同步增加：

- `extension_info.xml` 中的 `DataType`；
- Kotlin registry 中相同的 type ID；
- `docs/data-fields.md`；
- registry 集合、唯一性和 Extension ID 契约测试；
- Karoo 真机上的字段添加、实时值和 `NotAvailable` 检查。

## 来源

- Hammerhead 官方 `karoo-ext` README：Extension service、`EXTENSION_INFO`、
  `extension_info.xml` 示例，以及 `app:installDebug` 安装方式：
  https://github.com/hammerheadnav/karoo-ext/blob/master/README.md
- Hammerhead 官方 `karoo-ext` API 文档入口：
  https://hammerheadnav.github.io/karoo-ext/
- Hammerhead 官方 Extension 模板：
  https://github.com/hammerheadnav/karoo-ext-template

