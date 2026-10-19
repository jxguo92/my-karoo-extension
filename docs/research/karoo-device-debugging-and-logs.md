# Karoo Extension 真机调试与日志指南

> 调研日期：2026-10-19。依据为 Hammerhead、Android/AOSP 和日志库作者的一手文档与源码。
> 以下区分官方确认能力、Android 通用方法和项目建议；命令尚未在 Karoo 真机上执行。

## 推荐工作流

室外实测采用 **安装带文件诊断功能的 debug APK → 验证本地日志与导出 → 拔 USB 正常骑行 → 回来连接电脑导出 → 按事件时间线分析**。USB 仅用于出发前安装和回来后取证，骑行期间无需电脑、调试器、Wi-Fi 或 ADB 连接。该方案需要新增应用内文件记录能力，本项目目前尚未实现，具体设计和操作见下文“室外实测方案”。

室内可复现问题时，再使用 USB ADB、Android Studio 附加进程和持续保存完整 Logcat。进程内断点适合定位状态和数据转换，完整系统日志适合发现服务绑定、跨进程显示、崩溃与重启问题。

`karoo-ext` 的第三方代码运行在第三方应用自己的进程中，因此应调试本项目进程，而不是假设扩展代码运行在 Karoo 骑行应用进程里。图形字段最终由其他进程显示，故只收集扩展进程日志也可能遗漏问题。来源：[Hammerhead SDK README](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/README.md)。

## 本项目当前状态

- applicationId：`com.jxguo92.mykarooextension`。
- Extension ID：`my-karoo-extension`；入口为 `extension/MyKarooExtension.kt`。
- 使用 SDK `1.1.9`；Manifest 没有 launcher Activity，也没有为 service 配置独立 `android:process`。
- 已有实际 Data Field，应通过 Karoo 的字段配置和骑行页面触发，而非期待安装后自动打开设置页。
- 当前业务代码仅发现同心仪表字段的一处 `Log.w`，tag 为 `concentric-dashboard`，消息为 `Unable to submit dashboard frame; will retry`；目前没有日志导出页、文件轮转或持久化日志设施。

这些是调研时的仓库检查结果，后续代码变更后应重新核对：`app/build.gradle.kts`、`gradle/libs.versions.toml`、`app/src/main/AndroidManifest.xml`、`MyKarooExtension.kt` 和 `ConcentricDataField.kt`。

## 连接与安装

### USB：首选且有 Karoo 官方依据

