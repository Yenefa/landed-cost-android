package com.landed.africa

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun textOf(tag: String): String =
        rule.onNodeWithTag(tag).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text } ?: ""

    @Test
    fun defaultResults_render() {
        rule.onNodeWithTag("result-landed").assertIsDisplayed().assertTextContains("15,921", substring = true)
        rule.onNodeWithTag("result-price").assertTextContains("23,786", substring = true)
        rule.onNodeWithTag("result-be").assertTextContains("81%", substring = true)
    }

    @Test
    fun editingUnitCost_recalculatesLive() {
        val before = textOf("result-landed")
        rule.onNodeWithTag("采购单价").performScrollTo().performClick()
        rule.onNodeWithTag("采购单价").performTextClearance()
        rule.onNodeWithTag("采购单价").performTextInput("200")
        rule.waitForIdle()
        val after = textOf("result-landed")
        assertNotEquals("改采购单价后落地成本应变化", before, after)
        assertTrue("落地成本应明显变大: $before -> $after", after.length >= before.length)
    }

    @Test
    fun changingDutyCategory_recalculates() {
        val before = textOf("result-landed")
        rule.onNodeWithTag("品类（决定进口关税）").performScrollTo().performClick()
        rule.onNodeWithText("服装鞋帽 · 20%").performClick()
        rule.waitForIdle()
        assertNotEquals("关税 10%→20% 后落地成本应变化", before, textOf("result-landed"))
    }

    @Test
    fun advancedPanel_expands() {
        rule.onNodeWithText("数据来源与假设").performScrollTo()
        rule.onNodeWithTag("advanced-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("认证费").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("国内杂费").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun touchTargets_areAtLeast44dp() {
        rule.onNodeWithTag("采购单价").performScrollTo().assertHeightIsAtLeast(44.dp)
        rule.onNodeWithTag("advanced-toggle").performScrollTo().assertHeightIsAtLeast(44.dp)
        rule.onNodeWithTag("advanced-toggle").performClick()
        rule.onNodeWithTag("btn-reset").performScrollTo().assertHeightIsAtLeast(44.dp)
    }

    @Test
    fun fields_haveAccessibleLabel() {
        rule.onNodeWithTag("采购单价").performScrollTo()
            .assertContentDescriptionEquals("采购单价")
        rule.onNodeWithTag("采购数量").assertContentDescriptionEquals("采购数量")
    }

    @Test
    fun switchingMarket_changesCurrency() {
        rule.onNodeWithTag("market-switcher").performClick()
        rule.onNodeWithTag("market-KE").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("currency-symbol").assertTextContains("KSh", substring = true)
        rule.onNodeWithTag("result-landed").assertIsDisplayed()

        rule.onNodeWithTag("market-switcher").performClick()
        rule.onNodeWithTag("market-ZA").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("currency-symbol").assertTextContains("R", substring = true)
    }

    @Test
    fun shareCard_rendersValidBitmap() {
        val r = com.landed.africa.engine.Calc.compute(com.landed.africa.engine.Calc.Inputs())
        val bmp = com.landed.africa.engine.ShareCard.render(r)
        org.junit.Assert.assertEquals(1080, bmp.width)
        org.junit.Assert.assertEquals(1350, bmp.height)
        // 左上角应为纸色底、中部应为小票深色，证明真的画上了内容
        val paper = bmp.getPixel(10, 10)
        org.junit.Assert.assertEquals(0xF5, android.graphics.Color.red(paper))
        val receipt = bmp.getPixel(540, 700)
        org.junit.Assert.assertTrue("小票卡应为深色", android.graphics.Color.red(receipt) < 60)
        bmp.recycle()
    }

    @Test
    fun shareButton_existsAndIsAccessible() {
        rule.onNodeWithTag("btn-share").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(44.dp)
            .assertContentDescriptionEquals("分享结果卡片")
    }

    @Test
    fun canScrollToBottom_withoutCrash() {
        rule.onNodeWithTag("cost-bar").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("risk-list").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("数据来源与假设").performScrollTo().assertIsDisplayed()
    }
}

