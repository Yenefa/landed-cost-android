package com.landed.africa.engine

/** 计算内核：从 Web 版 src/calc.js 移植；v0.2 起税费/渠道按市场参数化，全部纯函数便于单测。 */
object Calc {

    data class DutyCat(val key: String, val label: String, val rate: Double)
    data class CommCat(val key: String, val label: String, val rate: Double)
    data class Freight(val key: String, val label: String, val unit: String, val value: Double, val transitDays: Int)
    data class CertRule(val name: String, val cats: Set<String>, val costUSD: Double)

    data class Market(
        val id: String,
        val flag: String,
        val countryZh: String,
        val countryEn: String,
        val platform: String,
        val currencyCode: String,
        val currencySymbol: String,
        val fxLocalPerUSD: Double,
        val fxLabel: String,
        val vatRate: Double,
        val paymentProcessing: Double,
        val vatOnFees: Double,
        val outboundPerItemLocal: Double,
        val payoutDays: Int,
        val surchargeOnDuty: Double,
        val cissOnFob: Double,
        val etlsOnCif: Double,
        val idfOnCif: Double,
        val rdlOnCif: Double,
        val dutyCats: List<DutyCat>,
        val commCats: List<CommCat>,
        val certRules: List<CertRule>,
        val defaultCertUSD: Double,
        val defaultClearanceUSD: Double,
        val sources: List<Pair<String, String>>,
        val note: String,
    ) {
        val cycleDaysSea: Int get() = Data.PRODUCTION_DAYS + 38 + Data.CLEARANCE_DAYS + Data.SELL_THROUGH_DAYS + payoutDays
        val commissionRange: String
            get() = "${(commCats.minOf { it.rate } * 100).toInt()}–${(commCats.maxOf { it.rate } * 100).toInt()}%"
    }

    private val DUTY_DEFS = listOf(
        "apparel" to "服装鞋帽",
        "electronics" to "消费电子（手机/电脑/电视）",
        "white_goods" to "白色家电（冰箱/洗衣机/空调）",
        "furniture" to "家具家居",
        "processed_food" to "加工食品饮料",
        "cosmetics" to "美妆个护",
        "auto_parts" to "汽车配件",
        "medical" to "医疗用品",
        "machinery" to "工业机械",
        "books" to "图书教具",
        "other" to "其他（默认税率）",
    )

    private val COMM_DEFS = listOf(
        "phones" to "手机",
        "electronics" to "消费电子",
        "computing" to "电脑办公",
        "fashion" to "服饰鞋包",
        "beauty" to "美妆个护",
        "home" to "家居生活",
        "baby" to "母婴",
        "groceries" to "食品杂货",
        "other" to "其他（默认费率）",
    )

    private fun dutySet(rates: Map<String, Double>): List<DutyCat> =
        DUTY_DEFS.map { (k, label) -> DutyCat(k, label, rates[k] ?: 0.2) }

    private fun commSet(rates: Map<String, Double>): List<CommCat> =
        COMM_DEFS.map { (k, label) -> CommCat(k, label, rates[k] ?: 0.10) }

    object Markets {
        val NIGERIA = Market(
            id = "NG", flag = "🇳🇬", countryZh = "尼日利亚", countryEn = "NIGERIA", platform = "Jumia",
            currencyCode = "NGN", currencySymbol = "₦", fxLocalPerUSD = 1500.0, fxLabel = "NGN / USD",
            vatRate = 0.075, paymentProcessing = 0.015, vatOnFees = 0.075,
            outboundPerItemLocal = 350.0, payoutDays = 21,
            surchargeOnDuty = 0.07, cissOnFob = 0.01, etlsOnCif = 0.005, idfOnCif = 0.0, rdlOnCif = 0.0,
            dutyCats = dutySet(
                mapOf(
                    "apparel" to 0.20, "electronics" to 0.10, "white_goods" to 0.20, "furniture" to 0.20,
                    "processed_food" to 0.20, "cosmetics" to 0.20, "auto_parts" to 0.10, "medical" to 0.05,
                    "machinery" to 0.05, "books" to 0.0, "other" to 0.20,
                )
            ),
            commCats = commSet(
                mapOf(
                    "phones" to 0.05, "electronics" to 0.06, "computing" to 0.07, "fashion" to 0.18,
                    "beauty" to 0.18, "home" to 0.15, "baby" to 0.15, "groceries" to 0.10, "other" to 0.12,
                )
            ),
            certRules = listOf(
                CertRule("SONCAP", setOf("electronics", "white_goods", "auto_parts", "machinery", "medical"), 600.0),
                CertRule("NAFDAC", setOf("cosmetics", "processed_food", "medical"), 500.0),
            ),
            defaultCertUSD = 600.0, defaultClearanceUSD = 450.0,
            sources = listOf(
                "Nigeria Import Duty Calculator 2026（Precebol，含关税/CISS/ETLS/VAT 公式）" to "https://www.precebollogistics.co.uk/nigeria-import-duty-calculator",
                "Jumia 开店及运营费用列表（2026 版，佣金 5%–20%）" to "https://vendor.jumia.com/shop-onboarding-and-related-operating-fees-in-chinese/",
                "Jumia 官方在线定价计算" to "https://www.jumia-global.com.cn/static/JumiaPrice.html",
            ),
            note = "尼日利亚：关税 + 7% 附加费 + CISS 1% + ETLS 0.5% + VAT 7.5%",
        )

