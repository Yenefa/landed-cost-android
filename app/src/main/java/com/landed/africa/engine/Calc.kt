package com.landed.africa.engine

/** 计算内核：从 Web 版 src/calc.js 一比一移植，全部纯函数，便于单测。 */
object Calc {

    data class DutyCat(val key: String, val label: String, val rate: Double)
    data class CommCat(val key: String, val label: String, val rate: Double)
    data class Freight(val key: String, val label: String, val unit: String, val value: Double, val transitDays: Int)

    object Data {
        val dutyCats = listOf(
            DutyCat("apparel", "服装鞋帽", 0.20),
            DutyCat("electronics", "消费电子（手机/电脑/电视）", 0.10),
            DutyCat("white_goods", "白色家电（冰箱/洗衣机/空调）", 0.20),
            DutyCat("furniture", "家具家居", 0.20),
            DutyCat("processed_food", "加工食品饮料", 0.20),
            DutyCat("cosmetics", "美妆个护", 0.20),
            DutyCat("auto_parts", "汽车配件", 0.10),
            DutyCat("medical", "医疗用品", 0.05),
            DutyCat("machinery", "工业机械", 0.05),
            DutyCat("books", "图书教具", 0.00),
            DutyCat("other", "其他（默认 20%）", 0.20)
        )
        val commCats = listOf(
            CommCat("phones", "手机", 0.05),
            CommCat("electronics", "消费电子", 0.06),
            CommCat("computing", "电脑办公", 0.07),
            CommCat("fashion", "服饰鞋包", 0.18),
            CommCat("beauty", "美妆个护", 0.18),
            CommCat("home", "家居生活", 0.15),
            CommCat("baby", "母婴", 0.15),
            CommCat("groceries", "食品杂货", 0.10),
            CommCat("other", "其他（默认 12%）", 0.12)
        )
        val freights = listOf(
            Freight("sea_lcl", "海运拼箱 LCL", "USD/CBM", 130.0, 38),
            Freight("air", "空运", "USD/kg", 7.5, 9)
        )

        // 尼日利亚进口税费率
        const val SURCHARGE_ON_DUTY = 0.07
        const val CISS_ON_FOB = 0.01
        const val ETLS_ON_CIF = 0.005
        const val VAT_ON_IMPORT = 0.075
        // Jumia
        const val PAYMENT_PROCESSING = 0.015
        const val VAT_ON_FEES = 0.075
        const val OUTBOUND_PER_ITEM_NGN = 350.0
        const val PAYOUT_DAYS = 21
        // 杂费
        const val CERT_PER_SHIPMENT_USD = 600.0
        const val CLEARANCE_USD = 450.0
        // 周期
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
        val returnLossRate: Double = Data.DEFAULT_RETURN_LOSS_RATE
    )

    data class ImportCost(
        val cif: Double, val duty: Double, val surcharge: Double,
        val ciss: Double, val etls: Double, val vat: Double, val total: Double
    )

    data class CostItem(val key: String, val label: String, val usd: Double)
    data class SensCell(val returnRate: Double, val netProfit: Double, val netMargin: Double)
    data class SensRow(val fxShock: Double, val fx: Double, val cells: List<SensCell>)

    data class Result(
        val unitNGN: Double, val unitCNY: Double, val batchNGN: Double,
        val dutyRate: Double, val commissionRate: Double,
        val priceNGN: Double, val autoPriceNGN: Double, val breakEvenPriceNGN: Double,
        val netProfitNGN: Double, val netMargin: Double,
        val platformCostNGN: Double, val returnCostNGN: Double,
        val recoverRatio: Double, val breakEvenUnits: Double,
        val cycleDays: Int, val capitalLockedNGN: Double,
        val stack: List<CostItem>, val certSchemes: List<String>,
        val sensitivity: List<SensRow>, val qty: Int, val fxNGNperUSD: Double, val fxCNYperUSD: Double
    )

    fun computeImport(goodsValueUSD: Double, freightUSD: Double, dutyRate: Double, insuranceRate: Double): ImportCost {
        val cif = goodsValueUSD + freightUSD + goodsValueUSD * insuranceRate
        val duty = cif * dutyRate
        val surcharge = duty * Data.SURCHARGE_ON_DUTY
        val ciss = goodsValueUSD * Data.CISS_ON_FOB
        val etls = cif * Data.ETLS_ON_CIF
        val vatBase = cif + duty + surcharge + ciss + etls
        val vat = vatBase * Data.VAT_ON_IMPORT
        val total = duty + surcharge + ciss + etls + vat
        return ImportCost(cif, duty, surcharge, ciss, etls, vat, total)
    }

    private fun certSchemes(cats: List<String>): Pair<List<String>, Double> {
        val schemes = mutableListOf<String>()
        var cost = 0.0
        val soncap = listOf("electronics", "white_goods", "auto_parts", "machinery", "medical")
        val nafdac = listOf("cosmetics", "processed_food", "medical")
        if (cats.any { soncap.contains(it) }) { schemes.add("SONCAP"); cost += Data.CERT_PER_SHIPMENT_USD }
        if (cats.any { nafdac.contains(it) }) { schemes.add("NAFDAC"); cost += 1500.0 / 3.0 }
        return schemes to cost
    }

