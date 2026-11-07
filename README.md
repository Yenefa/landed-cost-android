# 落地价 · Android 原生版（Jetpack Compose）

把 Web 版「非洲落地成本计算器」原生化：**Kotlin + Jetpack Compose + Material 3**，单 Activity，无 WebView。
计算内核从 Web 版 `calc.js` 一比一移植，逻辑与测试用例保持一致。

- 包名：`com.landed.africa` ｜ 应用名：落地价 ｜ versionName 0.1.0
- minSdk 26 ｜ targetSdk 35 ｜ compileSdk 35
- 产物：`landed-cost-v0.1-debug.apk`（约 9.5 MB，debug 签名，可直接装）

## 安装 / 运行

```powershell
# 手机开启 USB 调试并连接后
adb install -r landed-cost-v0.1-debug.apk
adb shell am start -n com.landed.africa/.MainActivity
```

## 重新构建

本机环境：JDK 用 Android Studio 自带 JBR（21），Gradle 8.9（已解压到 `..\tools\gradle-8.9`）。

```powershell
# 需要 JDK 17+（Android Studio 自带 JBR 21 即可）
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug --console=plain
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

首次构建会自动下载 Gradle 8.9 与依赖（AGP 8.7.3 / Kotlin 2.1.0 / Compose BOM 2024.12.01，走阿里云镜像）；
之后进本地缓存可离线增量构建。

单元测试（JVM，6 个用例）：

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

## 已实现（与 Web 版对齐）

- 三段式输入（产品与货值 / 物流与品类 / 渠道与利润）+ 高级参数（汇率、认证费、清关费、国内杂费）
- 全部字段 `rememberSaveable`，旋转屏幕不丢
- 输入即算（Compose 重组驱动，无“计算”按钮）
- 小票式结果卡：单件落地成本、建议售价、净利/净利率、回本率、整批成本、资金周转
- 成本结构堆叠色带 + 明细占比
- 钱去哪了（整批）表
- 敏感性矩阵（奈拉升/贬值 × 退货率）
- 风险提示（净利过薄、退货过高、高税品类、缺认证、回本不了、现金周期长）
- 数据来源可点击外链

## 代码结构

```
app/src/main/java/com/landed/africa/
├── MainActivity.kt            # 入口，enableEdgeToEdge + setContent
├── engine/Calc.kt             # 纯计算内核（移植自 calc.js）+ 数据表
└── ui/
    ├── Theme.kt               # 配色（暖纸底 / 墨黑 / 琥珀）与 M3 主题
    └── LandedApp.kt           # 全部界面与组件
app/src/test/java/com/landed/africa/CalcTest.kt   # 6 个 JVM 单测
```

## 真机验证（已完成）

- 设备：**PEHM00 / Android 12 / 1080×2400**
- `adb install -r` 成功，冷启动无崩溃（`logcat` 无 `FATAL EXCEPTION`）
- 已验证：全部区块渲染、上下滚动、输入框编辑后**结果实时重算**（把采购单价 45 改成 20045，落地成本/售价/净利同步变化）
- 截图见 `screenshots/`：01 首页、02 表单、03 成本结构、04 明细、05 敏感性与风险

本轮真机调试修掉的两个问题：

1. **状态栏遮挡**：edge-to-edge 下滚动内容会钻到状态栏图标下面 → 增加与页面同色的顶部/底部不透明遮罩。
2. **统计卡换行**：「140+ 天」在窄屏折成两行 → 改为「140+天」并限制单行。

## 已知限制

- 目前固定「尼日利亚 / Jumia」，多市场切换（肯尼亚/南非）留到 v0.2，数据结构已按国家/渠道拆好。
- debug 包未做混淆与正式签名；上架需生成 release keystore。
- 本机模拟器（AVD `smoke` + WHPX + swiftshader）启不来，调试全程用真机；自动化截图需先 `input keyevent KEYCODE_WAKEUP` 并上滑解锁。

## Roadmap

- v0.2 多市场切换（Kenya/Kilimall、South Africa/Takealot）
- v0.3 分享卡（生成图片）+ 导出 PDF
- v0.4 汇率/运价在线更新
