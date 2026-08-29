# SQuartor

SQuartor 是一个面向中文读者的本地电子书阅读器，使用 Flutter 编写，支持
**EPUB / TXT** 两种格式，并通过 WebDAV 做多设备同步。专注本地体验：自带书架、
阅读统计、翻页/滚动两种阅读流、自定义字体与翻页特效。

> Android 包名 `com.squartor.reader`，桌面/iOS 暂未列入主线测试。

---

## 功能要点

- **导入**：单本 / 多本 / 整目录；支持 Android "用其他应用打开" 调起。
- **阅读**：分页 + 滚动两种模式、章节目录、书签下拉、自定义卷曲翻页（自绘）。
- **样式**：明/暗/跟随系统、Material 3 动态主题、自定义主题色、阅读字体 + 应用字体导入。
- **统计**：每日阅读热力图、按书 / 按日聚合。
- **云同步**：自实现 WebDAV，三级身份匹配（strict / base / title）合并书籍位置、书签、阅读时长。
- **中文友好**：GBK / UTF-8 自适应解码、`lpinyin` 排序、可选淡化日文。

---

## 项目结构

```
lib/
├── main.dart                       # 入口：BookRepository → AppState → SQuartorApp
└── src/
    ├── app.dart, app_state.dart    # 应用壳与中心状态机
    ├── models / models.dart        # BookEntry、ReadingStyle、AppPalette、CloudSyncSettings...
    ├── repository/                 # 仓储：解析、持久化、云同步
    ├── home/                       # 4-tab Shell + 懒加载页栈 + 浮层
    ├── shelf/                      # 书架（网格/列表/系列/选择模式）
    ├── reader/                     # 阅读器（mixin 拆分 + EPUB/TXT 双引擎 + 卷曲）
    ├── settings/                   # 设置主屏 + 详情
    ├── stats/                      # 阅读统计（热力图、单日详情）
    ├── widgets/                    # 通用 widget（按尺寸解码的封面等）
    └── screens/                    # 旧屏幕薄壳（保留兼容入口）
```

详细模块说明见 [`overview.md`](./overview.md) 与 [`docs/`](./docs/)。

---

## 开发环境

| 工具 | 推荐版本 / 路径 |
| --- | --- |
| Flutter SDK | `^3.12.0`（维护机：`D:\antigravity_projects\flutter\bin\flutter.bat`） |
| JDK | 17 |
| Android Gradle Plugin | 8.x（默认）或 9.x（需要应用 pub-cache 补丁，详见下文） |
| Android NDK | 由 Flutter 决定 |
| ABI 默认 | `x86_64`（适配 MuMu 模拟器，见 `android/app/build.gradle.kts`） |

### 一次性配置

```bash
flutter pub get
# AGP 9 用户需要：
bash tooling/apply_pub_cache_patches.sh         # 或 PowerShell：
# pwsh -File tooling\apply_pub_cache_patches.ps1
```

`apply_pub_cache_patches` 会把维护过的 `build.gradle` 复制到本机 pub-cache，
解决以下两个上游兼容问题（详见 [`docs/build.md`](./docs/build.md)）：

- `file_picker-11.0.2`：AGP 9 + `android.builtInKotlin=false` 时跳过 Kotlin 插件。
- `flutter_inappwebview_android-1.1.3`：使用了 AGP 9 已删除的 `proguard-android.txt`。

> 每次 `flutter pub get` / `flutter pub cache repair` 之后都要重跑该脚本。
> 如果不想维护补丁，可以把 AGP 降回 8.x。

---

## 运行

```bash
flutter run                           # 默认设备
flutter run -d emulator-5554          # 选定设备
flutter run --release                 # 真机性能测试
```

MuMu 模拟器（维护机示例）：

```bash
'D:\Program Files\Netease\MuMu\nx_main\adb.exe' connect 127.0.0.1:16384
flutter run -d 127.0.0.1:16384
```

构建 Android APK：

```bash
flutter build apk --debug
flutter build apk --release        # release 包目前未列入 CI，可能要继续调 AGP/KGP
```

---

## 验证

```bash
flutter analyze
flutter test
```

测试覆盖目前集中在云同步合并、EPUB 导入流程与本地解析。新增功能时请相应补测。

---

## 文档索引

- [`overview.md`](./overview.md) — 项目结构与既往优化记录
- [`docs/build.md`](./docs/build.md) — 构建链注意事项与 pub-cache 补丁说明
- [`design/`](./design/) — UI 设计稿草图（HTML 预览）

---

## 已知坑 / TODO

- `lib/src/app_state.dart` 仍然过大，已规划按职责拆分（库 / 进度 / 统计 / 样式 / 同步 / 导入活动）。
- `shelf_screen.dart`、`reader_*_mixin.dart`、`reader_txt_view.dart` 均超过 1500 行，待拆分。
- `lib/src/screens/` 是历史兼容层，迁移完毕后可移除。
- WebDAV 同步暂未做 ETag / 冲突重试，密码以明文存于 `SharedPreferences`。
- 持久化基于 `SharedPreferences`，书籍较多时整 JSON 写盘有 IO 压力，未来需迁移 `sqflite`/`drift`。

---

## License

私有项目，暂不发布到 pub.dev（`pubspec.yaml` 中已设 `publish_to: 'none'`）。
