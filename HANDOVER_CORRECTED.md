# 项目交接文档（已根据代码实际情况修正）

> 本文档在原交接文档基础上，逐项比对了 `D:\antigravity_projects\squartor` 仓库的实际代码并做了修正。
> 凡原文档与实际代码不符之处，均以 **【修正】** 标注并说明真实情况。
> 修正日期：2026-06-24

---

## 1. 项目基本信息

**项目名称：** Squartor

**项目目标：** 打造一款拥有极致丝滑物理仿真翻页效果、极简夜间沉浸式设计的跨平台小说/电子书阅读器。

**要解决的核心问题：** 解决传统阅读器在翻页动画上的视觉瑕疵（3D 网格形变导致字体发糊、边缘锯齿、切割闪烁、白屏延迟），提供像素级锐利的完美翻页视觉体验。

**技术栈：** Flutter (SDK `^3.12.0`) / Dart

**运行环境：** Android / iOS（当前主要针对 Android 真机与模拟器测试）

**主要依赖（以 `pubspec.yaml` 为准）：**

| 类别 | 依赖 | 版本 | 说明 |
|------|------|------|------|
| 存储/路径 | `shared_preferences` | `^2.5.5` | KV 存储，承载书架/设置/统计等全部持久化 |
| 存储/路径 | `path_provider` | `^2.1.5` | 文件路径 |
| 存储/路径 | `path` | `^1.9.1` | 路径拼接 |
| 文件选择 | `file_picker` | `^11.0.2` | 选书/选字体/选文件夹 |
| 主题 | `dynamic_color` | `^1.8.1` | Material 3 取色（壁纸主题） |
| 外链 | `url_launcher` | `^6.3.2` | 打开外链 |
| 解析/核心 | `archive` | `^4.0.9` | ZIP/EPUB 解压 |
| 解析/核心 | `xml` | `^7.0.1` | DOM 解析 |
| 解析/核心 | `html` | `^0.15.6` | HTML 解析 |
| 解析/核心 | `gbk_codec` | `^0.4.0` | 中文 GBK 乱码处理 |
| 解析/核心 | `lpinyin` | `^2.0.3` | 拼音（书架排序/搜索） |
| 渲染后备 | `flutter_inappwebview` | `^6.1.5` | 复杂 EPUB 渲染后备 |
| **翻页引擎** | **`flutter_page_curl`** | **`^0.1.0`** | **GLSL 片段着色器翻页（实际渲染方案）** |
| iOS 图标 | `cupertino_icons` | `^1.0.8` | — |

dev_dependencies：`flutter_lints ^6.0.0`。

**项目目录：** `D:\antigravity_projects\squartor`

**分支状态：** 本地主干 `main` 开发。当前工作区有大量未提交改动（约 45 个文件 `M`/`??`），含新模块 `cloud_sync`、`custom_page_curl_view`、`reader_bookmark_*` 等。

**线上部署：** 无（纯客户端 App）。

**数据库：** 无独立数据库。全部持久化依赖 `shared_preferences`（JSON 序列化）+ 本地文件系统（书籍/字体文件落在 app 目录）。

**第三方 API：** 无云端业务 API。**【修正】** 已内置 **WebDAV 云同步**（`cloud_sync_service.dart`，HTTP PUT/GET 同步阅读进度与书签），属用户自配的私有 WebDAV，非第三方 SaaS。

**敏感配置/密钥：** 无。WebDAV 凭据由用户在设置页填入并存于 `shared_preferences`（明文，非加密存储——见第 7 节风险）。

---

## 2. 当前项目进度

### 模块 A：核心阅读器引擎（Core Reader & Curl Engine）

**状态：已完成 / 已验证**

**相关文件：**
- `lib/src/reader/custom_page_curl_view.dart` —— 翻页视图（手势 + 双缓冲 + 着色器驱动）
- `lib/src/reader/reader_txt_view.dart` —— TXT 排版与翻页宿主
- `flutter_page_curl-0.1.0`（pub 缓存）—— `lib/src/page_curl_painter.dart`、`page_curl_controller.dart`、`shaders/page_curl.frag`

**【修正】实际渲染方案：GLSL 片段着色器，而非原交接文档所述的「2D 反射矩阵 + Path 裁剪」。**

