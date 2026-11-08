package com.landed.africa.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PDF 导出：A4 单页报价单（595×842pt），给货代/合伙人看的正式版。
 * 内容与 App 内结果一致，末尾带估算免责声明与生成时间。
 */
object PdfExport {

    private const val W = 595
    private const val H = 842
    private const val M = 40f

    private fun fmt(v: Double, digits: Int = 0): String {
        if (v.isNaN() || v.isInfinite()) return "—"
        val pattern = if (digits > 0) "#,##0." + "0".repeat(digits) else "#,##0"
        return DecimalFormat(pattern).format(v)
    }

    private fun pct(v: Double, digits: Int = 1): String =
        if (v.isNaN() || v.isInfinite()) "—" else fmt(v * 100, digits) + "%"

    fun render(r: Calc.Result): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create())
        val c = page.canvas

        val ink = Color.parseColor("#111814")
        val muted = Color.parseColor("#5A615B")
        val line = Color.parseColor("#D2CABB")
        val accent = Color.parseColor("#8A5A12")
        val paper = Color.parseColor("#F5F1E8")
        val receipt = Color.parseColor("#141B17")

        fun paint(size: Float, color: Int, bold: Boolean = false): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            }

        // 背景
        c.drawColor(paper)

        // 品牌
        c.drawRoundRect(M, M, M + 40f, M + 40f, 10f, 10f, paint(0f, ink).apply { color = ink })
        c.drawText("落", M + 12f, M + 28f, paint(20f, Color.parseColor("#F5F1E8"), true))
        c.drawText("落地价", M + 52f, M + 22f, paint(20f, ink, true))
        c.drawText("LANDED COST", M + 53f, M + 36f, paint(8f, muted, true))

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
        val pDate = paint(8f, muted)
        c.drawText("生成 $date", W - M - pDate.measureText("生成 $date"), M + 20f, pDate)

        // 标题
        c.drawText("${r.marketName} 落地成本估算", M, M + 86f, paint(24f, ink, true))
        c.drawText(
            "关税 ${pct(r.dutyRate, 0)} · 增值税 ${pct(r.vatRate)} · 平台佣金 ${pct(r.commissionRate, 0)}",
            M, M + 108f, paint(10f, muted)
        )

        // 主结果卡
        val cardTop = M + 124f
        c.drawRoundRect(M, cardTop, W - M, cardTop + 112f, 14f, 14f, paint(0f, receipt).apply { color = receipt })
        c.drawText("单件落地成本", M + 20f, cardTop + 26f, paint(9f, Color.parseColor("#92A298"), true))
        val pSym = paint(22f, Color.parseColor("#F3B04B"), true)
        c.drawText(r.currencySymbol, M + 20f, cardTop + 72f, pSym)
        c.drawText(
            fmt(r.unitNGN),
            M + 22f + pSym.measureText(r.currencySymbol),
            cardTop + 72f,
            paint(40f, Color.WHITE, true)
        )
        c.drawText(
            "≈ ¥${fmt(r.unitCNY, 2)} · 整批 ${r.currencySymbol}${fmt(r.batchNGN)}",
            M + 20f, cardTop + 96f, paint(9f, Color.parseColor("#92A298"))
        )

        // 明细
        var y = cardTop + 150f
        val rows = listOf(
            "建议售价" to "${r.currencySymbol}${fmt(r.priceNGN)}",
            "单件净利" to "${r.currencySymbol}${fmt(r.netProfitNGN)}",
            "净利率" to pct(r.netMargin, 1),
            "回本率" to if (r.recoverRatio.isInfinite()) "无法回本" else pct(r.recoverRatio, 0),
            "资金周转" to "${r.cycleDays} 天",
            "整批总成本" to "${r.currencySymbol}${fmt(r.batchNGN)}",
            "认证 / 清关" to (r.certSchemes.joinToString(" / ").ifBlank { "—" }) + " · 已计入成本",
        )
        c.drawText("关键指标", M, y, paint(11f, ink, true))
        y += 10f
        rows.forEach { (label, value) ->
            y += 22f
            c.drawText(label, M, y, paint(10f, muted))
            val pv = paint(10f, ink, true)
            c.drawText(value, W - M - pv.measureText(value), y, pv)
            c.drawLine(M, y + 6f, W - M, y + 6f, paint(0.5f, line).apply { strokeWidth = 0.5f; color = line })
        }

        // 成本结构
        y += 34f
        c.drawText("成本结构（占落地总成本）", M, y, paint(11f, ink, true))
        y += 14f
        val total = r.stack.sumOf { it.usd }.coerceAtLeast(1e-9)
        r.stack.filter { it.usd > 0 }.forEach { item ->
            y += 16f
            c.drawText(item.label, M, y, paint(9f, muted))
            val p = pct(item.usd / total, 1)
            val pv = paint(9f, ink)
            c.drawText(p, W - M - pv.measureText(p), y, pv)
            // 简单条形
            val barW = 150f
            val left = M + 200f
            c.drawRect(left, y - 7f, left + (barW * (item.usd / total).toFloat()).coerceAtLeast(1f), y - 2f, paint(0f, accent))
        }

        // 免责声明
        val footY = H - 58f
        c.drawLine(M, footY - 18f, W - M, footY - 18f, paint(0.5f, line))
        c.drawText(
            "估算值，非报价。税率、费率、汇率以官方与合同为准；本工具不构成法律/税务意见。",
            M, footY, paint(8f, muted)
        )
        c.drawText("落地价 · 非洲跨境落地成本计算器", M, footY + 14f, paint(8f, accent, true))

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
