# My Karoo Extension

面向 Hammerhead Karoo 的自定义扩展项目。当前包含第一个 Data Field：显示当前后飞轮片齿数。
系统提供实时齿数时直接显示；否则按 SRAM CS-XG-1371-E1 的档位序数推测，并在
推测值末尾追加 `'`，例如 `21'`。

Android 的包名不能包含连字符，因此 namespace/applicationId 使用 `com.jxguo92.mykarooextension`；Karoo Extension ID 使用 `my-karoo-extension`。

官方 SDK 仓库：[hammerheadnav/karoo-ext](https://github.com/hammerheadnav/karoo-ext)。

## 环境要求

- Android Studio 或 JDK 17+
- Android SDK Platform 37
- GitHub Packages 只读凭据（用于下载 `io.hammerhead:karoo-ext`）

在用户级 `~/.gradle/gradle.properties` 中配置凭据，不要提交到仓库：

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Token 需要 `read:packages` 权限。CI 也可使用 `GITHUB_ACTOR`/`GITHUB_TOKEN`，或 `USERNAME`/`TOKEN` 环境变量。

和风天气实况还需要 API Host 与 API KEY（控制台 → 设置可查看 API Host，另需创建 API KEY 凭据）。在仓库根目录的 `local.properties`（已在 `.gitignore` 中）追加：

```properties
qweather.apiHost=your-id.qweatherapi.com
qweather.apiKey=your-api-key
```

CI 可改用环境变量 `QWEATHER_API_HOST` 与 `QWEATHER_API_KEY`。未配置时工程仍可编译，`QWeatherConfig.fromBuildConfig()` 返回 `null`。这两个值会写入所有构建类型（含 release）APK 的 `BuildConfig`，可被反编译取出；不要把 `local.properties` 提交到仓库。

## 构建

Windows：

```powershell
.\gradlew.bat assembleDebug
```

完整验证：

```powershell
.\gradlew.bat lint test assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## Data Field

字段契约、映射和真机检查项见 [Data Fields](docs/data-fields.md)。新增 Data Field 时，
按 [AGENTS.md](AGENTS.md) 中的垂直切片约定实现，并同步 `extension_info.xml`、Kotlin
registry、契约测试和字段文档。完整结构决策见
[Karoo Extension 项目结构调研](docs/research/karoo-extension-project-structure.md)。
