package com.landed.africa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landed.africa.engine.Calc
import java.text.DecimalFormat

private fun fmt(v: Double, digits: Int = 0): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    val pattern = if (digits > 0) "#,##0." + "0".repeat(digits) else "#,##0"
    return DecimalFormat(pattern).format(v)
}

private fun money(v: Double, symbol: String, digits: Int = 0): String = symbol + fmt(v, digits)
private fun ratePct(v: Double): String = fmt(v * 100, 1).removeSuffix(".0") + "%"
private fun pct(v: Double, digits: Int = 1): String =
    if (v.isNaN() || v.isInfinite()) "—" else fmt(v * 100, digits) + "%"

private fun d(s: String): Double = s.toDoubleOrNull() ?: 0.0
private fun i(s: String): Int = s.toIntOrNull() ?: 1

@Composable
fun LandedApp() {
    LandedTheme {
        Surface(color = Palette.Paper, modifier = Modifier.fillMaxSize()) {
            var marketId by rememberSaveable { mutableStateOf(Calc.Markets.NIGERIA.id) }
            val market = Calc.Markets.byId(marketId)
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    TopBar(market) { marketId = it.id }
                    Hero(market)
                    CalculatorBody(market)
                }
                // 顶部不透明遮罩：滚动时内容不会跑到状态栏图标下面
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(topInset)
                        .background(Palette.Paper)
                        .align(Alignment.TopCenter)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(bottomInset)
                        .background(Palette.Paper)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ---------------- top bar ----------------
@Composable
private fun TopBar(market: Calc.Market, onSelect: (Calc.Market) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.Ink),
            contentAlignment = Alignment.Center
        ) {
            Text("落", color = Palette.Paper, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("落地价", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Palette.Ink)
            Text("LANDED COST", fontSize = 8.sp, letterSpacing = 2.sp, color = Palette.Muted, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(999.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Line2),
                modifier = Modifier.testTag("market-switcher")
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${market.flag} ${market.countryZh} · ${market.platform}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink2
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("▾", fontSize = 10.sp, color = Palette.Muted)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Calc.Markets.all.forEach { m ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("${m.flag} ${m.countryZh} · ${m.platform}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${m.currencyCode} · 佣金 ${m.commissionRange}", fontSize = 10.sp, color = Palette.Dim)
                            }
                        },
                        onClick = { onSelect(m); expanded = false },
                        modifier = Modifier.testTag("market-${m.id}")
                    )
                }
            }
        }
    }
}

// ---------------- hero ----------------
@Composable
private fun Hero(market: Calc.Market) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Palette.Amber))
            Spacer(Modifier.width(8.dp))
            Text(
                "${market.countryEn} · ${market.platform.uppercase()} · 2026",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = Palette.Amber2,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "先算清落地价，\n再谈非洲梦。",
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Palette.Ink,
            letterSpacing = (-0.6).sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "关税、增值税、认证、清关、佣金、退货——每一笔都摊开给你看。",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Palette.Muted
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip(ratePct(market.vatRate), "进口增值税", Modifier.weight(1f))
            StatChip(market.commissionRange, "平台佣金", Modifier.weight(1f))
            StatChip("${market.cycleDaysSea}+天", "资金周转", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Line)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                color = Palette.Ink, maxLines = 1, softWrap = false)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Palette.Muted, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------- calculator ----------------