        val KENYA = Market(
            id = "KE", flag = "🇰🇪", countryZh = "肯尼亚", countryEn = "KENYA", platform = "Jumia",
            currencyCode = "KES", currencySymbol = "KSh", fxLocalPerUSD = 129.0, fxLabel = "KES / USD",
            vatRate = 0.16, paymentProcessing = 0.020, vatOnFees = 0.16,
            outboundPerItemLocal = 200.0, payoutDays = 14,
            surchargeOnDuty = 0.0, cissOnFob = 0.0, etlsOnCif = 0.0, idfOnCif = 0.035, rdlOnCif = 0.02,
            dutyCats = dutySet(
                mapOf(
                    "apparel" to 0.25, "electronics" to 0.25, "white_goods" to 0.25, "furniture" to 0.25,
                    "processed_food" to 0.25, "cosmetics" to 0.25, "auto_parts" to 0.25, "medical" to 0.0,
                    "machinery" to 0.0, "books" to 0.0, "other" to 0.25,
                )
            ),
            commCats = commSet(
                mapOf(
                    "phones" to 0.05, "electronics" to 0.06, "computing" to 0.07, "fashion" to 0.18,
                    "beauty" to 0.18, "home" to 0.15, "baby" to 0.15, "groceries" to 0.10, "other" to 0.12,
                )
            ),
            certRules = listOf(
                CertRule("KEBS PVoC", setOf("electronics", "white_goods", "auto_parts", "machinery", "medical", "cosmetics", "processed_food"), 600.0),
            ),
            defaultCertUSD = 600.0, defaultClearanceUSD = 500.0,
            sources = listOf(
                "Kenya Revenue Authority · Import Duty / VAT（CIF、IDF 3.5%、RDL 2%、VAT 16%）" to "https://www.kra.go.ke/individual/individual-life-cycle/importation",
                "Jumia Kenya 卖家费用（佣金 5%–20%）" to "https://sellercenter.jumia.com.ke/",
                "KEBS · PVoC 出口前符合性认证" to "https://www.kebs.org/",
            ),
            note = "肯尼亚：关税 + IDF 3.5% + RDL 2% + VAT 16%（参考税率，实际以 KRA 税则为准）",
        )

        val SOUTH_AFRICA = Market(
            id = "ZA", flag = "🇿🇦", countryZh = "南非", countryEn = "SOUTH AFRICA", platform = "Takealot",
            currencyCode = "ZAR", currencySymbol = "R", fxLocalPerUSD = 18.2, fxLabel = "ZAR / USD",
            vatRate = 0.15, paymentProcessing = 0.025, vatOnFees = 0.15,
            outboundPerItemLocal = 65.0, payoutDays = 15,
            surchargeOnDuty = 0.0, cissOnFob = 0.0, etlsOnCif = 0.0, idfOnCif = 0.0, rdlOnCif = 0.0,
            dutyCats = dutySet(
                mapOf(
                    "apparel" to 0.45, "electronics" to 0.15, "white_goods" to 0.20, "furniture" to 0.20,
                    "processed_food" to 0.20, "cosmetics" to 0.20, "auto_parts" to 0.20, "medical" to 0.0,
                    "machinery" to 0.0, "books" to 0.0, "other" to 0.20,
                )
            ),
            commCats = commSet(
                mapOf(
                    "phones" to 0.08, "electronics" to 0.08, "computing" to 0.08, "fashion" to 0.12,
                    "beauty" to 0.12, "home" to 0.10, "baby" to 0.10, "groceries" to 0.08, "other" to 0.10,
                )
            ),
            certRules = listOf(
                CertRule("NRCS LOA", setOf("electronics", "white_goods", "auto_parts", "medical", "machinery"), 700.0),
            ),
            defaultCertUSD = 700.0, defaultClearanceUSD = 550.0,
            sources = listOf(
                "SARS · Customs & Excise / VAT 15%" to "https://www.sars.gov.za/customs-and-excise/",
                "Takealot 卖家佣金与费用" to "https://seller.takealot.com/",
                "NRCS · Letter of Authority（LOA）" to "https://www.nrcs.org.za/",
            ),
            note = "南非：关税 + VAT 15%（参考税率，实际以 SARS 税则为准）",
        )