    fun compute(i: Inputs): Result {
        val qty = i.qty.coerceAtLeast(1)
        val dutyCat = Data.dutyCats.firstOrNull { it.key == i.dutyKey } ?: Data.dutyCats.last()
        val commCat = Data.commCats.firstOrNull { it.key == i.commKey } ?: Data.commCats.last()
        val freight = Data.freights.firstOrNull { it.key == i.freightKey } ?: Data.freights.first()

        val goodsValueCNY = i.unitCostCNY * qty
        val goodsValueUSD = goodsValueCNY / i.fxCNYperUSD
        val freightUSD = if (freight.key == "air") i.unitWeightKg * qty * freight.value
        else i.unitVolumeCbm * qty * freight.value

        val imp = computeImport(goodsValueUSD, freightUSD, dutyCat.rate, i.insuranceRate)
        val (schemes, autoCert) = certSchemes(listOf(i.dutyKey, i.commKey))
        val certUSD = if (i.certUSD >= 0) i.certUSD else autoCert
        val clearanceUSD = i.clearanceUSD
        val domesticFixedUSD = i.domesticFixedCNY / i.fxCNYperUSD

        val landedTotalUSD = imp.cif + imp.total + certUSD + clearanceUSD + domesticFixedUSD
        val landedUnitUSD = landedTotalUSD / qty
        val landedUnitNGN = landedUnitUSD * i.fxNGNperUSD
        val landedUnitCNY = landedUnitUSD * i.fxCNYperUSD

        val commission = commCat.rate
        val proc = Data.PAYMENT_PROCESSING
        val feeVat = (commission + proc) * Data.VAT_ON_FEES
        val feeRate = commission + proc + feeVat

        val autoPrice = landedUnitNGN / (1.0 - feeRate - i.adRate - i.desiredMargin).coerceAtLeast(0.01)
        val price = if (i.targetPriceNGN > 0) i.targetPriceNGN else autoPrice

        val commissionNGN = price * commission
        val procNGN = price * proc
        val feeVatNGN = (commissionNGN + procNGN) * Data.VAT_ON_FEES
        val adNGN = price * i.adRate
        val platformCostNGN = commissionNGN + procNGN + feeVatNGN + adNGN

        val returnCostPerUnit = i.returnRate * (Data.OUTBOUND_PER_ITEM_NGN + landedUnitNGN * i.returnLossRate)
        val netProfitUnit = price - platformCostNGN - landedUnitNGN - returnCostPerUnit
        val netMargin = if (price > 0) netProfitUnit / price else -1.0

        val contribution = price - platformCostNGN - returnCostPerUnit
        val batchTotalNGN = landedTotalUSD * i.fxNGNperUSD
        val breakEvenUnits = if (contribution > 0) batchTotalNGN / contribution else Double.POSITIVE_INFINITY
        val recoverRatio = breakEvenUnits / qty

        val breakEvenPrice = (landedUnitNGN + i.returnRate * Data.OUTBOUND_PER_ITEM_NGN) /
                ((1 - i.returnRate) * (1 - feeRate - i.adRate)).coerceAtLeast(0.01)

        val cycleDays = Data.PRODUCTION_DAYS + freight.transitDays + Data.CLEARANCE_DAYS +
                Data.SELL_THROUGH_DAYS + Data.PAYOUT_DAYS
        val capitalLockedNGN = batchTotalNGN + (certUSD + clearanceUSD) * i.fxNGNperUSD

        val stack = listOf(
            CostItem("goods", "采购成本", goodsValueUSD),
            CostItem("freight", "头程运费", freightUSD),
            CostItem("insurance", "保险", goodsValueUSD * i.insuranceRate),
            CostItem("duty", "关税", imp.duty),
            CostItem("surcharge", "附加费", imp.surcharge),
            CostItem("ciss", "CISS", imp.ciss),
            CostItem("etls", "ETLS", imp.etls),
            CostItem("vat", "进口增值税", imp.vat),
            CostItem("cert", "认证", certUSD),
            CostItem("clearance", "清关杂费", clearanceUSD),
            CostItem("domestic", "国内杂费", domesticFixedUSD)
        )

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
                val rc = rr * (Data.OUTBOUND_PER_ITEM_NGN + landed2 * i.returnLossRate)
                val np = price - platformCostNGN - landed2 - rc
                SensCell(rr, np, if (price > 0) np / price else -1.0)
            }
            SensRow(s, fx2, cells)
        }

        return Result(
            unitNGN = landedUnitNGN, unitCNY = landedUnitCNY, batchNGN = batchTotalNGN,
            dutyRate = dutyCat.rate, commissionRate = commission,
            priceNGN = price, autoPriceNGN = autoPrice, breakEvenPriceNGN = breakEvenPrice,
            netProfitNGN = netProfitUnit, netMargin = netMargin,
            platformCostNGN = platformCostNGN, returnCostNGN = returnCostPerUnit,
            recoverRatio = recoverRatio, breakEvenUnits = breakEvenUnits,
            cycleDays = cycleDays, capitalLockedNGN = capitalLockedNGN,
            stack = stack, certSchemes = schemes, sensitivity = sensitivity,
            qty = qty, fxNGNperUSD = i.fxNGNperUSD, fxCNYperUSD = i.fxCNYperUSD
        )
    }
}