原交接文档把翻页描述为「纯 2D 数学反射（P' = P - 2·dot(P-M, n)·n）」，并指向一个 `lib/src/reader/curl/path_curl_painter.dart` 文件。**该文件在仓库中不存在**，反射公式与 `verticalDampening` 机制也**未在 Dart 层出现**。实际实现如下：

1. **着色器渲染：** 翻页效果由 GLSL 片段着色器 `page_curl.frag` 完成。它将屏幕每个像素投影到一根「卷曲圆柱」上，按片段到卷轴的距离 `d` 分三种场景采样：
   - `d > r`：卷轴前方 → 显示下一页（带卷边阴影）
   - `0 < d <= r`：卷曲圆柱表面 → 正面 `p1`（凸面高光）/ 背面 `p2`（去饱和 + 压暗，模拟纸张背面）
   - `d <= 0`：卷轴后方 → 反面展开区或平整当前页
2. **Uniforms：** `uSize`、`uCurlPos`、`uCurlDir`、`uRadius`、`uShadowWidth`、`uBackOpacity`、`uReverse` + 两张纹理（当前页 / 下一页）。
3. **反向翻页：** 通过 `uReverse` 标志对 UV.x 与方向做镜像，复用同一套数学。
4. **Painter：** `lib/src/reader/custom_page_curl_view.dart` 末尾的 `PageCurlPainter` 负责向 shader 灌 uniform 与采样器（注意：这是项目内的一份同名实现，与包内 `page_curl_painter.dart` 并存）。

**双缓冲 / 截图策略：**
- 每页用 `GlobalKey`-keyed `RepaintBoundary` 包裹，启动与翻页后通过 `boundary.toImage()` **预截图**到 `ui.Image` 缓存（`_pageImages`）。
- 卷曲启动时若缓存命中则零延迟喂给 shader；缺失则 `addPostFrameCallback` 异步补截图。
- 翻页 `commitCurl()` 后 `_pageImages.clear()` 清掉过期纹理，并对新页重新预截图。

**【修正】「1 帧视觉欺骗延迟器」机制：** 原文档描述的"按下后故意等 1 帧再隐藏原生文字"在当前代码里**未以该形式实现**。当前防白屏靠的是**预截图缓存 + 异步补截图**，而非 1 帧延迟锁。请勿按"1 帧延迟"去寻找或维护该机制。

**手势流程（`_PageCurlGestureWrapper`）：**
- `onPanStart`：仅记录 pending 起点与左右边缘判定，**不立即启动卷曲**。
- `onPanUpdate`：验证滑动方向匹配后，调用 `controller.startCurl()` 再 `updateCurl()`（方向不符则丢弃 pending）。
- `onPanEnd`：按速度阈值判定 `commit` / `cancel`，提交时用 `AnimationController` 插值 `curlPosition` 到终点后落地。
- 中心点击 / 边缘点击翻页：`onTapUp` 中按 `edgeZoneWidth` 判区，边缘则模拟一次卷曲提交。

**当前问题：** 核心渲染稳定。注意：翻页动画是**可选**的——由 `ReadingStyle.pageTurnAnimation` 控制，默认 `false`（即默认走普通 `PageView.builder`，关闭时 `CustomPageCurlView` 不挂载）。开发与调试翻页时务必确认该开关已打开。

**下一步：** 保持现状。不要把渲染改回 3D 网格，也不要试图把 GLSL 方案"重构成"文档描述的 2D 反射矩阵方案——**当前 GLSL 方案就是唯一正解**。

---

### 模块 B：状态与控制总线（State Management）

**状态：已完成 / 已验证**

**相关文件：** `lib/src/app_state.dart`、`lib/src/reader/reader_state_fields.dart`、`lib/src/reader/reader_barrel.dart`

**核心逻辑：**
- `AppState extends ChangeNotifier`，是全局状态集散地。内部用多个细分 `ChangeNotifier`（`_appThemeChanges`、`_readingStyleChanges`、`_readerChromeChanges`、`_libraryChanges`、`_statisticsChanges`、`_messageChanges`、`_cloudSyncChanges`）并通过 `Listenable.merge` 组合出各页面关心的 `Listenable`（`shelfChanges`、`readerChanges`、`statsScreenChanges`、`settingsChanges` 等）。
- 阅读器内部状态（是否拖拽进度、是否显示菜单、页码等）在 `ReaderStateFields` mixin。
- `main.dart` 构造 `AppState(BookRepository())` 并在启动时触发 `load()` 全量载入快照。