        val all = listOf(NIGERIA, KENYA, SOUTH_AFRICA)
        fun byId(id: String): Market = all.firstOrNull { it.id == id } ?: NIGERIA
    }

    object Data {
        val freights = listOf(
            Freight("sea_lcl", "海运拼箱 LCL", "USD/CBM", 130.0, 38),
            Freight("air", "空运", "USD/kg", 7.5, 9)
        )
        // 兼容旧单测/旧 UI 的尼日利亚常量
        const val SURCHARGE_ON_DUTY = 0.07
        const val CISS_ON_FOB = 0.01
        const val ETLS_ON_CIF = 0.005
        const val VAT_ON_IMPORT = 0.075
        const val PAYMENT_PROCESSING = 0.015
        const val VAT_ON_FEES = 0.075
        const val OUTBOUND_PER_ITEM_NGN = 350.0
        const val PAYOUT_DAYS = 21
        const val CERT_PER_SHIPMENT_USD = 600.0
        const val CLEARANCE_USD = 450.0
        const val PRODUCTION_DAYS = 15
        const val CLEARANCE_DAYS = 10
        const val SELL_THROUGH_DAYS = 60
        const val DEFAULT_INSURANCE_RATE = 0.003
        const val DEFAULT_RETURN_LOSS_RATE = 0.5
    }

    data class Inputs(
        val unitCostCNY: Double = 45.0,
        val qty: Int = 500,
        val unitWeightKg: Double = 0.6,
        val unitVolumeCbm: Double = 0.004,
        val freightKey: String = "sea_lcl",
        val dutyKey: String = "electronics",
        val commKey: String = "electronics",
        val desiredMargin: Double = 0.20,
        val returnRate: Double = 0.12,
        val adRate: Double = 0.05,
        val targetPriceNGN: Double = 0.0,
        val fxNGNperUSD: Double = 1500.0,
        val fxCNYperUSD: Double = 7.1,
        val certUSD: Double = Data.CERT_PER_SHIPMENT_USD,
        val clearanceUSD: Double = Data.CLEARANCE_USD,
        val domesticFixedCNY: Double = 800.0,
        val insuranceRate: Double = Data.DEFAULT_INSURANCE_RATE,
        val returnLossRate: Double = Data.DEFAULT_RETURN_LOSS_RATE,
        val market: Market = Markets.NIGERIA,
    )

    data class ImportCost(
        val cif: Double, val duty: Double, val surcharge: Double,
        val ciss: Double, val etls: Double, val idf: Double, val rdl: Double,
        val vat: Double, val total: Double
    )

    data class CostItem(val key: String, val label: String, val usd: Double)
    data class SensCell(val returnRate: Double, val netProfit: Double, val netMargin: Double)
    data class SensRow(val fxShock: Double, val fx: Double, val cells: List<SensCell>)

    /** 三国对比的一行：同一批货落在某个市场的结果 */
    data class MarketCompare(
        val marketId: String,
        val flag: String,
        val countryZh: String,
        val platform: String,
        val currencyCode: String,
        val currencySymbol: String,
        val unitLocal: Double,
        val unitCNY: Double,
        val price: Double,
        val netProfit: Double,
        val netMargin: Double,
        val cycleDays: Int,
        val recoverRatio: Double,
        val dutyRate: Double,
        val vatRate: Double,
        val isCurrent: Boolean,
    )

    data class Result(
        val unitNGN: Double, val unitCNY: Double, val batchNGN: Double,
        val dutyRate: Double, val commissionRate: Double,
        val priceNGN: Double, val autoPriceNGN: Double, val breakEvenPriceNGN: Double,
        val netProfitNGN: Double, val netMargin: Double,
        val platformCostNGN: Double, val returnCostNGN: Double,
        val recoverRatio: Double, val breakEvenUnits: Double,
        val cycleDays: Int, val capitalLockedNGN: Double,
        val stack: List<CostItem>, val certSchemes: List<String>,
        val sensitivity: List<SensRow>, val qty: Int, val fxNGNperUSD: Double, val fxCNYperUSD: Double,
        val currencyCode: String, val currencySymbol: String, val marketId: String,
        val marketName: String, val vatRate: Double,
    )