Karoo 2 与新款 Karoo 的开发者选项开启步骤相同：`Settings → About → Build Number` 连点 7 次，返回 Settings，进入 `Developer Options` 并开启总开关。然后开启 `USB Debugging`，用支持数据传输的 USB-C 线连接电脑。来源：[新款 Karoo 开发者选项](https://support.hammerhead.io/hc/en-us/articles/30696553134363-Karoo-Enabling-Developer-Options)、[Karoo 2 开发者选项](https://support.hammerhead.io/hc/en-us/articles/360058885393--Karoo-2-Enabling-Developer-Options)、[Hammerhead USB 数据连接说明](https://support.hammerhead.io/hc/en-us/articles/360005925374-Downloading-a-FIT-file-from-Karoo-to-a-PC)。

电脑安装 Android SDK Platform Tools，执行：

```bash
adb devices -l
```

设备若弹出电脑 RSA 指纹授权框，应在设备上确认。目标行应显示 `device`；`unauthorized` 表示授权尚未完成，`offline` 表示链路未正常响应。空列表优先排查数据线、USB 接口和 USB Debugging。多台 Android 设备同时连接时，后文每条命令的 `adb` 后加 `-s 设备序列号`。来源：[Android ADB](https://developer.android.com/tools/adb)。

Hammerhead 的 FIT 导出文档另有 `Developer Options → Select USB configuration → MTP` 步骤；那是通过文件管理器访问存储的步骤，**不能据此把 MTP 写成 ADB 的必需条件**。遇到 USB 文件访问问题时按该文档操作；ADB 是否建立以 `adb devices -l` 为准。[Hammerhead FIT 导出说明](https://support.hammerhead.io/hc/en-us/articles/360005925374-Downloading-a-FIT-file-from-Karoo-to-a-PC)

在仓库根目录构建并安装（macOS/Linux）：

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可用 `./gradlew :app:installDebug`（Windows 为 `.\gradlew.bat :app:installDebug`）。Hammerhead SDK sample 采用 `app:installDebug`；`adb install -r` 覆盖安装并保留应用数据。若遇签名不一致，先确认设备上安装的是哪个签名版本，避免为排错直接卸载而丢失数据。来源：[SDK sample 安装命令](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/README.md)、[Android ADB 安装命令](https://developer.android.com/tools/adb)。

### Android Studio：安装与断点调试

本项目没有 Activity。Run configuration 可设为 `Launch: Nothing`，完成安装后在 Karoo 手工添加字段或进入含字段的数据页，使扩展进程启动。再选择 `Run → Attach debugger to Android process`，选目标 Karoo 和 `com.jxguo92.mykarooextension`。Kotlin/Java 调试器可检查变量、线程、调用栈，设置条件断点及不暂停程序的 logging breakpoint。必须安装 debuggable 的 debug 变体；release APK 通常不可这样附加。来源：[Android Studio 调试指南](https://developer.android.com/studio/debug)、[Run 配置的 Launch 选项](https://developer.android.com/studio/run/rundebugconfig#app)、[debug 变体默认可调试](https://developer.android.com/build/build-variants#build-types)。

建议断点位置是扩展 `onCreate`、字段 `startStream`/`startView`、Flow 的状态分支与 emitter 取消回调。断点暂停会改变实时数据和 Binder/协程时序，诊断掉帧或超时时优先 logging breakpoint 和时间戳日志。这是结合本项目生命周期作出的调试建议，并非 Hammerhead 的时序保证。

如果附加时已经错过 `onCreate`，可在扩展进程下次启动前设置等待调试器：

```bash
adb shell am set-debug-app -w com.jxguo92.mykarooextension
# 让 Karoo 重新启动扩展，再用 Studio Attach；此设置不会主动启动进程
# 排查完毕或放弃附加时都执行清理
adb shell am clear-debug-app
```

如扩展仍在运行，可先在 Karoo 的扩展详情 Force Quit，再设置等待并通过宿主重新触发；宿主是否立即重启须实测。等待期间字段会停住，也可能触发宿主超时，适合室内启动流程排查。命令依据：[Android activity manager](https://developer.android.com/tools/adb#am)。

### Wi-Fi ADB：Android 通用备选，需要先在设备验证

Android 官方提供先用 USB 建立连接，再切换 TCP/IP 的方式：

```bash
# 先通过 USB 确认 adb devices 中状态为 device
adb tcpip 5555
# 电脑和 Karoo 在同一可信 Wi-Fi；将示例地址替换为 Karoo 实际 IP
adb connect 192.168.1.123:5555
adb devices -l
```

确认网络设备连接后可移除 USB，后续安装、Logcat、调试命令显式使用 `adb -s 192.168.1.123:5555 ...`。结束网络调试后可重新接入 USB，运行 `adb -s 192.168.1.123:5555 usb` 使 adbd 返回 USB 模式；以实际连接状态检查结果。来源：[Android ADB：初次 USB 后无线连接](https://developer.android.com/tools/adb#wireless)。

这是 Android 通用方法：本次没有找到 Hammerhead 对 Karoo 当前固件开放 TCP/IP ADB 的保证，也未验证真机。不要假设 Karoo 提供 Android 11 的二维码/配对码 `Wireless debugging` 菜单。网络切换、休眠和热点隔离均可能导致断连；它适合室内复现，不是脱机骑行日志的替代品。

### Companion/Extension Library：安装渠道，不提供调试器或 Logcat

新款 Karoo 可把手机本地 APK 或 APK URL 分享给 Companion App，在 Karoo 确认安装。官方要求 Karoo `1.538.2049+`、Android Companion `1.36.0+`/iPhone Companion `1.12.0+`，设备连 Wi-Fi、手机有网络；此方式不适用于 Karoo 2。APK URL 官方只验证了 GitHub。来源：[Companion App - Sideloading](https://support.hammerhead.io/hc/en-us/articles/31576497036827-Companion-App-Sideloading)。

Extension Library 同时支持 Karoo 2 和新款 Karoo，入口为 `Apps → Extensions`，应用详情提供 Install、Uninstall 与 Force Quit。适合分发和重启应用验证；侧载或 Library 安装本身不会建立电脑调试连接。来源：[Extensions Library](https://support.hammerhead.io/hc/en-us/articles/34676015530907-Karoo-OS-Extensions-Library)、[Hammerhead Extensions FAQ](https://support.hammerhead.io/hc/en-us/articles/31150180125083-FAQ-Hammerhead-Extensions)。

## 查看和记录 Logcat

### Android Studio 实时查看

连接真机后打开 `View → Tool Windows → Logcat`，选 Karoo。先按包名过滤：

```text
package:com.jxguo92.mykarooextension
```

崩溃查询可使用 `is:crash`；若确认项目识别正确也可用 `package:mine`。跨进程显示或系统绑定问题应清除 package 过滤，并结合故障时间、关键消息查找。Studio Logcat 显示进程结束/重启标记；持续落盘使用下节命令。来源：[Android Studio Logcat](https://developer.android.com/studio/debug/logcat)。

### 命令行：先保存完整现场，再筛选

先在电脑终端准备目录并**保持采集运行**：

```bash
mkdir -p captures/karoo
adb logcat -b all -v threadtime > captures/karoo/repro-all.txt
```

Windows PowerShell：

```powershell
New-Item -ItemType Directory -Force captures/karoo | Out-Null
adb logcat -b all -v threadtime > captures/karoo/repro-all.txt
```

此时在设备复现问题，完成后按 `Ctrl+C` 停止。`-b all` 读取设备向当前权限开放的所有缓冲区；`threadtime` 保留日期、时间、PID、TID、优先级和 tag。不要预先只保留 Error：关键的状态转换通常是 Info/Debug。设备固件对可访问日志和选项可能有差异，可先运行 `adb logcat --help`。来源：[Logcat 命令行说明](https://developer.android.com/tools/logcat)。

若问题已经发生、还没重启或清日志，可立即导出当前缓冲区：

```bash
adb logcat -b all -d -v threadtime > captures/karoo/buffer-after-failure.txt
adb logcat -b crash -d -v threadtime > captures/karoo/crash-buffer.txt
```

缓冲区是有界循环日志，可能覆盖较早记录。`-d` 只导出当前内容然后退出，无法恢复已覆盖的日志；因此稳定复现时优先提前持续采集。不要在唯一故障现场先执行 `adb logcat -c`，它会清除日志。[Android Logcat](https://developer.android.com/tools/logcat)

按 tag 快速观察本项目现有警告：

```bash
adb logcat -v threadtime 'concentric-dashboard:V' '*:S'
```

`'*:S'` 必须加引号，避免 zsh 把星号展开为文件名。此命令仅显示该 tag，不会显示其他字段、SDK 或系统崩溃，不能代替完整采集。tag 与 priority filter 的语法来源：[Android Logcat](https://developer.android.com/tools/logcat)。

辅助：只观察当前扩展 PID（macOS/Linux）：

```bash
karoo_pid=$(adb shell pidof com.jxguo92.mykarooextension | tr -d '\r')
# 先确认 karoo_pid 为一个非空数字；否则先在 Karoo 进入字段页面
adb logcat --pid="$karoo_pid" -v threadtime
```

Windows PowerShell：

```powershell
$karooPid = [string](adb shell pidof com.jxguo92.mykarooextension)
$karooPid = $karooPid.Trim()
# PowerShell 的 $PID 是系统变量，不能用于此处
if ($karooPid -match '^\d+$') {
    adb logcat --pid=$karooPid -v threadtime
} else {
    Write-Error '未找到唯一的扩展进程，请先在 Karoo 进入字段页面'
}
```

进程崩溃重启后 PID 会改变，需重新查询并执行；只按 PID 会遗漏系统/宿主进程和旧 PID 日志。设备若无 `pidof`，改用 `adb shell ps` 找到进程。`--pid` 选项应以真机 `adb logcat --help` 为准；依据：[AOSP Logcat 源码](https://android.googlesource.com/platform/system/logging/+/refs/heads/main/logcat/logcat.cpp)。

### SDK 自带日志的陷阱：有日志调用不等于一定输出

SDK `1.1.9` 的 `KarooExtension` 使用 Timber，记录 bind、stream/view 开始和停止等事件；`KarooSystemService` 记录绑定、连接、断开、consumer 注册/移除等事件。它们把 `KarooExtension:`/`KarooSystem:` 放在**消息正文**里，并未通过 `Timber.tag(TAG)` 指定固定 Android tag。来源：[KarooExtension 1.1.9](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt)、[KarooSystemService 1.1.9](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/lib/src/main/kotlin/io/hammerhead/karooext/KarooSystemService.kt)。

Timber 默认没有安装任何 Tree；`DebugTree` 才把日志写出并根据调用类生成 tag。本项目没有 `Timber.plant` 初始化，因此不能承诺 SDK 日志默认可见，更不能根据没看到它们判断扩展没启动。来源：[Timber 作者 README](https://github.com/JakeWharton/timber)。

如果要启用，可在 `MyKarooExtension.onCreate()` 的 `super.onCreate()` 后、`karooSystem.connect()` 前添加以下逻辑；直接使用 Timber 时，应在 `libs.versions.toml` 声明并引用直接依赖，确保项目生成 `BuildConfig`，补上本项目 `BuildConfig` 与 `timber.log.Timber` 的 import。本轮调研没有修改应用；示例不是已经启用的功能。

```kotlin
if (BuildConfig.DEBUG && Timber.treeCount == 0) {
    Timber.plant(Timber.DebugTree())
}
```

`treeCount` 与 `DebugTree` 的行为依据：[Timber 5.0.1 源码](https://github.com/JakeWharton/timber/blob/5.0.1/timber/src/main/java/timber/log/Timber.kt)。后续引入版本以实际依赖解析结果为准。

启用后先保留完整日志，再按消息正文检索 `KarooExtension:`、`KarooSystem:`。`adb logcat 'KarooExtension:V' ...` 是按 Android tag 过滤，可能漏掉这些正文消息。

## 写日志应该记录什么

以下为本项目建议，尚未实现。少量业务诊断可直接使用 Android `Log.i`/`Log.w`/`Log.e`，异常使用包含 `Throwable` 的重载保留堆栈，不必为了日志先引入大型框架。API 依据：[Android Log](https://developer.android.com/reference/android/util/Log)。

优先记录状态边界而非每一帧：

- 扩展进程启动、版本、SDK 版本，Karoo 连接/断开及重连结果。
- 字段 typeId、stream/view 创建与取消、固定会话 ID、当前订阅数；便于发现重复订阅与退出后残留任务。
- 输入从正常到缺失/`NotAvailable` 的转换、暂停/重置、单位和字段尺寸改变。
- 渲染失败、重试次数、降级和恢复；HTTP 请求结果、耗时、超时类型、缓存命中与数据年龄。
- 异常堆栈以及用户触发动作；每条带时间和稳定 tag，必要时带递增事件序号。

尤其应给 `KarooFlowAdapter` 的 `onError` 记录原始错误：目前 `streamStates` 直接转换为 `NotAvailable`，另两个 consumer 直接关闭 Flow，会丢失排错原因。图形日志记录 `ViewConfig` 的尺寸与 preview 标记，网络日志记录选择的传输路径和耗时；避免每帧打印整个配置、输入对象或位置。

高频数值使用节流/抽样；常规级别记录状态变化与汇总。不要写 token、认证头、私钥、和风凭据或完整带凭据 URL；位置、健康和骑行数据仅记录排错必需内容。日志输出本身不应阻塞流处理和图形渲染。

## 室外实测方案：安装 → 拔线使用 → 导出 → 分析

电脑执行的 `adb logcat > 文件` 保存的是电脑端文件；断开 ADB 后不会继续替你记录。回家再执行 `logcat -d` 只能读仍未覆盖的内容，不能当作完整骑行历史。来源：[Android Logcat 缓冲区与导出语义](https://developer.android.com/tools/logcat)。

### 1. debug APK 需要先具备的记录能力

**以下是待实施设计，不是当前 APK 已有功能。** 最小方案不需要新增 Activity、网络上传、前台服务或额外存储权限；在现有扩展进程执行期间自动记录即可，进程停止时不会继续采样。

- debug 构建自动启用诊断，release 默认关闭。应用进程初始化时安装一次日志设施，在首次 SDK 连接前就可用；不要随字段关闭删除日志。
- 文件放 `context.filesDir/diagnostics/`，采用 UTF-8 JSON Lines（一行一个完整事件）。使用持久文件目录，避免放 `cacheDir`。重启保留已落盘文件；卸载、清除应用数据会删除文件，因此先导出再升级/重装。Android 官方确认该目录不需要额外存储权限：[应用私有存储](https://developer.android.com/training/data-storage/app-specific)。
- 初始建议：单文件最多 4 MiB，总计最多 8 个文件（含当前文件），达到上限删除最旧文件；另留一个小型限额的最后崩溃记录。每个分卷都带版本和会话元数据。32 MiB 是建议的保留预算，不能保证任意强度日志下都保留整次骑行，长距离测试前要用实际写入速率估算。
- 普通记录由单个后台 writer 串行处理，队列有界，按时间/大小批量 flush，初始目标为正常负载下 5 秒内写出；记录队列丢弃数、写入失败和文件轮转。优先保留错误与状态事件，高频样本可丢弃。不能因磁盘满或日志异常使 Data Field 崩溃。
- 每条包含 `wallTimeUtc`、`elapsedRealtimeMs`、`processSessionId`、进程内递增 `seq`、`event`、`typeId`（适用时）和结构化属性。每次进程重启产生新 session，跨重启按 UTC/启动元数据拼接；单调时间用于同一启动周期内计算间隔，不能跨设备重启直接相减。
- `session_start` 包含 application/extension/SDK 版本、构建标识（建议 Git SHA）、设备型号、系统版本和日志格式版本，不记录设备序列号、凭据或完整位置。记录 `ride_state` 和首次 view 的尺寸/preview 标记，以区分预览、室内和实际骑行。
- 原有业务警告与 SDK Timber 都送到同一文件记录器；仅安装 `DebugTree` 只会输出到 Logcat，不能完成脱机持久化。接入文件 Tree，并让现有 `Log.w` 通过应用日志入口双写，或显式补充对应文件事件。
- 每 5 秒最多一条小型快照，建议包含当前可用值、各来源最后更新时间/数据年龄、连接状态、天气缓存年龄与路由；状态变化和异常立即入队。需要逐样本复现纯计算时，针对目标字段短期开更细采样，并验证存储和耗电成本。不要为了日志额外维持原本已取消的传感器订阅。

重点事件：`karoo_connected/disconnected`、`view_start/cancel`、`consumer_add/remove/error`、`ride_state`、`input_unavailable/recovered`、`render_failed/recovered`、`http_started/completed/failed`。字段的输入值/状态、派生结果与最后成功提交时间一同记录，才能区分数据源没更新、业务计算异常和显示提交异常。天气额外记录 Wi-Fi 直连或手机转发、状态码、耗时、重试与缓存年龄，不记录认证头和带密钥 URL。

未捕获异常应尽力同步写入单独的有界 crash 文件，然后**委托原有 UncaughtExceptionHandler**，保留 Android 默认崩溃处理。不能把普通异步入队当作异常落盘保证；也不能依赖 `onDestroy` 收尾。异常日志没有结束事件，只能说明会话中断，不能单独证明 Java 崩溃：ANR、系统杀进程、native crash、断电都可能没有回调，突然断电也可能损失已 flush 但尚未真正持久化的数据。依据：[UncaughtExceptionHandler](https://developer.android.com/reference/java/lang/Thread.UncaughtExceptionHandler)、[Service.onDestroy 生命周期限制](https://developer.android.com/reference/android/app/Service#onDestroy())。

若设备 API ≥30，可补充查询上一进程的 `ApplicationExitInfo`；必须检查运行时版本，不能把新 Android API 当作所有 Karoo 固件可用的能力。来源：[ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo)。

### 2. 出发前：确认记录和导出都能工作

完成上述能力后，在电脑构建安装 debug APK，清除“等待调试器”设置，在 Karoo 启用扩展并进入实际字段页面：

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am clear-debug-app
adb shell run-as com.jxguo92.mykarooextension pwd
adb shell run-as com.jxguo92.mykarooextension ls -l files/diagnostics
```

Windows 构建命令替换为 `.\gradlew.bat :app:assembleDebug`，其他命令相同。等待一次 flush，读取一个实际文件确认有 `session_start` 和 view/连接事件；先按下一节导出一次。随后做一次短暂拔线测试，回来确认脱线时段有新增事件，再进行长时间室外测试。

`run-as` 以本应用 UID 运行并进入应用数据目录，要求包已安装且可调试；固件也可以禁用它。首次必须在目标 Karoo 上验证，不能因为 APK 是 debug 就保证可用。若失败，应先设计应用受控导出入口，再出发；不应骑完才发现私有文件无法取回。来源：[AOSP run-as](https://android.googlesource.com/platform/system/core/+/master/run-as/run-as.cpp)。

### 3. 室外：拔 USB，按正常骑行条件使用

拔线并断开调试器；不要求保持 Wi-Fi，按真实使用方式连接手机与传感器。预先记下本次 APK/Git 版本和测试重点，例如手机转发天气、断线恢复、暂停/继续、字段切换。

出现问题时记下码表时间、字段、现象和当时动作；条件允许时拍照，并记录当时是否有手机连接/Wi-Fi。继续测试或正常结束骑行，不必为了日志保持 ADB。不要在骑行中操作调试命令。扩展进程重启后应自动建立新 session，已保存的旧会话仍受容量上限保护。

### 4. 回来：先保留现场，再导出应用文件

结束骑行后重新接 USB。在升级、卸载、清数据、Force Quit 或重启之前，先补抓仍保留的系统缓冲区和 bugreport；它们只补充回来时还能获取的现场，不能保证保留整段骑行的宿主日志。

macOS/Linux，使用每次不同的目录名（下面只是一次测试的示例）：

```bash
mkdir -p captures/karoo/outdoor-20261019-01
adb logcat -b all -d -v threadtime > captures/karoo/outdoor-20261019-01/logcat.txt
adb bugreport captures/karoo/outdoor-20261019-01
```

结束骑行并等待正常 flush 周期后，导出文件时可先只停止本扩展，以免边读取边轮转；这会丢失可能尚未写出的末尾记录，不应依赖它触发 `onDestroy`。先完成上述系统取证，并记下这次人为停止的时刻，分析时不把它算成骑行故障：

```bash
adb shell am force-stop com.jxguo92.mykarooextension
adb shell run-as com.jxguo92.mykarooextension ls -l files/diagnostics
adb exec-out run-as com.jxguo92.mykarooextension tar -C files -cf - diagnostics > captures/karoo/outdoor-20261019-01/diagnostics.tar
tar -tf captures/karoo/outdoor-20261019-01/diagnostics.tar
tar -xf captures/karoo/outdoor-20261019-01/diagnostics.tar -C captures/karoo/outdoor-20261019-01
```

设备的 `tar` 命令也需在出发前检查。若不可用，可用 `run-as ... ls` 列出文件，再逐个 `adb exec-out run-as 包名 cat files/diagnostics/实际文件名 > 本地文件`。不能直接假设 `adb pull /data/user/0/包名/files/...` 有权限，也不要依赖以应用 UID 向 `/data/local/tmp` 写文件的做法。`exec-out` 和 `am force-stop` 的依据：[Android ADB](https://developer.android.com/tools/adb)。

Windows PowerShell 创建目录使用 `New-Item -ItemType Directory -Force captures/karoo/outdoor-20261019-01 | Out-Null`。归档是二进制，统一通过 `cmd /c` 重定向，避免旧 PowerShell 文本管道改变字节：

```powershell
cmd /c "adb exec-out run-as com.jxguo92.mykarooextension tar -C files -cf - diagnostics > captures\karoo\outdoor-20261019-01\diagnostics.tar"
tar -tf captures/karoo/outdoor-20261019-01/diagnostics.tar
tar -xf captures/karoo/outdoor-20261019-01/diagnostics.tar -C captures/karoo/outdoor-20261019-01
```

先执行同样的系统取证、等待和停止扩展步骤，再运行上面的归档命令；其他 ADB 命令相同。Microsoft 文档说明 PowerShell **7.4 起**原生命令 stdout 重定向保留字节流，不应对 Windows PowerShell 5.1 套用此行为。[PowerShell 重定向](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_redirection)

核对命令退出码、归档可解开、包含预期会话且覆盖故障时段，再更新 APK。导出后要在 Karoo 重新启用/触发扩展；force-stop 后宿主恢复行为仍需真机确认。原始导出件保留不改，将人工复现说明放在同一目录。

### 5. 分析顺序和可回答的问题

1. 先检查采集质量：版本是否一致、故障时段是否仍在保留范围、是否有队列丢弃/写入失败、分卷是否缺失。JSONL 末尾可能因异常只有半行，保留原件，分析时报告并跳过破损行。
2. 按 session 和 seq 还原进程内顺序，用单调时间判断间隔；按 UTC 和人工记录对齐故障前后 1–2 分钟，设备时钟变化时以会话内顺序为准。
3. 对照 `输入/更新时间 → 状态转换 → 计算结果 → 最后提交`。输入停止与 consumer error 指向源头/连接；输入正常而无提交指向字段逻辑/渲染；提交成功也不能证明 Karoo 宿主已显示到屏幕。
4. 天气错误先看传输路径、超时、状态码、缓存年龄及重试；确认是不是回到电脑旁连 Wi-Fi 才恢复，避免把环境变化当作问题消失。
5. session 中断后有新 session 说明进程曾重新初始化；结合 crash 文件、仍可用的 Logcat/bugreport 和可用时的退出原因判断，不能只凭日志空白区就认定 crash/ANR。
6. 重复进入退出字段时，比较 consumer 的创建/移除和活跃计数，查取消后是否仍产出样本；记录来自已有订阅，不额外创建“仅用于监控”的长期订阅。

首版交付应包括纯 Kotlin 文件轮转/格式/容量测试、异常与坏行恢复、限流和失败降级验证，以及实际断 USB、关闭 Wi-Fi、扩展重启后仍可导出旧记录的 smoke test。整次骑行的宿主系统日志不是应用文件记录器能保证捕获的内容；若问题只发生在宿主，仍需结合现场时刻和 Hammerhead 的诊断渠道。

不要把未验证的 `nohup adb shell logcat ...` 或远端后台 shell 当作可靠脱机记录方案：它依赖 adbd、shell 生命周期、文件权限和固件行为，本次未得到 Karoo 官方保证。本项目目前也不存在可直接 `adb pull` 的应用日志路径。

## 崩溃、卡死与系统诊断

故障后尽快保存 Logcat；需要系统服务和 ANR 上下文时，再生成 Android bugreport：

```bash
adb bugreport captures/karoo
adb shell dumpsys package com.jxguo92.mykarooextension > captures/karoo/package.txt
adb shell dumpsys activity services com.jxguo92.mykarooextension > captures/karoo/services.txt
```

`bugreport` 汇集设备日志、堆栈及系统诊断，生成较大的报告；`dumpsys package`/`activity services` 辅助判断安装信息和 service 运行情况。具体内容、耗时和访问权限取决于固件，并非所有系统信息均开放。来源：[Android bugreport](https://developer.android.com/studio/debug/bug-report)、[Android dumpsys](https://developer.android.com/tools/dumpsys)。

本次没有找到 Hammerhead 面向第三方开发者、公开承诺的统一“Extension 日志文件目录/导出接口”。官方 FIT 导出说明记录的是 `FitFiles`，骑行 FIT 不能替代应用 Logcat。网络上提到的 `crash-logs` 等旧目录不能未经真机确认就当作当前新款 Karoo/Karoo 2 的标准接口。向 Hammerhead 报系统问题时，先通过官方支持入口获得当前固件所需诊断步骤；不要凭猜测上传私人存储全量内容。[Hammerhead FIT 说明](https://support.hammerhead.io/hc/en-us/articles/360005925374-Downloading-a-FIT-file-from-Karoo-to-a-PC)、[官方支持入口](https://support.hammerhead.io/hc/en-us/requests/new)

## 一次可复核的真机排查

1. 记录设备型号、KOS 版本、APK versionName/versionCode、Git commit、安装变体、连接方式以及字段 typeId/布局。
2. `adb devices -l` 确认设备在线；安装 debug APK。先检查扩展是否被发现、字段能否添加。
3. 开始完整 Logcat 采集，在设备按固定步骤复现。另记故障时刻和时区，以及预期/实际结果，避免只留一张错误截图。
4. 分别验证正常输入、传感器断线/无值、暂停/重置、单位改变、所有需要支持的字段尺寸与点击。
5. 退出页面再进入、切换配置、断开重连并观察是否还有旧订阅；需要时用附加调试器检查取消路径。
6. 故障后保存当前缓冲区和必要的 bugreport，再考虑 Force Quit 或重启。每次复现使用不同文件名，避免覆盖上一次证据。
7. 报告中附复现步骤、时刻、采集文件、是否能稳定重现及是否连接调试器；对外分享前检查凭据和位置等敏感内容。

这些是项目操作建议。调试器、日志和 JVM 测试不能替代 Karoo 上的字段发现、真实布局、跨进程渲染和订阅取消 smoke test。

## 本次验证边界

- 已阅读本仓库测试与发布说明，并检查当前 manifest、扩展入口和日志调用。
- 已联网核对上述官方文档与 SDK `1.1.9` 源码；没有以第三方博客或社区用户评论作为设备能力依据。
- 本机执行 `adb devices -l` 后设备列表为空，未连接 Karoo。因此安装、断点附加、各 logcat 选项在 Karoo 的可用性、Wi-Fi ADB 与文件日志均未完成真机验证。
- 本次交付为调研文档，没有新增日志代码或依赖，也没有运行与此文档无关的 Gradle 测试。