**【修正】持久化闭环已落地，不再是 TODO。** 原文档称"行距、字号等可能尚未对接本地持久化"——实际：
- `ReadingStyle`、`CloudSyncSettings`、`BookEntry`/`BookChapter`/`BookBookmark`、`ImportedFont` 均**已实现** `toJson` / `fromJson`。
- `AppState.updateStyle()` 等所有变更均经防抖 Timer（650ms / 900ms / 2s）落库到 `shared_preferences`（keys：`books.v1`、`style.v1`、`fonts.v1`、`reading_stats.v1`、`shelves.v1`、`cloud_sync_settings.v1`）。
- 启动时 `BookRepository.loadSnapshot()` 一次性还原 books/fonts/shelves/readingStats/style/cloudSyncSettings，并跑 `_upgradeImportedEpubs/Txts/WordCounts` 做版本升级。
- 因此"改字号/背景 → 杀进程冷启动 → 保留"这一验收标准**当前已满足**。

**当前问题：** 无致命问题。注意 `updateStyle` 在字号/行距/段距/字距/页边距/首行缩进/字体变更时会**主动清掉所有章节的 `cachedPageCount`** 并触发书籍重存（因为分页结果变了），这是有意为之，重构时不要破坏该联动。

**下一步：** 维持现状即可。若要新增设置项，遵循既有模式：在 `ReadingStyle` 加字段 + `toJson/fromJson` + `copyWith` + 在 `updateStyle` 的变更判定里补上。

---

### 模块 C：宿架与 UI 交互层（Skeleton & Overlay UI）

**状态：已完成 / 持续打磨**

**相关文件：** `lib/src/reader/reader_screen.dart`（宿架，靠 mixin 组装）及大量 mixin/面板文件：
- 状态/能力 mixin：`reader_state_fields.dart`、`reader_txt_mixin.dart`、`reader_epub_mixin.dart`、`reader_epub_fallback.dart`、`reader_overlay_mixin.dart`、`reader_gesture_mixin.dart`、`reader_navigation_mixin.dart`、`reader_time_mixin.dart`、`reader_bookmark_mixin.dart`
- UI 面板：`reader_menu.dart`、`reader_dock.dart`、`reader_footer.dart`、`reader_progress.dart`、`reader_toc.dart`、`reader_floating_panel.dart`、`reader_bookmark_pull.dart`、`reader_scroll_edge.dart`、`reader_panel_scrim.dart`、`reader_settings.dart`、`reader_glass_palette.dart`、`reader_enums.dart`

**核心逻辑：** 划分屏幕热区（中间菜单、左右翻页），叠加顶部信息栏、底部进度拖拽条、目录、书签下拉、浮动设置面板等。`reader_screen.dart` 本身很薄，能力由 mixin 注入。

**当前问题：** 面板较多，层级遮挡与动画连贯性属常规打磨项，无阻断性 bug。

**下一步：** 细化 UI 过渡动画、补全 TOC 树状渲染等体验细节。

---

### 模块 D：旧版 3D 翻页方案（3D Mesh Curl）

**状态：已废弃**

**核心问题：** 早期试图通过 Mesh 顶点形变实现 3D 翻页，会导致字体发糊、边缘锯齿、反光伪影。

**下一步：** 不要让新 AI 重复尝试 3D 网格方案——**绝对死胡同**。当前的 GLSL 卷曲着色器方案是唯一正解。

---

## 3. 文件结构说明

### 核心目录结构（以实际 `lib/src` 为准）

