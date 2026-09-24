# Karoo Extension 发布、安装与私人分发调研

> 调研日期：2026-09-24。结论以当日可访问的 Hammerhead、Android 和 GitHub
> 官方资料为准。
>
> 来源等级：本文只使用 Hammerhead/SRAM、Android、GitHub 的一手资料和本仓库现状，
> 未使用二手来源。下文每组“来源”的访问日期均为 **2026-09-24**。

## 结论

本项目若暂时只供作者自己使用，不需要先向 Hammerhead 申请或审批。推荐分两条路径：

1. 开发期优先使用 USB/ADB 安装 debug APK，不需要托管 APK。Hammerhead 明确记录了
   Karoo 与 Karoo 2 的 USB Debugging、数据线和 USB 配置步骤；具体 APK 安装命令来自
   Android 官方 ADB 文档及 Hammerhead SDK 样例，仍应在作者设备上 smoke test。
2. 日常稳定使用时，将固定文件名的、使用自有密钥签名的 release APK 放到本项目的
   GitHub Releases；新款 Karoo 可通过手机 Companion App 分享 APK 文件或 GitHub
   下载 URL 安装。Karoo 2 可使用 Dashboard 开发者私测的 Extension Library；若真机
   确认 ADB 可用，也可继续直接安装。

当前仓库
[`jxguo92/my-karoo-extension`](https://github.com/jxguo92/my-karoo-extension)
是公开仓库且截至调研日[尚无 Release](https://github.com/jxguo92/my-karoo-extension/releases)，
因此可以直接在现有仓库启用 Releases，无需另建下载站（访问日期：2026-09-24）。

只有希望让所有 Karoo 用户从官方 Extension Library 直接安装时，才需要走 Hammerhead
Dashboard 自助提交、SDK 许可协议和 SRAM 审批。个人侧载与官方上架是两回事。

## “注册 Extension”具体指什么

### 1. APK 内向 Karoo OS 声明 Extension：必须

官方 `karoo-ext` README 要求 APK 在 `AndroidManifest.xml` 中声明一个带
`io.hammerhead.karooext.KAROO_EXTENSION` intent filter 的 service，并通过
`io.hammerhead.karooext.EXTENSION_INFO` 指向 `extension_info.xml`。这使 Karoo OS
能够在本机发现 Extension，并不是联网备案或服务端审批。

`KarooExtension` 构造函数中的 Extension ID 必须与 `extension_info.xml` 的 `id`
一致。截至调研日，官方本地发现流程只要求 APK 内 service、intent filter 和
`extension_info.xml`，Dashboard 流程登记的则是远程 application manifest URL；没有公开
要求个人侧载前先在 Hammerhead 服务端预留或注册 Extension ID。这里的结论是“官方没有
公布该要求”，不扩张为对未公开服务端实现的断言。

本项目已经具备这层本地注册：

- application ID：`com.jxguo92.mykarooextension`
- Extension ID：`my-karoo-extension`
- Extension service：`.extension.MyKarooExtension`

来源（访问日期：2026-09-24）：

- [Hammerhead `karoo-ext` README（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/README.md)
- [官方 `KarooExtension` 源码（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/extension/KarooExtension.kt)

### 2. 只在本人账号设备测试：可选的 Dashboard 自助登记

Hammerhead 在 2026 年上线了 Extension 自助流程。开发者可以：

1. 创建 Hammerhead Dashboard 开发者账号；
2. 启用 Extension developer settings；
3. 填写联系信息；
4. 添加一个有效的 application manifest URL。

加入 manifest 后，应用会自动出现在所有登录该开发者账号的 Karoo 设备的 Extension
Library 中。该流程适合多台自有设备测试，但并非 ADB 或 Companion App 侧载的前置条件。

来源（访问日期：2026-09-24）：[Extensions: Self-Service for Developers](https://support.hammerhead.io/hc/en-us/articles/53739708787099-Extensions-Self-Service-for-Developers)

### 3. 面向所有用户进入官方 Extension Library：需要申请

面向全部 Karoo 用户发布时，需要在 Dashboard 提交应用供 SRAM 审批，并签署 SDK
许可协议。官方明确提示签约和审批可能各耗时数周。获批后 Karoo 根据 application
manifest 拉取最新 APK；开发者更新 manifest 后，用户会在 Extension Library 收到更新
提示。

因此，本项目当前没有必要走公开上架流程；功能和真机行为稳定后再申请即可。

来源（访问日期：2026-09-24）：

- [Extensions: Self-Service for Developers](https://support.hammerhead.io/hc/en-us/articles/53739708787099-Extensions-Self-Service-for-Developers)
- [Karoo OS - Extensions Library](https://support.hammerhead.io/hc/en-us/articles/34676015530907-Karoo-OS-Extensions-Library)

## APK 安装方式

### 开发期：USB/ADB

Hammerhead 的 FIT 文件导出文档明确适用于“Karoo or Karoo 2”，要求开启 `USB Debugging`、
使用 USB-C **数据线**，并在 `Select USB configuration` 中选择 MTP。这确认两代设备至少
公开支持 USB debugging 和 USB 数据链路。Hammerhead SDK 样例给出
`./gradlew app:installDebug`；具体 `adb install`、`-r` 与卸载语义则来自 Android 官方
文档。

1. 在 Karoo 打开 `Settings → About`，连续点击 `Build Number` 7 次，再返回并开启
   `Developer Options`。
2. 开启 `USB Debugging`，使用支持数据传输的 USB-C 线连接电脑。若电脑无法识别设备，
   按 Hammerhead 文档在 `Select USB configuration` 中重新选择 MTP。
3. 运行 `adb devices`；若设备弹出 RSA 授权框，解锁设备并确认。
4. 构建并安装：

   ```bash
   ./gradlew :app:installDebug
   ```

   或先构建再安装：

   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

`-r` 表示保留应用数据并替换已安装版本。官方 SDK 示例使用 Gradle
`app:installDebug`，Android 官方文档也确认 `adb install path_to_apk` 是标准安装方式。
卸载可使用 `adb uninstall com.jxguo92.mykarooextension`；Android 官方文档说明
`uninstall` 会移除 package，`-k` 才会保留 data/cache。

来源（访问日期：2026-09-24）：

- [Karoo 2 - Enabling Developer Options](https://support.hammerhead.io/hc/en-us/articles/360058885393--Karoo-2-Enabling-Developer-Options)
- [Karoo - Enabling Developer Options](https://support.hammerhead.io/hc/en-us/articles/30696553134363-Karoo-Enabling-Developer-Options)
- [Downloading a FIT file from Karoo to a PC（明确 Karoo/Karoo 2、USB Debugging 与数据线）](https://support.hammerhead.io/hc/en-us/articles/360005925374-Downloading-a-FIT-file-from-Karoo-to-a-PC)
- [Hammerhead `karoo-ext` README（含 `app:installDebug`，固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/README.md)
- [Android Debug Bridge](https://developer.android.com/tools/adb)

### 日常安装：Companion App，仅新款 Karoo

Hammerhead 官方支持在手机上把以下任一内容分享到 Karoo Companion App：

- 手机本地 APK 文件；
- 直接指向 APK 的 URL。

传输完成后，Karoo 会显示安装确认页。官方说明：

- 该功能只适用于最新一代 Karoo，不适用于 Karoo 2；
- Karoo 需要 KOS `1.538.2049` 或更高；
- Android Companion App 需要 `1.36.0` 或更高，iPhone 版需要 `1.12.0` 或更高；
- Hammerhead 目前只验证了 GitHub URL；
- 通过 GitHub URL 安装的应用可以在 Karoo 应用详情页获得后续更新。

来源（访问日期：2026-09-24）：[Companion App - Sideloading](https://support.hammerhead.io/hc/en-us/articles/31576497036827-Companion-App-Sideloading)

### 官方 Extension Library

通过审批或开发者账号私测登记后，可以在 Karoo 的
`Apps → Extensions → 应用详情 → Install` 安装，也可在同一位置卸载或 Force Quit。

官方 FAQ 说明当前 `karoo-ext` 同时支持 Karoo 2 和新款 Karoo；Extension Library 文档则
明确写有面向两者的原生 Extension。与 Companion 侧载“仅新款 Karoo”不同，Library 是
Karoo 2 的官方安装入口之一。

来源（访问日期：2026-09-24）：

- [Karoo OS - Extensions Library](https://support.hammerhead.io/hc/en-us/articles/34676015530907-Karoo-OS-Extensions-Library)
- [FAQ: Hammerhead Extensions](https://support.hammerhead.io/hc/en-us/articles/31150180125083-FAQ-Hammerhead-Extensions)

## APK 放在哪里最方便

### 推荐：当前公开仓库的 GitHub Releases

这是本项目成本最低、与官方示例最一致的选择：

- APK 是直接下载 URL，适合 Companion App；
- 每个版本有 tag、发布说明和历史产物；建议另外上传 SHA-256 校验文件；
- 可以使用稳定地址
  `https://github.com/jxguo92/my-karoo-extension/releases/latest/download/my-karoo-extension.apk`；
- Hammerhead 官方 sample 本身也把 APK 和 `manifest.json` 放在 GitHub Releases，并使用
  `/releases/latest/download/...` URL。

每个 Release 都应上传相同文件名的 `my-karoo-extension.apk`，不要把版本号写进资产
文件名；版本由 tag、`versionName` 和 `versionCode` 表达。

官方示例与 GitHub 文档（访问日期：2026-09-24）：

- [sample application manifest（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/manifest.json)
- [sample release workflow（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/.github/workflows/tag-and-release.yml)
- [GitHub：About releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)

### 开发期最私密：不托管

若 APK 不希望公开，最简单可靠的方式不是找私有下载站，而是：

- 新款 Karoo：将电脑生成的 APK 放到自己的手机，再把本地文件分享到 Companion App；
- Karoo 2：若真机确认 ADB 可用，直接 `adb install -r`；否则使用 Dashboard 开发者私测
  manifest，让应用只出现在登录作者开发者账号的 Karoo 上。

“本地 APK → Companion”和“ADB”不需要公网 APK URL。Dashboard 私测则仍需要 Karoo
可访问的 manifest/APK URL；私密的是 Extension Library 的可见范围，并不自动保证底层
下载 URL 私有。

### 私有 GitHub Releases：适合归档，不适合设备自动拉取

私有仓库的 Release 很适合保存版本、签名 APK 和发布说明，但只对有仓库读取权限的用户
可见。官方 `KarooAppManifest` 只定义 URL，没有 token/header 字段；Hammerhead 也没有
文档说明 Karoo 或 Companion 能保存 GitHub 私有仓库凭据。因此：

- 可以登录 GitHub 手工下载私有 Release APK，再把**本地文件**分享给新款 Karoo 或经
  ADB 安装；
- 不应把私有 Release URL 直接作为无人值守的 `MANIFEST_URL`/`latestApkUrl`；
- 不要把 GitHub token 塞进 URL。

这里“不能稳定自动拉取”是由 GitHub 私有可见性与 Karoo manifest 无认证字段共同得出的
保守推论，不是 Hammerhead 对私有 GitHub Releases 的专项声明。

来源（访问日期：2026-09-24）：

- [GitHub：About repositories（private repository 可见性）](https://docs.github.com/en/repositories/creating-and-managing-repositories/about-repositories)
- [Hammerhead `KarooAppManifest`（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooAppManifest.kt)

### GitHub Actions artifacts：适合 CI 中间产物，不适合发布

Actions artifacts 面向 CI 中间产物，不适合作为设备更新源：

- 默认保留 90 天，之后会删除；
- 下载者必须登录 GitHub 且拥有仓库读取权限；
- 它依附 workflow run，而不是稳定的 `/releases/latest/download/...` 发布地址。

来源（访问日期：2026-09-24）：

- [Store and share data with workflow artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data)
- [Downloading workflow artifacts](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)

### 云盘或自建静态下载

普通云盘常返回登录页、预览页或短期重定向 URL。Hammerhead 明确表示目前只验证 GitHub
URL，并不负责排查其他站点的 APK 链接问题。若必须自建托管，应使用无需认证、稳定 HTTPS
直链且返回 APK 正文的静态地址；带过期时间的签名 URL 会让 manifest 更新链路失效。
自建静态站可将源码保持私有，但 APK URL 本身若无需认证，拿到链接的人仍能下载。

公开 Release 意味着任何人都能下载 APK。当前源码仓库已经公开，因此这不是额外的源码
保密损失；但 APK 中仍不得包含 token、账号密码或其他秘密。

来源（访问日期：2026-09-24）：[Companion App - Sideloading](https://support.hammerhead.io/hc/en-us/articles/31576497036827-Companion-App-Sideloading)

### 综合选择

- **公开 GitHub Release**：设备安装和版本追踪最方便，且 Hammerhead 已验证 GitHub
  URL；代价是任何人都可下载 APK。
- **私有 GitHub Release**：访问控制和版本归档好，但必须先人工下载，不能作为已获官方
  确认的 Karoo 自动更新源。
- **Actions artifact**：适合 CI 验证和短期取件；90 天默认过期与登录要求使其不适合
  长期安装地址。
- **云盘/自建静态站**：可独立于源码仓库，但需要自己保证稳定 HTTPS 直链、长期可用性
  和访问控制；Hammerhead 未承诺兼容。
- **本地文件/ADB**：公网暴露最少，也最适合单人开发；没有远程更新提示，版本历史应由
  Git tag、release notes 和 SHA-256 文件另行维护。

## 更新、签名和 application manifest

### Android 更新约束

后续 APK 要覆盖安装，必须保持：

- 相同 `applicationId`；
- 相同签名证书；
- 不降低 `versionCode`；本项目发布时应让每个 Release 的 `versionCode` 严格递增。

Android 使用签名证书确认更新来自同一发布者。debug APK 使用本机 debug keystore；换电脑
或丢失该 keystore 后，新 APK 不能覆盖旧安装。因此长期日常使用应创建专用 release
签名密钥，并把 keystore、密码放在本地安全存储或 CI secrets，绝不提交到 Git。

来源（访问日期：2026-09-24）：

- [Android：Version your app](https://developer.android.com/studio/publish/versioning)
- [Android：Sign your app](https://developer.android.com/studio/publish/app-signing)

### Karoo application manifest

`application manifest` 与 `extension_info.xml` 不是同一个文件：

- `extension_info.xml` 描述 Data Field、Bonus Action 等 Karoo 能力，打包在 APK 内；
- 远程 JSON application manifest 描述 APK 下载地址、版本和商店展示信息。

官方 `KarooAppManifest` 至少包含：

- `label`
- `packageName`
- `latestApkUrl`
- `latestVersion`
- `latestVersionCode`

还可包含 `iconUrl`、`developer`、`description`、`releaseNotes`、`screenshotUrls` 和
`tags`。APK 的 `<application>` 应添加：

```xml
<meta-data
    android:name="io.hammerhead.karooext.MANIFEST_URL"
    android:value="https://github.com/jxguo92/my-karoo-extension/releases/latest/download/manifest.json" />
```

建议每个 GitHub Release 同时上传固定文件名的 `manifest.json` 和
`my-karoo-extension.apk`。JSON 中的 `latestApkUrl` 指向该 Release 的稳定 latest
下载地址，版本字段与 APK 一致。

来源（访问日期：2026-09-24）：

- [官方 `KarooAppManifest` 源码（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/models/KarooAppManifest.kt)
- [官方 Constants 源码（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/lib/src/main/kotlin/io/hammerhead/karooext/Constants.kt)
- [官方 sample manifest（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/manifest.json)
- [官方 sample AndroidManifest（固定 commit）](https://github.com/hammerheadnav/karoo-ext/blob/f79f103cd0e1a9808db64474796b30d822cc0b17/app/src/main/AndroidManifest.xml)

## 对本项目的建议落地顺序

### 立即开发自测（最少步骤）

1. 在作者实际设备上确认
   `Developer Options → USB debugging → adb devices → ./gradlew :app:installDebug`；
   可用时无需托管 APK。
2. 若是新款 Karoo 且不想使用 ADB，也可运行 `./gradlew :app:assembleDebug`，把
   `app-debug.apk` 传到手机后作为本地文件分享给 Companion App，并在 Karoo 安装页确认。

### 首次长期自用发布（一次性设置）

1. 创建并安全备份专用 release keystore；把密码放在本地安全存储或 GitHub Actions
   secrets，不提交密钥。
2. 在项目中配置 release signing，并添加与官方 `KarooAppManifest` 契约一致的
   `manifest.json` 和 `MANIFEST_URL`。
3. 在当前公开仓库创建首个 GitHub Release，固定上传
   `my-karoo-extension.apk`、`manifest.json` 和可选的 SHA-256 文件。
4. 新款 Karoo 分享 Release APK URL 到 Companion；Karoo 2 优先在 Dashboard 添加
   manifest 做开发者账号私测，已验证 ADB 可用时也可直接安装。

### 每次更新（最少步骤）

1. 递增 `versionCode`，更新 `versionName` 和 manifest 中对应版本。
2. 运行 `./gradlew lint test assembleRelease`，用同一 release key 签名。
3. 创建 tag/Release，并用固定资产名上传新 APK、manifest 与校验文件。
4. 在 Karoo 检查更新提示；若使用 ADB，则执行
   `adb install -r app/build/outputs/apk/release/app-release.apk`。

上述流程稳定后再加入 release workflow 自动化；功能成熟且确有其他用户需求时，再在
Hammerhead Dashboard 提交公开上架。

当前仓库尚未配置 release signing、远程 application manifest、release workflow 或
GitHub Release；这些都是后续实现项。在真机确认 ADB 链路后，它们不影响当前安装 debug
APK。

## 仍需真机确认

以下细节未在官方资料中得到足够明确的跨设备保证，应在作者自己的设备上 smoke test：

- 本项目无 launcher Activity 时，Companion App URL 侧载后是否立即在 Extensions 和
  Data Field 选择器中刷新，还是需要重启 Extensions/Ride app 或设备；
- Karoo 2 与新款 Karoo 在作者当前 KOS、电脑和线材组合下，`adb devices`、
  `adb install -r` 与 `adb uninstall` 是否正常；官方已确认两代设备的 USB Debugging
  与数据线流程，但没有提供完整的 Karoo APK ADB 命令教程；
- 新款 Karoo 从 GitHub stable URL 安装后，自动更新提示的触发时机；
- Dashboard 开发者私测 manifest 在 Karoo 2 上的实际展示与安装行为；
- 直接从设备端文件管理器打开 APK 安装是否可用；Hammerhead 官方只记录了 Companion
  弹出的 Install 页面，没有记录通用设备端文件安装器流程；
- release APK 经 R8/资源收缩后，Extension service、`extension_info.xml` 和 Glance
  RemoteViews 是否完整保留。

无论采用哪种分发方式，正式发布前仍应在 Karoo 真机验证：扩展可发现、字段可添加、实时
值与 `NotAvailable` 正常、覆盖更新保留设置、卸载/重装行为符合预期。