    fun computeImport(
        goodsValueUSD: Double,
        freightUSD: Double,
        dutyRate: Double,
        insuranceRate: Double,
        market: Market = Markets.NIGERIA,
    ): ImportCost {
        val cif = goodsValueUSD + freightUSD + goodsValueUSD * insuranceRate
        val duty = cif * dutyRate
        val surcharge = duty * market.surchargeOnDuty
        val ciss = goodsValueUSD * market.cissOnFob
        val etls = cif * market.etlsOnCif
        val idf = cif * market.idfOnCif
        val rdl = cif * market.rdlOnCif
        val vatBase = cif + duty + surcharge + ciss + etls + idf + rdl
        val vat = vatBase * market.vatRate
        val total = duty + surcharge + ciss + etls + idf + rdl + vat
        return ImportCost(cif, duty, surcharge, ciss, etls, idf, rdl, vat, total)
    }

    private fun certSchemes(cats: List<String>, market: Market): Pair<List<String>, Double> {
        val schemes = mutableListOf<String>()
        var cost = 0.0
        market.certRules.forEach { rule ->
            if (cats.any { rule.cats.contains(it) }) { schemes.add(rule.name); cost += rule.costUSD }
        }
        return schemes to cost
    }

    fun compute(i: Inputs): Result {
        val market = i.market
        val qty = i.qty.coerceAtLeast(1)
        val dutyCat = market.dutyCats.firstOrNull { it.key == i.dutyKey } ?: market.dutyCats.last()
        val commCat = market.commCats.firstOrNull { it.key == i.commKey } ?: market.commCats.last()
        val freight = Data.freights.firstOrNull { it.key == i.freightKey } ?: Data.freights.first()

        val goodsValueCNY = i.unitCostCNY * qty
        val goodsValueUSD = goodsValueCNY / i.fxCNYperUSD
        val freightUSD = if (freight.key == "air") i.unitWeightKg * qty * freight.value
        else i.unitVolumeCbm * qty * freight.value

        val imp = computeImport(goodsValueUSD, freightUSD, dutyCat.rate, i.insuranceRate, market)
        val (schemes, autoCert) = certSchemes(listOf(i.dutyKey, i.commKey), market)
        val certUSD = if (i.certUSD >= 0) i.certUSD else autoCert
        val clearanceUSD = i.clearanceUSD
        val domesticFixedUSD = i.domesticFixedCNY / i.fxCNYperUSD

        val landedTotalUSD = imp.cif + imp.total + certUSD + clearanceUSD + domesticFixedUSD
        val landedUnitUSD = landedTotalUSD / qty
        val landedUnitLocal = landedUnitUSD * i.fxNGNperUSD
        val landedUnitCNY = landedUnitUSD * i.fxCNYperUSD

        val commission = commCat.rate
        val proc = market.paymentProcessing
        val feeVat = (commission + proc) * market.vatOnFees
        val feeRate = commission + proc + feeVat

        val autoPrice = landedUnitLocal / (1.0 - feeRate - i.adRate - i.desiredMargin).coerceAtLeast(0.01)
        val price = if (i.targetPriceNGN > 0) i.targetPriceNGN else autoPrice

        val commissionLocal = price * commission
        val procLocal = price * proc
        val feeVatLocal = (commissionLocal + procLocal) * market.vatOnFees
        val adLocal = price * i.adRate
        val platformCostLocal = commissionLocal + procLocal + feeVatLocal + adLocal

        val returnCostPerUnit = i.returnRate * (market.outboundPerItemLocal + landedUnitLocal * i.returnLossRate)
        val netProfitUnit = price - platformCostLocal - landedUnitLocal - returnCostPerUnit
        val netMargin = if (price > 0) netProfitUnit / price else -1.0

        val contribution = price - platformCostLocal - returnCostPerUnit
        val batchTotalLocal = landedTotalUSD * i.fxNGNperUSD
        val breakEvenUnits = if (contribution > 0) batchTotalLocal / contribution else Double.POSITIVE_INFINITY
        val recoverRatio = breakEvenUnits / qty

        val breakEvenPrice = (landedUnitLocal + i.returnRate * market.outboundPerItemLocal) /
                ((1 - i.returnRate) * (1 - feeRate - i.adRate)).coerceAtLeast(0.01)

        val cycleDays = Data.PRODUCTION_DAYS + freight.transitDays + Data.CLEARANCE_DAYS +
                Data.SELL_THROUGH_DAYS + market.payoutDays
        val capitalLockedLocal = batchTotalLocal + (certUSD + clearanceUSD) * i.fxNGNperUSD

        val stack = buildList {
            add(CostItem("goods", "采购成本", goodsValueUSD))
            add(CostItem("freight", "头程运费", freightUSD))
            add(CostItem("insurance", "保险", goodsValueUSD * i.insuranceRate))
            add(CostItem("duty", "关税", imp.duty))
            if (imp.surcharge > 0) add(CostItem("surcharge", "附加费", imp.surcharge))
            if (imp.ciss > 0) add(CostItem("ciss", "CISS", imp.ciss))
            if (imp.etls > 0) add(CostItem("etls", "ETLS", imp.etls))
            if (imp.idf > 0) add(CostItem("idf", "IDF 进口申报费", imp.idf))
            if (imp.rdl > 0) add(CostItem("rdl", "RDL 铁路发展税", imp.rdl))
            add(CostItem("vat", "进口增值税", imp.vat))
            add(CostItem("cert", "认证", certUSD))
            add(CostItem("clearance", "清关杂费", clearanceUSD))
            add(CostItem("domestic", "国内杂费", domesticFixedUSD))
        }

        val shocks = listOf(-0.15, 0.0, 0.15)
        val rets = listOf(
            (i.returnRate - 0.07).coerceAtLeast(0.0),
            i.returnRate,
            (i.returnRate + 0.08).coerceAtMost(0.6)
        )
        val sensitivity = shocks.map { s ->
            val fx2 = i.fxNGNperUSD * (1 + s)
            val landed2 = landedUnitUSD * fx2
            val cells = rets.map { rr ->
                val rc = rr * (market.outboundPerItemLocal + landed2 * i.returnLossRate)
                val np = price - platformCostLocal - landed2 - rc
                SensCell(rr, np, if (price > 0) np / price else -1.0)
            }
            SensRow(s, fx2, cells)
        }

        return Result(
            unitNGN = landedUnitLocal, unitCNY = landedUnitCNY, batchNGN = batchTotalLocal,
            dutyRate = dutyCat.rate, commissionRate = commission,
            priceNGN = price, autoPriceNGN = autoPrice, breakEvenPriceNGN = breakEvenPrice,
            netProfitNGN = netProfitUnit, netMargin = netMargin,
            platformCostNGN = platformCostLocal, returnCostNGN = returnCostPerUnit,
            recoverRatio = recoverRatio, breakEvenUnits = breakEvenUnits,
            cycleDays = cycleDays, capitalLockedNGN = capitalLockedLocal,
            stack = stack, certSchemes = schemes, sensitivity = sensitivity,
            qty = qty, fxNGNperUSD = i.fxNGNperUSD, fxCNYperUSD = i.fxCNYperUSD,
            currencyCode = market.currencyCode, currencySymbol = market.currencySymbol,
            marketId = market.id, marketName = market.countryZh, vatRate = market.vatRate,
        )
    }