@Composable
private fun CalculatorBody(market: Calc.Market) {
    var unitCost by rememberSaveable { mutableStateOf("45") }
    var qty by rememberSaveable { mutableStateOf("500") }
    var weight by rememberSaveable { mutableStateOf("0.6") }
    var volume by rememberSaveable { mutableStateOf("0.004") }
    var freightKey by rememberSaveable { mutableStateOf("sea_lcl") }
    var dutyKey by rememberSaveable { mutableStateOf("electronics") }
    var commKey by rememberSaveable { mutableStateOf("electronics") }
    var margin by rememberSaveable { mutableStateOf("20") }
    var ret by rememberSaveable { mutableStateOf("12") }
    var ad by rememberSaveable { mutableStateOf("5") }
    var price by rememberSaveable { mutableStateOf("") }
    val fxDefault = if (market.fxLocalPerUSD % 1.0 == 0.0) market.fxLocalPerUSD.toInt().toString() else market.fxLocalPerUSD.toString()
    var fx by rememberSaveable(market.id) { mutableStateOf(fxDefault) }
    var cny by rememberSaveable { mutableStateOf("7.1") }
    var cert by rememberSaveable(market.id) { mutableStateOf(market.defaultCertUSD.toInt().toString()) }
    var clearance by rememberSaveable(market.id) { mutableStateOf(market.defaultClearanceUSD.toInt().toString()) }
    var domestic by rememberSaveable { mutableStateOf("800") }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }

    val inputs = Calc.Inputs(
        unitCostCNY = d(unitCost),
        qty = i(qty).coerceAtLeast(1),
        unitWeightKg = d(weight),
        unitVolumeCbm = d(volume),
        freightKey = freightKey,
        dutyKey = dutyKey,
        commKey = commKey,
        desiredMargin = d(margin) / 100.0,
        returnRate = d(ret) / 100.0,
        adRate = d(ad) / 100.0,
        targetPriceNGN = d(price),
        fxNGNperUSD = d(fx).coerceAtLeast(1.0),
        fxCNYperUSD = d(cny).coerceAtLeast(1.0),
        certUSD = d(cert),
        clearanceUSD = d(clearance),
        domesticFixedCNY = d(domestic),
        market = market
    )
    val res = Calc.compute(inputs)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        ResultCard(res)
        Spacer(Modifier.height(16.dp))

        StepCard("01", "产品与货值", "你卖什么，进多少") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("采购单价", "¥ / 件", unitCost, { unitCost = it }, Modifier.weight(1f), KeyboardType.Decimal)
                LabeledField("采购数量", "件", qty, { qty = it }, Modifier.weight(1f), KeyboardType.Number)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("单件重量", "kg", weight, { weight = it }, Modifier.weight(1f), KeyboardType.Decimal)
                LabeledField("单件体积", "CBM", volume, { volume = it }, Modifier.weight(1f), KeyboardType.Decimal)
            }
        }
        Spacer(Modifier.height(14.dp))

        StepCard("02", "物流与品类", "怎么运、按什么税率") {
            DropdownField(
                "头程方式",
                Calc.Data.freights.map { it.key to (it.label + " · " + fmt(it.value, 0) + " " + it.unit + " · " + it.transitDays + "天") },
                freightKey, { freightKey = it }
            )
            Spacer(Modifier.height(12.dp))
            DropdownField(
                "品类（决定进口关税）",
                market.dutyCats.map { it.key to (it.label + " · " + fmt(it.rate * 100, 0) + "%") },
                dutyKey, { dutyKey = it }
            )
        }
        Spacer(Modifier.height(14.dp))

        StepCard("03", "渠道与利润", "平台抽多少，你想赚多少") {
            DropdownField(
                "类目（决定平台佣金）",
                market.commCats.map { it.key to (it.label + " · " + fmt(it.rate * 100, 0) + "%") },
                commKey, { commKey = it }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("目标净利率", "%", margin, { margin = it }, Modifier.weight(1f), KeyboardType.Decimal)
                LabeledField("退货率", "%", ret, { ret = it }, Modifier.weight(1f), KeyboardType.Decimal)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("广告占比", "%", ad, { ad = it }, Modifier.weight(1f), KeyboardType.Decimal)
                LabeledField("目标售价", "${market.currencySymbol} · 留空自动", price, { price = it }, Modifier.weight(1f), KeyboardType.Decimal)
            }
        }
        Spacer(Modifier.height(12.dp))

        // 高级参数
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Line2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advanced-toggle")
                        .minimumInteractiveComponentSize()
                        .clickable { advancedOpen = !advancedOpen }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (advancedOpen) "−" else "+", fontWeight = FontWeight.Bold, color = Palette.Ink2)
                    Spacer(Modifier.width(10.dp))
                    Text("高级参数", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Palette.Ink)
                    Spacer(Modifier.width(8.dp))
                    Text("汇率 / 认证 / 杂费", fontSize = 12.sp, color = Palette.Dim)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .testTag("btn-reset")
                            .heightIn(min = 44.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                unitCost = "45"; qty = "500"; weight = "0.6"; volume = "0.004"
                                freightKey = "sea_lcl"; dutyKey = "electronics"; commKey = "electronics"
                                margin = "20"; ret = "12"; ad = "5"; price = ""
                                fx = fxDefault; cny = "7.1"
                                cert = market.defaultCertUSD.toInt().toString()
                                clearance = market.defaultClearanceUSD.toInt().toString()
                                domestic = "800"
                            }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("恢复默认", fontSize = 12.sp, color = Palette.Muted, fontWeight = FontWeight.Bold)
                    }
                }
                if (advancedOpen) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledField("汇率", market.fxLabel, fx, { fx = it }, Modifier.weight(1f), KeyboardType.Decimal)
                        LabeledField("汇率", "CNY / USD", cny, { cny = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LabeledField("认证费", "$ / 批", cert, { cert = it }, Modifier.weight(1f), KeyboardType.Decimal)
                        LabeledField("清关费", "$ / 批", clearance, { clearance = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledField("国内杂费", "¥ / 批", domestic, { domestic = it }, Modifier.fillMaxWidth(), KeyboardType.Decimal)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        CostStructureCard(res)
        Spacer(Modifier.height(16.dp))
        MoneyTableCard(res)
        Spacer(Modifier.height(16.dp))
        SensitivityCard(res)
        Spacer(Modifier.height(16.dp))
        RiskCard(res, inputs)
        Spacer(Modifier.height(16.dp))
        SourcesCard(market)
        Spacer(Modifier.height(18.dp))
        Text(
            "本工具为估算器，不构成报价或法律/税务意见。税率、费率、汇率以官方与合同为准 · v0.2 多市场",
            fontSize = 11.sp,
            color = Palette.Dim,
            modifier = Modifier.padding(bottom = 28.dp)
        )
    }
}

// ---------------- components ----------------
@Composable
private fun ResultCard(r: Calc.Result) {
    Surface(shape = RoundedCornerShape(22.dp), color = Palette.Receipt, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("单件落地成本", fontSize = 11.sp, letterSpacing = 2.sp, color = Palette.ReceiptMuted, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(r.currencySymbol, fontSize = 17.sp, color = Palette.ReceiptAccent, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, end = 2.dp).testTag("currency-symbol"))
                Text(fmt(r.unitNGN), fontFamily = FontFamily.Monospace, fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-1.5).sp,
                    modifier = Modifier.testTag("result-landed"))
            }
            Spacer(Modifier.height(6.dp))
            Text("≈ ¥" + fmt(r.unitCNY, 2) + " · 含关税 " + fmt(r.dutyRate * 100, 0) + "% · 增值税 " + ratePct(r.vatRate) + " · 认证与清关",
                fontSize = 12.sp, color = Palette.ReceiptMuted)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
            ResultRow("建议售价", money(r.priceNGN, r.currencySymbol), Color.White, tag = "result-price")
            ResultRow("单件净利 / 净利率", money(r.netProfitNGN, r.currencySymbol) + "  " + pct(r.netMargin, 1),
                if (r.netProfitNGN >= 0) Color(0xFF6FE3B6) else Color(0xFFFF8C77), tag = "result-profit")
            ResultRow("回本率", if (r.recoverRatio.isInfinite()) "无法回本" else pct(r.recoverRatio, 0),
                when { r.recoverRatio > 1 -> Color(0xFFFF8C77); r.recoverRatio > 0.85 -> Palette.ReceiptAccent; else -> Color(0xFF6FE3B6) }, tag = "result-be")
            ResultRow("整批总成本", money(r.batchNGN, r.currencySymbol), Color.White, tag = "result-batch")
            ResultRow("资金周转", r.cycleDays.toString() + " 天", Color.White, last = true, tag = "result-cycle")
            Spacer(Modifier.height(8.dp))
            Text("估算值，非报价 · 所有参数可改，结果实时重算", fontSize = 10.sp, color = Palette.ReceiptMuted)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: Color, last: Boolean = false, tag: String? = null) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, color = Palette.ReceiptMuted, modifier = Modifier.weight(1f))
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = valueColor,
                modifier = if (tag != null) Modifier.testTag(tag) else Modifier)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
    }
}

