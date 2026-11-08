package com.landed.africa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object Palette {
    val Paper = Color(0xFFF5F1E8)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF111814)
    val Ink2 = Color(0xFF2A332E)
    val Muted = Color(0xFF5A615B)
    val Dim = Color(0xFF656C65)
    val Line = Color(0xFFE3DDD1)
    val Line2 = Color(0xFFD2CABB)
    val Amber = Color(0xFFE9A13B)
    val Amber2 = Color(0xFF8A5A12)
    val Green = Color(0xFF1C7A57)
    val Red = Color(0xFFC2412B)
    val Receipt = Color(0xFF141B17)
    val ReceiptInk = Color(0xFFF2F5F0)
    val ReceiptMuted = Color(0xFF92A298)
    val ReceiptAccent = Color(0xFFF3B04B)
    val FieldBg = Color(0xFFFCFAF4)

    fun stackColor(key: String): Color = when (key) {
        "goods" -> Color(0xFF1F4B3F)
        "freight" -> Color(0xFF3B7A63)
        "insurance" -> Color(0xFF7BA18E)
        "duty" -> Color(0xFFC57E1C)
        "surcharge" -> Color(0xFFE9A13B)
        "ciss" -> Color(0xFFF0BE6A)
        "etls" -> Color(0xFFF4D9A8)
        "idf" -> Color(0xFFD9A15A)
        "rdl" -> Color(0xFFC98A7A)
        "vat" -> Color(0xFFC2412B)
        "cert" -> Color(0xFF8C5B3F)
        "clearance" -> Color(0xFFB07C5A)
        "domestic" -> Color(0xFFD2B79E)
        else -> Color(0xFF999999)
    }
}

private val LightColors = lightColorScheme(
    primary = Palette.Amber,
    onPrimary = Color(0xFF1A1200),
    background = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.FieldBg,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Line2
)

@Composable
fun LandedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