    /**
     * 三国对比：同一批货分别落在三个市场的结果。
     *
     * 语义与 Web 一致：
     *  - 当前市场用用户改过的汇率 / 认证费 / 清关费；
     *  - 其他市场用该市场默认值（certUSD = -1 表示按品类自动算认证费），
     *    否则拿尼日利亚的汇率去算肯尼亚会算错。
     */
    fun compareMarkets(base: Inputs): List<MarketCompare> =
        Markets.all.map { m ->
            val inputs =
                if (m.id == base.market.id) {
                    base.copy(market = m)
                } else {
                    base.copy(
                        market = m,
                        fxNGNperUSD = m.fxLocalPerUSD,
                        certUSD = -1.0,
                        clearanceUSD = m.defaultClearanceUSD,
                    )
                }
            val r = compute(inputs)
            MarketCompare(
                marketId = m.id,
                flag = m.flag,
                countryZh = m.countryZh,
                platform = m.platform,
                currencyCode = m.currencyCode,
                currencySymbol = m.currencySymbol,
                unitLocal = r.unitNGN,
                unitCNY = r.unitCNY,
                price = r.priceNGN,
                netProfit = r.netProfitNGN,
                netMargin = r.netMargin,
                cycleDays = r.cycleDays,
                recoverRatio = r.recoverRatio,
                dutyRate = r.dutyRate,
                vatRate = r.vatRate,
                isCurrent = m.id == base.market.id,
            )
        }
}