```
lib/
├── main.dart                      # 入口：初始化 AppState(BookRepository())，挂载 SQuartorApp
├── src/
│   ├── app.dart                   # barrel：export 'home/home_barrel.dart'
│   ├── app_state.dart             # 全局状态集散地（ChangeNotifier）
│   ├── typography.dart            # 字重/排版常量（AppTextWeight 等）
│   ├── epub_flow.dart             # EPUB 流式文档构建（EpubFlowDocument / _FlowBuilder），被 epub_parser 使用
│   ├── book_repository.dart       # barrel：export 'repository/repository_barrel.dart'（向后兼容旧 import）
│   │
│   ├── home/                      # 首页外壳
│   │   ├── app.dart               # SQuartorApp 根组件
│   │   ├── home_shell.dart        # 主壳 + 底部导航
│   │   ├── navigation.dart        # 路由/导航
│   │   ├── lazy_page_stack.dart   # 懒加载页面栈
│   │   ├── reading_now_page.dart  # 「正在阅读」页
│   │   ├── import_overlay.dart    # 导入浮层
│   │   ├── error_overlay.dart     # 错误浮层
│   │   └── home_barrel.dart
│   │
│   ├── shelf/                     # 书架
│   │   ├── shelf_screen.dart      # 书架主页（StatefulWidget，排序/多选/书架切换）
│   │   ├── shelf_widgets.dart / shelf_sheets.dart / shelf_selection.dart / shelf_enums.dart / shelf_barrel.dart
│   │
│   ├── settings/                  # 设置
│   │   ├── settings_screen.dart   # 设置主页（ListView 分组）
│   │   ├── settings_detail.dart   # 设置详情子页
│   │   ├── settings_widgets.dart  # 设置通用组件
│   │   └── settings_barrel.dart
│   │
│   ├── stats/                     # 阅读统计
│   │   ├── stats_screen.dart / stats_detail_screen.dart / stats_daily_detail.dart
│   │   ├── stats_heatmap.dart     # 热力图
│   │   ├── stats_widgets.dart / stats_barrel.dart
│   │
│   ├── reader/                    # 阅读器（见模块 A/C）
│   │   ├── reader_screen.dart     # 宿架（mixin 组装）
│   │   ├── custom_page_curl_view.dart  # 翻页视图 + PageCurlPainter
│   │   ├── reader_txt_view.dart   # TXT 排版/翻页宿主
│   │   ├── reader_txt_mixin.dart  # TXT 分页（TextPainter 单字测量 + 物理截断）
│   │   ├── reader_epub_mixin.dart / reader_epub_fallback.dart
│   │   ├── reader_state_fields.dart / reader_overlay_mixin.dart / reader_gesture_mixin.dart
│   │   ├── reader_navigation_mixin.dart / reader_bookmark_mixin.dart / reader_time_mixin.dart
│   │   ├── reader_menu.dart / reader_dock.dart / reader_footer.dart / reader_progress.dart
│   │   ├── reader_toc.dart / reader_floating_panel.dart / reader_bookmark_pull.dart
│   │   ├── reader_scroll_edge.dart / reader_panel_scrim.dart / reader_settings.dart
│   │   ├── reader_glass_palette.dart / reader_enums.dart / reader_barrel.dart
│   │
│   ├── repository/                # 数据层
│   │   ├── book_repository.dart       # SharedPreferences 读写 + 选书/选字体 + EPUB/TXT 升级
│   │   ├── book_repository_types.dart # BookRepositorySnapshot 等
│   │   ├── epub_parser.dart / epub_types.dart
│   │   ├── txt_parser.dart / txt_types.dart
│   │   ├── cloud_sync_service.dart    # WebDAV 同步（PUT/GET + 凭据校验）
│   │   └── repository_barrel.dart
│   │
│   ├── models/                    # 数据模型（均带 toJson/fromJson）
│   │   ├── models.dart            # barrel
│   │   ├── reading_style.dart     # ReadingStyle（字号/行距/段距/字距/页边距/主题/字体/翻页动画 等）
│   │   ├── app_palette.dart       # AppPalette / ReaderPalette / 主题色卡
│   │   ├── book_entry.dart        # BookEntry / ReaderChapter / BookBookmark
│   │   ├── book_format.dart       # BookFormat 枚举
│   │   ├── cloud_sync_settings.dart  # CloudSyncSettings（WebDAV 配置）
│   │   ├── imported_font.dart     # ImportedFont
│   │
│   ├── widgets/
│   │   └── book_cover.dart        # 封面绘制
│   │
│   └── screens/                   # barrel 转发层（向后兼容）
│       ├── reader_screen.dart     # export '../reader/reader_barrel.dart'
│       ├── settings_screen.dart   # export '../settings/settings_barrel.dart'
│       ├── shelf_screen.dart      # export '../shelf/shelf_barrel.dart'
│       └── stats_screen.dart      # export '../stats/stats_barrel.dart'
```

