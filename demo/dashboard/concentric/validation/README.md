# Concentric Dashboard 渲染验证

2026-09-30，在 Android API 35 模拟器运行生产 `ConcentricView` Canvas/RemoteViews 渲染器。

- `preview-day.png` / `preview-night.png`：计划固定预览，同一份数据，480×638。
- `missing-day.png` / `missing-night.png`：全部读数缺失，静态轨道与刻度仍在，无动态填充、功率指针或方向提示。
- `long-day.png` / `long-night.png`：极端长读数缩小、不省略，检查底部单位与相邻列不重叠。
- 已人工查看六张图，确认280°同心弧、当前功率区、径向渐隐、踏频填充、指北针、风箭头、日夜配色与原型 D/DL 对应；系统字体按计划使用 condensed/sans-serif。
- 仪器检查：实际 RemoteViews 可 inflate；每次渲染独立位图；480×638、960×1276、240×319、480×100、非正尺寸的上限和降级；非整页空白位图透明。

复现（已有运行中的 emulator，命令中的设备参数按实际设备调整）：

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w com.jxguo92.mykarooextension.test/com.jxguo92.mykarooextension.feature.datafield.concentricdashboard.ConcentricRenderInstrumentation
adb -s emulator-5554 exec-out run-as com.jxguo92.mykarooextension tar -C files/concentric-previews -cf - . > /tmp/concentric-previews.tar
tar -xf /tmp/concentric-previews.tar -C demo/dashboard/concentric/validation
```

模拟器检查不替代 Karoo 真机：字段发现/添加、5×2整页、系统主题传播、真实传感器与断线、暂停/结束、Wi-Fi/手机网络天气、反复进出页面订阅释放和位图 Binder 传输稳定性仍待确认。
