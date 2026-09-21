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