@Composable
private fun StepCard(num: String, title: String, sub: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(num, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Palette.Paper, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                    Text(sub, fontSize = 12.sp, color = Palette.Muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    unit: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink2)
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 10.sp, color = Palette.Dim, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = Palette.Ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Amber,
                unfocusedBorderColor = Palette.Line2,
                focusedContainerColor = Palette.FieldBg,
                unfocusedContainerColor = Palette.FieldBg,
                cursorColor = Palette.Amber
            ),
            modifier = Modifier.fillMaxWidth().testTag(label).semantics { contentDescription = label }
        )
    }
}

@Composable
private fun DropdownField(label: String, options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedKey }?.second ?: ""
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink2)
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(11.dp),
                color = Palette.FieldBg,
                border = BorderStroke(1.dp, Palette.Line2),
                modifier = Modifier.fillMaxWidth().testTag(label)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedLabel, fontSize = 13.sp, color = Palette.Ink, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("▾", fontSize = 12.sp, color = Palette.Muted)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (k, v) ->
                    DropdownMenuItem(text = { Text(v, fontSize = 13.sp) }, onClick = { onSelect(k); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, sub: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Spacer(Modifier.width(10.dp))
                Text(sub, fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.padding(bottom = 1.dp))
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun CostStructureCard(r: Calc.Result) {
    val total = r.stack.sumOf { it.usd }
    SectionCard("成本结构", "每一件落地价里都有什么") {
        Row(
            Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(11.dp)).testTag("cost-bar")
                .border(1.dp, Palette.Line2, RoundedCornerShape(11.dp))
        ) {
            r.stack.filter { it.usd > 0 }.forEach { item ->
                Box(
                    Modifier.weight(item.usd.toFloat().coerceAtLeast(0.001f))
                        .fillMaxSize()
                        .background(Palette.stackColor(item.key))
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        r.stack.filter { it.usd > 0 }.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Palette.stackColor(item.key)))
                Spacer(Modifier.width(9.dp))
                Text(item.label, fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.weight(1f))
                Text(pct(item.usd / total, 1), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Palette.Ink)
            }
        }
    }
}

