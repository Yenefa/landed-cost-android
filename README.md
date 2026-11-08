# 落地价 · Android 原生版（Jetpack Compose）

把 Web 版「非洲落地成本计算器」原生化：**Kotlin + Jetpack Compose + Material 3**，单 Activity，无 WebView。
计算内核从 Web 版 `calc.js` 一比一移植，逻辑与测试用例保持一致。

- 包名：`com.landed.africa` ｜ 应用名：落地价 ｜ versionName 0.5.0（多市场 + 三国对比 + 分享卡 + PDF）
- minSdk 26 ｜ targetSdk 35 ｜ compileSdk 35
- 产物：`app-debug.apk`（v0.2 约 9.6 MB，debug 签名，可直接装）

## 安装 / 运行

```powershell
# 手机开启 USB 调试并连接后
adb install -r app\build\outputs\apk\debug\app-debug.apk
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

单元测试（JVM，9 个用例）：

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

## 已实现（与 Web 版对齐 + v0.2 多市场）

- **多市场切换**：尼日利亚/Jumia（NGN）、肯尼亚/Jumia（KES）、南非/Takealot（ZAR）；税费公式、渠道费率、汇率、认证与来源卡随市场切换
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
app/src/test/java/com/landed/africa/CalcTest.kt   # 9 个 JVM 单测（含三市场税费/币种）
```

## 真机验证（已完成）

- 设备：**PEHM00 / Android 12 / 1080×2400**
- `adb install -r` 成功，冷启动无崩溃（`logcat` 无 `FATAL EXCEPTION`）
- 已验证：全部区块渲染、上下滚动、输入框编辑后**结果实时重算**（把采购单价 45 改成 20045，落地成本/售价/净利同步变化）
- 截图见 `screenshots/`：01 首页、02 表单、03 成本结构、04 明细、05 敏感性与风险

本轮真机调试修掉的两个问题：

1. **状态栏遮挡**：edge-to-edge 下滚动内容会钻到状态栏图标下面 → 增加与页面同色的顶部/底部不透明遮罩。
2. **统计卡换行**：「140+ 天」在窄屏折成两行 → 改为「140+天」并限制单行。

## 自动化测试

### JVM 单测（9）

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

覆盖：尼日利亚税费公式、默认参数自洽、涨价增利、退货增本、贬值增本、认证识别、三市场均产出正利润与本币符号、肯尼亚 IDF/RDL/VAT16%、南非 VAT15% 无尼日利亚附加费、**三国对比与单市场计算一致、当前市场用改后参数而他市场不被污染**。

### Compose instrumentation（14，真机）

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.landed.africa.MainActivityTest `
  com.landed.africa.test/androidx.test.runner.AndroidJUnitRunner
```

覆盖：默认结果渲染、输入实时重算、关税切换重算、高级面板展开、44dp 触控、字段无障碍标签、滚动到底、市场切换改币种、分享卡渲染、分享/保存/PDF 按钮可达性、PDF 结构与 `%PDF-` 头、**三国对比卡片三市场行**。

结果：**14/14 通过**（PEHM00 / Android 12）。截图 `_design_audit/android-v02/`、`android-v03/`、`android-v05/`。

### 多市场参数（参考值，App 内可核对来源）

| 市场     | 币种    | 主要税费                                          | 渠道                |
| -------- | ------- | ------------------------------------------------- | ------------------- |
| 尼日利亚 | NGN ₦   | 关税 + 7% 附加费 + CISS 1% + ETLS 0.5% + VAT 7.5% | Jumia 佣金 5–18%    |
| 肯尼亚   | KES KSh | 关税 + IDF 3.5% + RDL 2% + VAT 16%                | Jumia 佣金 5–18%    |
| 南非     | ZAR R   | 关税 + VAT 15%                                    | Takealot 佣金 8–12% |

> 税率为 2026 参考值，实际以清关日官方税则为准；App 内“数据来源与假设”按市场给官方链接。

> OPPO/ColorOS 注意：`adb install` 会弹「继续安装」确认；锁屏/休眠会让 instrumentation 启动的 Activity 进入 paused 导致挂起。测试前 `adb shell locksettings set-disabled true` 并保持唤醒。

### 无障碍与对比度

- 字段均带 `contentDescription`（TalkBack 能念字段名）
- 触控目标：字段 ≥56dp、来源链接 48dp、重置 44dp
- 文字对比度：`Muted 5.65:1`、`Dim 4.79:1`、`Amber2 5.25:1`

## 已知限制

- 平板 / 横屏 / 大字体未验证（本机 ROM 拒绝 `wm size`，缺 `WRITE_SECURE_SETTINGS`）。
- 无深色模式（固定浅色）。
- 多市场税率是 2026 参考值（三国官方税则 + 平台费率），上线前需按当期税则复核。
- debug 包未做混淆与正式签名；上架需生成 release keystore。
- 本机模拟器（AVD `smoke` + WHPX + swiftshader）启不来，调试全程用真机；自动化截图需先 `input keyevent KEYCODE_WAKEUP` 并上滑解锁。

## Roadmap

- ✅ v0.2 多市场切换（Kenya/Jumia、South Africa/Takealot）
- ✅ v0.3 分享卡（1080×1350 PNG）+ 保存到相册
- ✅ v0.4 A4 PDF 导出（Downloads/落地价）
- ✅ v0.5 三国对比（NG/KE/ZA 一屏并排，当前市场用改后参数）
- v0.6 汇率/运价在线更新
