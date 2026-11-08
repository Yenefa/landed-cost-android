package com.landed.africa.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.text.DecimalFormat

/**
 * 分享卡渲染：把小票式结果卡画成 1080×1350 的 PNG（4:5，适合微信/小红书/推特）。
 * 全部用 android.graphics 直接绘制，不依赖 Compose 截图 API，结果稳定可复现。
 */
object ShareCard {

    const val W = 1080
    const val H = 1350

    private fun fmt(v: Double, digits: Int = 0): String {
        if (v.isNaN() || v.isInfinite()) return "—"
        val pattern = if (digits > 0) "#,##0." + "0".repeat(digits) else "#,##0"
        return DecimalFormat(pattern).format(v)
    }

    private fun pct(v: Double, digits: Int = 1): String =
        if (v.isNaN() || v.isInfinite()) "—" else fmt(v * 100, digits) + "%"

    fun render(r: Calc.Result): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#F5F1E8"))

        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        fun paint(size: Float, color: Int, tf: Typeface = Typeface.DEFAULT): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = tf
            }

        val ink = Color.parseColor("#111814")
        val muted = Color.parseColor("#5A615B")
        val receipt = Color.parseColor("#141B17")
        val receiptMuted = Color.parseColor("#92A298")
        val accent = Color.parseColor("#F3B04B")
        val green = Color.parseColor("#6FE3B6")
        val white = Color.WHITE

        // 顶部品牌
        val logoBox = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        c.drawRoundRect(RectF(64f, 64f, 148f, 148f), 22f, 22f, logoBox)
        val pLogo = paint(40f, Color.parseColor("#F5F1E8"), bold)
        c.drawText("落", 84f, 124f, pLogo)

        c.drawText("落地价", 172f, 110f, paint(38f, ink, bold))
        c.drawText("LANDED COST", 174f, 140f, paint(20f, muted, bold))

        // 市场
        val market = "${r.marketName} · ${r.currencyCode}"
        val pMarket = paint(24f, muted, bold)
        c.drawText(market, W - 64f - pMarket.measureText(market), 112f, pMarket)

        // 小票卡
        val cardTop = 200f
        val cardBottom = 1180f
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = receipt }
        c.drawRoundRect(RectF(64f, cardTop, W - 64f, cardBottom), 40f, 40f, card)

        c.drawText("单件落地成本", 112f, cardTop + 76f, paint(26f, receiptMuted, bold))

        val pSym = paint(48f, accent, bold)
        c.drawText(r.currencySymbol, 112f, cardTop + 200f, pSym)
        val symW = pSym.measureText(r.currencySymbol)
        c.drawText(fmt(r.unitNGN), 112f + symW + 12f, cardTop + 200f, paint(96f, white, bold))

        c.drawText(
            "≈ ¥${fmt(r.unitCNY, 2)} · 含关税 ${fmt(r.dutyRate * 100, 0)}% · 增值税 ${pct(r.vatRate)}",
            112f, cardTop + 248f, paint(24f, receiptMuted)
        )

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#33FFFFFF"); strokeWidth = 2f }
        c.drawLine(112f, cardTop + 292f, W - 112f, cardTop + 292f, line)

        val rows = listOf(
            Triple("建议售价", "${r.currencySymbol}${fmt(r.priceNGN)}", white),
            Triple("单件净利", "${r.currencySymbol}${fmt(r.netProfitNGN)}", if (r.netProfitNGN >= 0) green else Color.parseColor("#FF8C77")),
            Triple("净利率", pct(r.netMargin, 1), if (r.netMargin >= 0.08) green else accent),
            Triple("回本率", if (r.recoverRatio.isInfinite()) "无法回本" else pct(r.recoverRatio, 0),
                if (r.recoverRatio > 1) Color.parseColor("#FF8C77") else green),
            Triple("整批总成本", "${r.currencySymbol}${fmt(r.batchNGN)}", white),
            Triple("资金周转", "${r.cycleDays} 天", white),
        )
        var y = cardTop + 350f
        rows.forEachIndexed { idx, (label, value, vc) ->
            c.drawText(label, 112f, y, paint(26f, receiptMuted))
            val pv = paint(30f, vc, mono)
            c.drawText(value, W - 112f - pv.measureText(value), y, pv)
            if (idx < rows.lastIndex) {
                c.drawLine(112f, y + 26f, W - 112f, y + 26f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1AFFFFFF"); strokeWidth = 1.5f
                })
            }
            y += 92f
        }

        c.drawText(
            "估算值，非报价 · 所有参数可改，结果实时重算",
            112f, cardBottom - 44f, paint(22f, receiptMuted)
        )

        // 底部：三个关键数 + 免责
        val chips = listOf(
            "关税" to "${fmt(r.dutyRate * 100, 0)}%",
            "增值税" to pct(r.vatRate),
            "佣金" to pct(r.commissionRate, 0),
        )
        val chipW = (W - 128f - 2 * 24f) / 3f
        chips.forEachIndexed { idx, (label, value) ->
            val left = 64f + idx * (chipW + 24f)
            c.drawRoundRect(RectF(left, 1216f, left + chipW, 1330f), 24f, 24f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            c.drawText(value, left + 28f, 1272f, paint(34f, ink, bold))
            c.drawText(label, left + 28f, 1306f, paint(22f, muted, bold))
        }

        return bmp
    }
}
