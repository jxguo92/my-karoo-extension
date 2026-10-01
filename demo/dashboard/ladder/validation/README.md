# Ladder Dashboard 渲染验证

生产渲染入口是 `LadderRenderer`，固定示例见 `LadderPreview`。已有 `ConcentricRenderInstrumentation` 现在同时运行两种 Dashboard 的 Canvas/RemoteViews 检查。

2026-10-01：仪器 APK 编译检查已加入；本地没有连接的 Android 模拟器或 Karoo，也没有可用 AVD，因此尚未执行实际渲染。此目录没有已验证的截图。JVM 计算/归约测试不能证明 Canvas 字体、裁切、Binder 提交或真实设备订阅行为。

已有设备后运行（设备编号按实际调整）：

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w com.jxguo92.mykarooextension.test/com.jxguo92.mykarooextension.feature.datafield.concentricdashboard.ConcentricRenderInstrumentation
```

仪器将 `preview.png`、`missing.png`、`long.png` 导出到应用私有 `files/ladder-previews/`。检查真实 RemoteViews inflate、非整页无 drawable、每视图复用同一位图、有效尺寸与非正尺寸回退。复制 PNG 到本目录后再人工检查：中央大字与圆弧、速度/心率指示条和刻度、灯珠、风向/指北针、辅助值与右侧4px单位间距、长数值缩小后不重叠。

Karoo 真机仍需完成 [计划](../../../../docs/plans/ladder-full.md) 中的整页资格、传感器/骑行状态、朝向与天气、重复进出订阅取消及 Binder/内存验收。