**关于 barrel 转发层：** `lib/src/app.dart`、`lib/src/book_repository.dart`、`lib/src/screens/*` 都是**纯 re-export 的向后兼容壳**，实际逻辑在 `home/`、`repository/`、`reader/` 等目录。新增代码请直接写进真实目录，不要往这些壳里塞逻辑。`main.dart` 目前仍 `import 'src/book_repository.dart'`（经壳转发到 `repository/`），可正常工作。

### 关键文件说明（新 AI 必读）

| 文件 | 作用 | 注意事项 |
|------|------|----------|
| `lib/main.dart` | 入口，构造 `AppState(BookRepository())` 并 `runApp` | 启动即触发 `load()` 全量载入 |
| `lib/src/app_state.dart` | 全局状态中枢 | 业务逻辑集中处；新增设置走 `updateStyle` |
| `lib/src/reader/reader_screen.dart` | 阅读器宿架 | **不要在此写业务逻辑**，找对应 mixin |
| `lib/src/reader/custom_page_curl_view.dart` | 翻页视图 + `PageCurlPainter` | 渲染靠 GLSL shader；预截图防白屏；翻页动画受 `pageTurnAnimation` 开关控制 |
| `flutter_page_curl-0.1.0/shaders/page_curl.frag` | 卷曲着色器（在 pub 缓存内） | 实际几何数学在此，不在 Dart 层 |
| `lib/src/reader/reader_txt_mixin.dart` & `reader_txt_view.dart` | TXT 排版分页 | `TextPainter` 单字测量 + 物理截断分页 |
| `lib/src/repository/book_repository.dart` | 持久化 + 文件导入 | 所有 KV 落 `shared_preferences` |
| `lib/src/repository/cloud_sync_service.dart` | WebDAV 同步 | 用户自配，凭据明文存于 prefs |

---

## 4. 核心逻辑说明（实际运行流程）

1. **启动：** `main()` → `AppState(BookRepository())` 构造时触发 `load()` → `BookRepository.loadSnapshot()` 从 `shared_preferences` 一次性还原 books/fonts/shelves/readingStats/style/cloudSyncSettings，并跑 EPUB/TXT/字数升级 → `runApp(SQuartorApp)`。
2. **进入阅读：** 用户点书 → 路由挂载 `ReaderScreen`（经 mixin 组装）→ `ReaderTxtMixin.loadAndPaginateTxt` 读取文件、初始化 `TxtLayoutMetrics`、在内存完成本章/全书分页，产出 `safePages`。
3. **渲染准备：** `reader_txt_view` 根据 `style.pageTurnAnimation` 决定挂 `CustomPageCurlView` 还是普通 `PageView.builder`。开翻页动画时，`CustomPageCurlView` 用 `RepaintBoundary` + `toImage()` 预截图当前页与相邻页到 `ui.Image` 缓存。
4. **触屏翻页：** `_PageCurlGestureWrapper` 在 `onPanUpdate` 确认方向后调用 `controller.startCurl()` 启动卷曲；`PageCurlPainter` 把当前/下一页纹理 + 卷曲 uniform 喂给 `page_curl.frag`，shader 按卷曲圆柱几何逐像素采样（正面/背面/下一页 + 阴影）。
5. **松手落地：** `onPanEnd` 按速度阈值判定提交/取消；提交时 `AnimationController` 插值 `curlPosition` 到终点 → `commitCurl()` 切页 → 清旧纹理 → 对新页预截图 → 触发 `onPageChanged` 通知外层更新进度。章节末翻页由导航逻辑（`reader_navigation_mixin`）拉取新章数据。
6. **进度回写：** `AppState.updateBookProgress` 计算章节+页内进度，防抖落库；`addReadingSeconds` 累计阅读时长到统计。

---

## 5. 环境变量与配置

**外部环境变量：** 无 `.env`，不依赖外部服务端环境变量。

**内部配置体系：**
- 存储：`shared_preferences`，keys 见第 2 节模块 B。
- `ReadingStyle`：维护 `fontSize` / `lineHeight` / `paragraphSpacing` / `letterSpacing` / `pageMargin` / `verticalMargin` / `firstLineIndent` / `readingFlow`(paged|scroll) / `reverseTapPageTurn` / `dimJapaneseText` / `pageTurnAnimation` / `brightnessMode`(system|light|dark) / `appTheme` / `customThemeColorValue` / `readerBackground` / 阅读&应用字体路径等。**远不止原文档列的 3 个字段。**
- `AppPalette` / `ReaderPalette`：由 `appTheme` + `brightnessMode` + 可选壁纸取色（`dynamic_color`）解析；包含多套主题色卡与阅读纸张背景。
- `CloudSyncSettings`：WebDAV `endpoint/username/password/enabled/lastUploadAt/lastDownloadAt`。