@Composable
private fun MoneyTableCard(r: Calc.Result) {
    val total = r.stack.sumOf { it.usd }
    SectionCard("钱去哪了", "整批金额与占比") {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("项目", fontSize = 10.sp, color = Palette.Dim, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
            Text("金额 ${r.currencySymbol}", fontSize = 10.sp, color = Palette.Dim, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("占比", fontSize = 10.sp, color = Palette.Dim, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
        }
        r.stack.filter { it.usd > 0 }.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.label, fontSize = 12.sp, color = Palette.Ink2, modifier = Modifier.weight(1.3f))
                Text(money(item.usd * r.fxNGNperUSD, r.currencySymbol), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = Palette.Ink, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(pct(item.usd / total, 1), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = Palette.Muted, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("落地总成本", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, modifier = Modifier.weight(1.3f))
            Text(money(r.batchNGN, r.currencySymbol), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Palette.Ink, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("100%", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Palette.Muted,
                modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun SensitivityCard(r: Calc.Result) {
    SectionCard("敏感性", "汇率 × 退货率 → 单件净利 ${r.currencySymbol}") {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("汇率 \\ 退货率", fontSize = 10.sp, color = Palette.Dim, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
            r.sensitivity[0].cells.forEach { c ->
                Text(pct(c.returnRate, 0), fontSize = 10.sp, color = Palette.Dim, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        r.sensitivity.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        row.fxShock == 0.0 -> "基准 · " + fmt(row.fx, 0)
                        row.fxShock > 0 -> "本币贬值 15% ↓"
                        else -> "本币升值 15% ↑"
                    },
                    fontSize = 11.sp, color = Palette.Ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f)
                )
                row.cells.forEach { c ->
                    val col = when {
                        c.netMargin < 0 -> Palette.Red
                        c.netMargin < 0.08 -> Palette.Amber2
                        else -> Palette.Green
                    }
                    Text(fmt(c.netProfit), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = col,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
        }
        Spacer(Modifier.height(10.dp))
        Text("格内为单件净利（${r.currencySymbol}）。基准售价 " + money(r.priceNGN, r.currencySymbol) + "，单件落地 " + money(r.unitNGN, r.currencySymbol) + "。本币贬值时你的美元成本折成本币更贵，售价却未必能同步涨。",
            fontSize = 11.sp, lineHeight = 16.sp, color = Palette.Muted)
    }
}

// ---------------- risks ----------------
private data class Risk(val level: String, val title: String, val desc: String)

private fun buildRisks(r: Calc.Result, i: Calc.Inputs): List<Risk> {
    val out = mutableListOf<Risk>()
    val m = r.netMargin
    if (r.recoverRatio > 1) out.add(Risk("high", "整批卖完也回不了本",
        "按当前售价需卖出 " + fmt(Math.ceil(r.breakEvenUnits)) + " 件才能覆盖成本，而本批只有 " + fmt(r.qty.toDouble()) + " 件。要么涨价，要么压落地成本。"))
    if (m < 0) out.add(Risk("high", "单件亏损", "扣完平台费、广告和退货后，每件亏 " + money(-r.netProfitNGN, r.currencySymbol) + "。"))
    else if (m < 0.08) out.add(Risk("mid", "净利率偏薄（" + pct(m, 1) + "）", "本币再贬 10% 就可能转亏，建议把售价锚定美元成本做动态调价。"))
    if (i.returnRate >= 0.2) out.add(Risk("high", "退货 / 妥投失败率 ≥ 20%", "${r.marketName}电商（尤其货到付款）未妥投会产生往返运费与仓储，实际损失常高于账面。"))
    else if (i.returnRate >= 0.15) out.add(Risk("mid", "退货率偏高", "按 " + pct(i.returnRate, 0) + " 计，每件已摊入 " + money(r.returnCostNGN, r.currencySymbol) + " 退货成本。"))
    if (r.dutyRate >= 0.2) out.add(Risk("mid", "高税品类（关税 " + fmt(r.dutyRate * 100, 0) + "%）", "关税 + 增值税占落地价大头，低值重货尤其容易被运费和税吃穿。"))
    if (r.certSchemes.isNotEmpty() && i.certUSD <= 0) out.add(Risk("high", "该品类需要 " + r.certSchemes.joinToString(" / ") + "，但认证费为 0", "没有认证无法清关 / 上架，请把认证费补进高级参数。"))
    if (r.cycleDays > 150) out.add(Risk("mid", "资金周转约 " + r.cycleDays + " 天", "资金占用约 " + money(r.capitalLockedNGN, r.currencySymbol) + "，备货前先确认能撑多久。"))
    if (i.adRate > 0.15) out.add(Risk("mid", "广告占比 > 15%", "独立站 / 新店常见，但会直接吃掉净利，先测自然转化。"))
    if (out.isEmpty()) out.add(Risk("ok", "主要风险可控", "按当前参数，净利率与回本率都在安全区，仍建议小批量试单验证真实退货率与运费。"))
    return out
}

@Composable
private fun RiskCard(r: Calc.Result, i: Calc.Inputs) {
    SectionCard("风险提示", "按当前参数自动判定") {
        Column(Modifier.testTag("risk-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            buildRisks(r, i).forEach { risk ->
                val color = when (risk.level) {
                    "high" -> Palette.Red
                    "mid" -> Palette.Amber
                    else -> Palette.Green
                }
                Row {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(risk.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        Text(risk.desc, fontSize = 12.sp, lineHeight = 17.sp, color = Palette.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesCard(market: Calc.Market) {
    val uri = LocalUriHandler.current
    val sources = market.sources
    SectionCard("数据来源与假设", "都能点开核对") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sources.forEach { (label, url) ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(Palette.Amber))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        label,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Palette.Green,
                        modifier = Modifier.minimumInteractiveComponentSize().clickable { runCatching { uri.openUri(url) } }
                    )
                }
            }
            Text(market.note + "。汇率、运费、认证费均为参考值，输入区可直接改；改动后所有结果实时重算。",
                fontSize = 12.sp, color = Palette.Muted)
        }
    }
}
