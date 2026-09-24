# Karoo Extension 发送 HTTP 请求调研

> 调研日期：2026-09-24。结论以当日可访问的 Hammerhead `karoo-ext` 1.1.9
> 官方源码、README、sample，以及 Android 官方文档为准。
>
> 来源等级：核心结论只使用 Hammerhead、Android 一手资料；客户端选型中的 OkHttp
> 能力补充引用其当前维护方的官方仓库。下文明确区分“官方明确说明”“从架构推导”
> 和“项目建议”。尚未在 Karoo 真机发起网络请求。

## 结论

- **官方明确说明**：`karoo-ext` 是供 Android 应用使用的 Android library；
  `KarooExtension` 源码直接继承 Android `Service`。Android 官方允许应用使用
  `HttpsURLConnection` 等平台客户端，并要求网络权限和非主线程执行。
  [Hammerhead README（1.1.9 固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/README.md#L1-L6)、
  [`KarooExtension` 源码](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L41-L70)、
  [Android：连接网络](https://developer.android.com/develop/connectivity/network-ops/connecting)。
- **官方明确说明**：联网有两条可用路径。扩展既可像普通 Android app 一样直接使用
  `HttpsURLConnection` 等客户端，也可通过 SDK 的
  `OnHttpResponse.MakeHttpRequest` 让 Karoo System 发起请求。SDK 路径优先使用
  Wi-Fi，并在设备支持时通过蓝牙交给已连接的 companion app；官方将它限定为与当前骑行
  重要、请求和响应体小于 100 KB 的请求。
  [`OnHttpResponse` 1.1.9 源码](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt#L200-L253)、
  [官方 sample](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/src/main/kotlin/io/hammerhead/sampleext/MainViewModel.kt#L110-L150)。
- **本项目现状**：`AndroidManifest.xml` 已声明 `INTERNET`；
  `UrlConnectionHttpGet` 已用 `java.net.HttpURLConnection` 执行 GET，并通过
  `withContext(Dispatchers.IO)` 离开主线程；请求 URL 由 `QWeatherProvider` 生成为
  `https://...`。`app/build.gradle.kts` 没有 OkHttp/Retrofit/Ktor 依赖，只有
  `karoo-ext` 1.1.9 和 coroutines；这条实现路径在结构上成立。
- **New Karoo 的关键限制**：New Karoo 没有蜂窝网络能力。直接使用
  `HttpsURLConnection` 时，请求只能走 Android 当前可用的默认网络；在设备未连接
  Wi-Fi（包括手机热点）时，应将这条路径视为**不可用**。仅连接 companion app 的蓝牙
  并不会让普通 Android HTTP 客户端自动获得 SDK 的蓝牙代理能力。若希望无 Wi-Fi 时仍
  请求天气 API，应使用 `OnHttpResponse.MakeHttpRequest`，并在目标设备和手机上验证
  官方所说的“if supported”蓝牙转发。
  [Hammerhead 官方产品比较：No cellular capability](https://www.ca.hammerhead.io/pages/karoo2comparison)、
  [`OnHttpResponse` 网络选择说明](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt#L200-L253)。
- **边界**：现有 `QWeatherLiveTest` 是本机 JVM live test。即使通过，也只证明开发机
  到 API 的 DNS/TLS/HTTP 和解析可用，不能证明 Manifest 权限、Karoo 的 KOS 网络栈、
  Wi-Fi/手机连接、休眠、切页、断网重连或 Extension 生命周期行为。

## 实现（2026-09-24）

调研后落地为 `core/http/NetworkAwareHttpGet`：每次请求时读取 Android 默认网络，
若为 Wi-Fi 且具备 `NET_CAPABILITY_INTERNET`，走路径 A（`UrlConnectionHttpGet`）；
否则走路径 B（`core/karoo/KarooHttpGet`，`waitForConnection=true`，10 秒业务
timeout，取消或超时时移除 consumer）。不要求 `VALIDATED`，避免 captive-portal
探测失败时误判。直连失败不会自动回退到 SDK 通道。为读取网络状态已声明
`ACCESS_NETWORK_STATE`。下文“本项目现状”描述的是调研时的代码。

网络路径应理解为：

```text
路径 A：扩展直接联网
Data Field / Extension coroutine
  → HttpGet
  → HttpsURLConnection（或未来的 OkHttp）
  → Android 网络栈
  → Karoo 当前默认网络（New Karoo 实际上需要 Wi-Fi/手机热点）
  → HTTPS API

路径 B：SDK 代理
Data Field / Extension coroutine
  → KarooSystemService.addConsumer(OnHttpResponse.MakeHttpRequest(...))
  → Karoo System 的最佳网络连接
    → Wi-Fi，或受支持时经蓝牙连接的 companion app
  → HTTPS API
```

## 网络请求是否必须经过 Karoo SDK

### 官方明确说明

Hammerhead 将 `karoo-ext` 定义为可加入 Android application 的普通 Android library，
并要求扩展实现继承 `KarooExtension`、在 Manifest 注册为 `<service>`。官方 sample
本身也使用 `com.android.application` 插件，而不是某种独立的 Karoo 应用运行时。
[README](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/README.md#L1-L6)、
[Extension 注册说明](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/README.md#L105-L135)、
[sample `build.gradle.kts`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/build.gradle.kts#L1-L18)。

Android 官方明确提供 `HttpsURLConnection` 平台客户端，支持 TLS、流式上传/下载、
超时、IPv6 和连接池；同时列出 Retrofit（基于 OkHttp）和 Ktor 等较高层客户端。
[Android：Choose an HTTP client](https://developer.android.com/develop/connectivity/network-ops/connecting#choose-http-client)。

### 官方 SDK HTTP 通道

请求**不强制**经过 SDK，但 `karoo-ext` 1.1.9 确实提供专门的 HTTP 通道：

- 以 `KarooSystemService.addConsumer(OnHttpResponse.MakeHttpRequest(...))` 发起；
- 参数包含 method、URL、headers、可选 body，以及默认 `true` 的
  `waitForConnection`；
- 依次可能收到 `Queued`、`InProgress` 和
  `Complete(statusCode, headers, body, error)`；
- `waitForConnection=true` 可在暂时没有连接时排队，调用方仍应设置业务 deadline，
  并在 coroutine 取消时调用 `removeConsumer`；
- 官方注释要求请求/响应体小于 100 KB，并只用于与当前骑行重要的请求。

SDK 路径的独特价值不是提供另一种 HTTP 语法，而是使用 Karoo 的最佳网络：有 Wi-Fi
时走 Wi-Fi，否则在支持的设备/配套应用组合上可通过蓝牙转发。官方 sample 用
`callbackFlow` 包装 consumer、在 `awaitClose` 中移除 consumer，并设置 10 秒 timeout。

来源：
[`OnHttpResponse.MakeHttpRequest`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooEvent.kt#L200-L253)、
[`HttpResponseState`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/models/HttpResponseState.kt#L21-L41)、
[官方 `makeHttpRequest` sample](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/src/main/kotlin/io/hammerhead/sampleext/MainViewModel.kt#L110-L150)。

### 两条路径如何选择

- 只依赖 Karoo 当前 Android 默认网络、传输可能超过 100 KB、需要流式处理，或需要完整
  OkHttp/Retrofit 能力：直接使用 Android 网络客户端。
- 对 New Karoo，直接 Android 客户端在没有 Wi-Fi/手机热点时不可用；普通
  `HttpsURLConnection` 不会自动借用 companion app 的蓝牙连接。
- 与骑行直接相关的小请求，并希望离开 Wi-Fi 后利用受支持的 companion 蓝牙转发：
  使用 SDK HTTP 通道。
- 本项目的实时天气响应很小且服务于骑行 Data Field，优先采用 SDK 通道更符合官方定位；
  可保留现有 `HttpGet` 接口，增加 Karoo-backed 实现并注入 `QWeatherProvider`。
- SDK 文档只说蓝牙转发“if supported”，不能假定所有 Karoo/KOS/手机组合都支持，必须
  真机测试。

## 权限与网络状态

### 官方明确说明

Android 的 `INTERNET` 权限允许应用打开网络 socket；它和
`ACCESS_NETWORK_STATE` 都是 normal permission，安装时授予，不需要运行时弹窗。
Android 的联网指南示例同时声明两者。
[Android `INTERNET`](https://developer.android.com/reference/android/Manifest.permission#INTERNET)、
[Android：连接网络](https://developer.android.com/develop/connectivity/network-ops/connecting)。

`ACCESS_NETWORK_STATE` 用于读取网络信息；若要监听默认网络或检查
`NET_CAPABILITY_VALIDATED`，应使用 `ConnectivityManager`/`NetworkCallback`。
`NET_CAPABILITY_INTERNET` 只表示网络配置为可访问互联网，不保证能到达公网；
`VALIDATED` 也只是系统当前探测到的最近似信号，网络仍可能随时变化。
[Android：读取网络状态](https://developer.android.com/develop/connectivity/network-ops/reading-network-state)。

### 本项目现状与建议

- 当前 Manifest 已有 `android.permission.INTERNET`，普通 GET 不需要 Karoo 专用权限，
  也不需要运行时申请。
- SDK 请求由 Karoo System 执行；官方 HTTP sample 的 Manifest 没有声明 `INTERNET`，
  这与请求不由扩展进程直接打开 socket 一致。不过官方 API 文档没有把“SDK 请求无需
  `INTERNET`”写成独立权限契约；若项目同时保留直接客户端，仍应保留当前权限。
- 当前没有 `ACCESS_NETWORK_STATE`。现有代码直接请求并处理异常，不查询连接状态，
  因而不应仅为“可能以后使用”增加权限；若后续真正注册 `NetworkCallback` 或展示离线
  状态，再同步声明它。
- 即使预检查显示 `VALIDATED`，请求本身仍必须有 timeout、异常处理和重试/缓存策略；
  不能用一次网络状态检查替代真实请求结果。

## 线程与协程

### 官方明确说明

Android 禁止在主线程执行网络操作；面向 Honeycomb 及以上的应用在主线程联网会抛
`NetworkOnMainThreadException`。Android 的协程最佳实践进一步要求执行阻塞操作的类
自行通过 `withContext` 移出主线程，并直接以阻塞 `HttpURLConnection` 作为
`IO dispatcher` 的示例。Android 同时建议注入 dispatcher，以便测试。
[Android：连接网络](https://developer.android.com/develop/connectivity/network-ops/connecting)、
[`NetworkOnMainThreadException`](https://developer.android.com/reference/android/os/NetworkOnMainThreadException)、
[协程最佳实践：main-safe](https://developer.android.com/kotlin/coroutines/coroutines-best-practices#main-safe)、
[协程最佳实践：注入 dispatcher](https://developer.android.com/kotlin/coroutines/coroutines-best-practices#inject-dispatchers)。

Android `Service` 自身也不会自动创建工作线程；Service callback 默认运行在宿主进程的
主线程，阻塞工作必须移到其他线程。
[Android Services overview](https://developer.android.com/develop/background-work/services)。

SDK `MakeHttpRequest` 是异步 consumer API；官方 sample 在 `viewModelScope` 中把状态
回调桥接成 `callbackFlow`。注册 consumer 不需要放入 `Dispatchers.IO`，但响应解析若
可能耗时，仍不应阻塞 callback/main thread。
[官方 SDK HTTP sample](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/src/main/kotlin/io/hammerhead/sampleext/MainViewModel.kt#L110-L150)。

### 本项目现状与建议

`UrlConnectionHttpGet.get()` 是 `suspend` 函数，并把整个阻塞请求放入
`withContext(Dispatchers.IO)`；这满足“不能在主线程做阻塞网络 I/O”的核心要求。
它还设置 10 秒 connect/read timeout，并在 `finally` 中 `disconnect()`。
Android 的 `HttpURLConnection` 文档也要求读完 response 后断开，并建议大响应流式处理，
而不是一次性全部放入内存。
[Android `HttpURLConnection`](https://developer.android.com/reference/java/net/HttpURLConnection)。

后续演进建议：

- 保留 `HttpGet` 小接口；如需精确测试调度，可把 IO dispatcher 注入
  `UrlConnectionHttpGet`，但这不是当前发请求的前置条件。
- `readBytes()` 会把完整响应读入内存。天气 JSON 较小时可以接受；若响应规模不可控，
  应限制 body 大小或流式解析。
- `HttpURLConnection` 是阻塞调用；取消 coroutine 不等于底层 socket 一定即时中止。
  当前 10 秒 timeout 提供了上限，但 Data Field 停止或 Extension 销毁时仍应取消拥有
  该请求/刷新循环的 job。
- 当前 `instanceFollowRedirects = false`，3xx 会原样返回并由上层当作失败；这应当是
  有意识的安全/产品选择，而不是误以为会自动跟随。

## HTTPS 与明文 HTTP 限制

### 官方明确说明

Android 9（API 28）起，面向 API 28 及以上的应用默认禁用明文流量；
`android:usesCleartextTraffic` 对 target API 28+ 默认是 `false`。如确需例外，应使用
Network Security Configuration 做尽可能窄的域名配置。Android 官方同时明确建议所有
应用流量使用 SSL/TLS；允许明文会带来窃听和篡改风险。
[Android Network Security Configuration：Cleartext traffic](https://developer.android.com/privacy-and-security/security-config#CleartextTraffic)、
[`usesCleartextTraffic`](https://developer.android.com/guide/topics/manifest/application-element#usesCleartextTraffic)、
[Android 网络安全建议](https://developer.android.com/develop/connectivity/network-ops/connecting#design-secure)。

### 本项目现状与建议

- 本项目 `targetSdk = 34`，Manifest 没有明文 opt-in 或自定义 network security config，
  因此平台默认拒绝明文 HTTP。
- `QWeatherProvider` 构造的是 `https://$apiHost/...`，`QWeatherConfig` 还主动拒绝
  `http://` host，与当前策略一致。
- 不要为了调试方便全局设置 `usesCleartextTraffic="true"`。若未来必须连接局域网 HTTP
  设备，应先确认真实需求，再做仅限目标域名、仅限 debug 或其他最小范围配置，并在实际
  Karoo/KOS 上验证。
- Hammerhead 没有说明 SDK 代理对明文 URL 应用扩展、Karoo System 还是 Companion 的
  哪套安全策略；不能把 SDK 路径当作绕过明文限制的办法。

## Extension service 与后台生命周期

### 官方明确说明

`KarooExtension` 是 Android `Service`，官方源码只通过 `onBind()` 暴露 binder；
SDK 在 Karoo 停止 scan、device connection、stream 或 view 时会移除对应 emitter 并调用
`cancel()`。`Emitter.setCancellable` 就是扩展挂接清理动作的契约。
[`KarooExtension` service/binder](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L46-L88)、
[`startStream`/`stopStream` 与 `startView`/`stopView`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt#L102-L129)、
[`Emitter.setCancellable`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/lib/src/main/kotlin/io/hammerhead/karooext/internal/Emitter.kt#L50-L104)。

Android 对纯 bound service 的规则是：只在有客户端绑定时存活；最后一个客户端解绑后，
系统销毁 service。后台 service 启动限制不直接限制 bound service，但这不等于 bound
service 可以脱离客户端无限常驻。Service 进程仍受内存压力和系统进程管理影响。
[Android：Bound services lifecycle](https://developer.android.com/develop/background-work/services/bound-services#Lifecycle)、
[Android 8 后台限制（bound service 例外）](https://developer.android.com/about/versions/oreo/background#services)、
[Android `Service` 进程生命周期](https://developer.android.com/reference/android/app/Service#ProcessLifecycle)。

官方 sample 的 Data Type 在 `startStream()` 创建 coroutine job，并在
`emitter.setCancellable` 中取消 job，给出了与 SDK stream 生命周期绑定的直接范例。
[官方 `CustomSpeedDataType`](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/src/main/kotlin/io/hammerhead/sampleext/extension/CustomSpeedDataType.kt#L45-L68)。

SDK HTTP sample 也在 `awaitClose` 中移除 consumer，并给收集设置 10 秒 timeout。
代理请求若接入 Data Field，应在 `emitter.setCancellable` 对应的 job/Flow 关闭路径中
执行 `removeConsumer(listenerId)`，避免字段移除后遗留监听。
[官方 `makeHttpRequest` sample](https://github.com/hammerheadnav/karoo-ext/blob/26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed/app/src/main/kotlin/io/hammerhead/sampleext/MainViewModel.kt#L110-L150)。

### 从架构推导

- 只为正在显示/消费的天气字段刷新数据时，请求 job 应归属于该字段 stream/view，并由
  emitter cancellable 停止；不能只因为进程还活着就继续轮询。
- 扩展级共享请求或 scope 至少要在 `onDestroy()` 取消；`onDestroy()` 是正常销毁时的
  清理点，不是“请求一定完成”或“进程永不被杀”的保证。
- 若需求是“没有 Karoo 客户端绑定、字段未显示时也要长期或定时同步”，它已经不是
  `KarooExtension` bound service 的自然生命周期。应先评估 Android 的调度机制，而不是
  试图通过 Karoo SDK 维持 HTTP 请求；是否需要 WorkManager、前台服务或不做后台刷新，
  取决于用户可见性、及时性与 KOS 真机行为。

### 本项目现状

`MyKarooExtension.onCreate()` 连接 `KarooSystemService`，`onDestroy()` 断开；当前没有
Extension 级 coroutine scope。天气 provider/HTTP 实现目前只被测试引用，尚未接入任何
Data Field stream，因此仓库里还没有需要审查的“天气刷新 job 属于谁、何时取消”的完整
运行链路。

## 客户端选择：HttpURLConnection 还是 OkHttp

### 官方明确说明

Android 自带 `HttpsURLConnection`，支持本项目所需的 TLS、timeout、IPv6、streaming
和 connection pooling；第三方高层库主要增加声明式 API、序列化等便利能力。
[Android：Choose an HTTP client](https://developer.android.com/develop/connectivity/network-ops/connecting#choose-http-client)。

OkHttp 官方说明其提供 HTTP/2、连接池、透明 gzip、响应缓存、常见网络问题恢复，以及
同步阻塞和异步 callback 两种调用方式。
[OkHttp 官方 README](https://github.com/lysine-dev/okhttp/blob/main/README.md)。

### 项目建议

若选择直接 Android 网络路径，当前继续使用 `HttpURLConnection` 是合理的，理由是：

- 目前只有一个低频 GET、少量 header 和简单 JSON；
- 已有 timeout、错误映射、资源关闭及独立 `HttpGet` 接口；
- 无需为“运行在 Karoo”专门改用 OkHttp；两者都走 Android 网络栈；
- 不增加 APK 依赖和额外客户端配置。

出现以下真实需求时再考虑 OkHttp：

- 多个 endpoint、统一 interceptor/auth/header；
- 更明确的 call cancellation、重试/连接恢复、缓存策略；
- MockWebServer 等更完整的 HTTP 集成测试；
- Retrofit 声明式 API 或成熟序列化栈能显著减少重复代码。

选择 OkHttp 后，阻塞 `execute()` 仍需放在 IO dispatcher；只有使用异步 API并正确桥接
coroutine cancellation 时，才不能简单照搬当前 `withContext` 结构。不要同时长期维护
两套客户端。

对当前天气字段而言，客户端库选择是第二层问题：先根据是否需要 companion 蓝牙转发决定
使用 SDK HTTP 通道还是 Android 网络栈；只有选择后者时，才需在
`HttpURLConnection`、OkHttp 或 Retrofit 之间选择。

## Karoo 真机验证边界

### 官方资料能证明什么

官方仓库能证明 Extension 是 Android app/service、SDK 的 binder 与 emitter 生命周期，
Android 文档能证明权限、线程、TLS/明文和 Service 的平台契约。它们不能替代目标设备、
目标 KOS 版本和真实网络环境。

### 必须在 Karoo 真机验证

1. 安装 APK 后扩展可发现，天气字段开始订阅时能完成 HTTPS DNS、TLS 握手和 GET。
2. 分别测试 Wi-Fi 下的直接 Android 请求与 SDK 请求；关闭 Wi-Fi 后验证目标设备/KOS/
   companion app 是否真的支持 SDK 蓝牙转发。切换网络、断网、弱网、DNS 失败、API
   timeout、401/403、429、5xx 时字段应进入明确的 unavailable/stale 状态，不阻塞 UI。
3. 验证 target 34 下 `https://` 正常；如以后引入任何 `http://`，确认它按预期被阻止，
   或最小范围 Network Security Configuration 确实生效。
4. 字段添加/移除、页面切换、开始/暂停/结束骑行、扩展 Force Quit、Karoo OS 解绑/重绑
   后，请求和刷新 job 不重复、不泄漏、不继续无意义轮询。
5. 请求进行中移除字段或销毁 Extension，确认取消行为和最长 10 秒 timeout 可接受。
6. 对 SDK 路径分别验证 `waitForConnection=true/false`、请求/响应接近或超过 100 KB、
   `Complete.error`，并确认取消后 consumer 已移除。
7. 长时间骑行中观察请求频率、缓存、数据年龄、耗电、流量和设备温度；不要把开发机
   JVM 测试的成功率外推到设备。
8. 若 API host 使用新的证书链、IPv6/CDN 或地区性网络，必须在实际使用地区和网络下
   验证。Android 的 `VALIDATED` 状态不保证特定 API 一定可达。

## 不确定项

- Hammerhead 官方提供 SDK HTTP 通道及 100 KB/骑行相关性约束，但没有给出蓝牙转发支持
  矩阵、配额、后台白名单或网络 SLA；因此不能声称 Karoo OS 会保证 Extension 始终联网。
- Hammerhead 没有公开 SDK 代理的 DNS、TLS、证书库、重定向、压缩、内部 timeout、
  自动重试、明文策略，或 `waitForConnection=true` 的排队时限和跨进程重启行为。
- Android 架构允许直接网络调用，但没有 Hammerhead 文档逐字确认“所有 KOS 版本上的
  所有 Android 网络 API 均与普通手机完全一致”。设备系统镜像、
  TLS provider、证书库、网络切换和省电策略仍需真机验证。
- 没有真机证据说明 Extension service 在骑行页、页面切换、锁屏/休眠、Force Quit 和
  KOS 更新后的精确绑定时序；实现应依赖 emitter/service 的取消契约，而不是猜测固定时序。
- 当前 live test 可使用真实 QWeather 凭据从开发机联网，但未执行 Karoo instrumentation
  或设备 smoke test；测试通过与否都不能关闭上述验证项。