**新 AI 注意：** 增设阅读器设置时，务必在 `ReadingStyle` 加字段 + `toJson/fromJson` + `copyWith` + 在 `AppState.updateStyle` 的变更判定里补齐，否则不会持久化、不会触发分页重算等联动。

---

## 6. 启动、运行、测试方式

- **安装依赖：** `flutter pub get`（已验证）
- **本地 Debug：** `flutter run`（已验证，连安卓真机或模拟器）
- **构建 Release APK：** `flutter build apk`，或指定构建器 `D:\Code\Android\fvm\versions\3.44.0\bin\flutter.bat build apk`（已验证）
- **测试：** 暂无系统性单元测试规范，主要依赖 UI 黑盒预览（注：`flutter_page_curl` 包自带 controller/gesture/view 的测试，但本项目自身无测试套件）
- **常见启动失败：** 旧库可能报 Kotlin Gradle Plugin (KGP) 兼容性警告，属 Flutter 编译常规警告，不阻断构建。
- **启动后应看到：** 首页（书架/正在阅读 等极简界面）。

**翻页效果调试提醒：** 默认 `pageTurnAnimation = false`，要观察卷曲翻页须在设置里打开「翻页动画」开关，否则看到的是普通 `PageView` 滑动翻页。

---

## 7. 已知问题与坑

### 已解决：松手黑三角闪现（The "Sliding Triangle" Flash）
- **表现：** 斜向拖动后松手，页面回弹到右边缘瞬间右上角闪过黑色切角。
- **【修正】** 原文档称"在底层着色器加入纸张韧性回正机制 (Vertical Dampening)"并指向 `path_curl_painter.dart` 的 `verticalDampening`。**该命名与文件在仓库中均不存在。** 当前 `page_curl.frag` 用卷曲圆柱几何 + 阴影因子处理边缘，未见独立的 dampening 通道。若该问题确已消失，应是 shader 整体几何方案规避了它；若仍复现，需在 shader 层排查，而非去找一个不存在的 `verticalDampening`。
- **新 AI 下一步：** 不要按原文档去找 `verticalDampening`。如需排查闪屏，从 `page_curl.frag` 的边缘采样与阴影分支入手。

### 已解决：快速滑动白屏 / 白边
- **【修正】** 原文档称靠"1 帧延迟等待锁 `_preparingTargetPage`"。**该字段在当前代码中不存在。** 实际防白屏靠 `CustomPageCurlView` 的**预截图缓存**（`_preCaptureAdjacentPages` / `_pageImages`）+ 缺失时 `addPostFrameCallback` 异步补截图。
- **新 AI 注意：** 重构时保证截图与纹理生命周期正确——`commitCurl` 后 `_pageImages.clear()`、`dispose` 里释放 `ui.Image`，切勿提前销毁仍被 shader 引用的纹理。

### 风险项（原文档未提，实测发现）
- **WebDAV 凭据明文存储：** `CloudSyncSettings.password` 经 `toJson` 直接写进 `shared_preferences`（明文）。若要上架或重视安全，应改用平台安全存储（如 `flutter_secure_storage`）。当前因无线上部署、属用户自配私有 WebDAV，风险可控。
- **工作区未提交改动较多：** 约 45 个文件处于 `M`/`??`，含若干新模块。动手前建议先 review diff、决定提交/分支策略，避免与他人或历史改动冲突。

---

## 8. 最近修改记录

### 修改 1：翻页松手闪屏修复 + 跨章连贯性
- **原因：** 斜向松手回弹的切角闪屏；跨章体验断裂。
- **【修正】实际改法：** 当前实现通过 GLSL 卷曲几何 + 预截图双缓冲处理闪屏，并通过 `reader_navigation_mixin` 处理跨章数据拉取。原文档所述"在 `PathPageCurlPainter` 添加距离计算 + 新增 `onEdgeNext/onEdgePrevious` 闭包"对应的代码结构在当前仓库中**未以该形式存在**——翻页提交回调为 `onPageChanged`，跨章由导航 mixin 驱动。
- **验证：** 原文档称双机型构建验证通过、极其丝滑。当前代码可正常构建。
- **可能影响：** 跨章跳跃依赖导航 mixin 正确重载章节数据；外层若未正确触发重载可能导致翻页空白。

### 近期新增模块（git status 中 `??` 的新文件）
- `lib/src/models/cloud_sync_settings.dart` + `lib/src/repository/cloud_sync_service.dart` —— WebDAV 云同步
- `lib/src/reader/custom_page_curl_view.dart` —— 着色器翻页视图（替换/新增于旧方案）
- `lib/src/reader/reader_bookmark_mixin.dart` + `reader_bookmark_pull.dart` —— 书签下拉与管理
- `stash.patch` / `stash_utf8.patch` —— 仓库根目录下的两个补丁文件（用途待确认，疑似临时暂存）

---

## 9. 下一步开发计划

### P1：阅读设置本地持久化闭环
**【修正】该目标已基本完成。** `ReadingStyle` / `BookEntry` 等均已 `toJson/fromJson` 并经 `AppState` 防抖落 `shared_preferences`，冷启动可还原。
- **剩余可补强项：** 若新增设置字段，按既有模式补齐即可；可考虑给持久化加单元测试覆盖 `toJson/fromJson` 往返。

### P2：书架模块的基础 UI 及本地解析入库
**【修正】该目标已基本完成。** `shelf/` 模块（主页 + 排序 + 多选 + 书架分组 + sheets）与 `repository/`（EPUB/TXT 解析 + 选书/选文件夹导入 + 封面）均已落地。
- **剩余可补强项：** 性能（大书架虚拟化）、封面生成的健壮性、可能的 SQLite/Isar 迁移（当前全 JSON，书量极大时 `compute` 编解码可能成为瓶颈）。

### P3：阅读器内部交互细节打磨（长按选词与划线）
**状态：** 待做。可基于 Flutter `SelectionArea` 或底层手势 + 文字偏移映射实现选词菜单。

### 开发红线
- ❌ **不要重构翻页渲染层**（`custom_page_curl_view.dart` / `page_curl.frag` / `flutter_page_curl` 包内代码）。当前 GLSL 方案稳定，是唯一正解。
- ❌ **不要尝试 3D 网格翻页**——死胡同。
- ❌ **不要按原交接文档去找 `path_curl_painter.dart` / `verticalDampening` / `1帧延迟锁` / `onEdgeNext`**——这些在当前代码中均不存在，按文档去维护会找错地方。**以本文档为准。**
- ❌ **不要过度投入 EPUB 复杂 CSS 兼容**——保证基础文字/段落解析可用即可。
- ❌ **不要接花哨的第三方在线书籍 API**——先做好本地阅读器。
- ⚠️ 翻页动画默认关闭，调试翻页前先在设置里打开 `pageTurnAnimation`。

---

## 附：与原交接文档的主要出入汇总

| 原文档描述 | 实际情况 |
|-----------|---------|
| 翻页用「2D 反射矩阵 + Path 裁剪」，公式 P'=P-2·dot(P-M,n)·n | 实为 **GLSL 片段着色器** `page_curl.frag` 的卷曲圆柱几何，Dart 层无反射矩阵 |
| `lib/src/reader/curl/path_curl_painter.dart` | **不存在**；Painter 在 `custom_page_curl_view.dart` 末尾 |
| `verticalDampening` 机制 | **不存在**于代码 |
| 「1 帧视觉欺骗延迟器」`_preparingTargetPage` | **不存在**；防白屏靠预截图缓存 |
| `onEdgeNext` / `onEdgePrevious` 闭包 | 当前为 `onPageChanged` + 导航 mixin 驱动跨章 |
| 设置项可能未持久化（P1 待做） | **已落地**：全部 `toJson/fromJson` + 防抖落库 |
| 无第三方 API | 有 **WebDAV 云同步**（用户自配） |
| 依赖清单 | 补全：`file_picker`、`url_launcher`、`path`、`flutter_page_curl` 等 |
| 文件结构（`reader/curl/` 等） | 实际为 `reader/` 平铺 + `repository/`、`home/`、`shelf/`、`settings/`、`stats/`、`models/`、`widgets/` + `screens/` 兼容壳 |
| 仅 ReadingStyle 三字段 | 实际 20+ 字段，含翻页动画开关、阅读流模式、字体、主题等 |
