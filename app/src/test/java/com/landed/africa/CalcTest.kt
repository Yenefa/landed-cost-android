package com.landed.africa

import com.landed.africa.engine.Calc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcTest {

    @Test
    fun importFormula_isCorrect() {
        val c = Calc.computeImport(1000.0, 0.0, 0.20, 0.0)
        assertEquals(1000.0, c.cif, 1e-6)
        assertEquals(200.0, c.duty, 1e-6)
        assertEquals(14.0, c.surcharge, 1e-6)
        assertEquals(10.0, c.ciss, 1e-6)
        assertEquals(5.0, c.etls, 1e-6)
        assertEquals(92.175, c.vat, 1e-6)
        assertEquals(321.175, c.total, 1e-6)
    }

    @Test
    fun defaults_areSane() {
        val r = Calc.compute(Calc.Inputs())
        assertTrue(r.unitNGN > 0)
        assertTrue(r.netProfitNGN > 0)
        assertTrue(r.recoverRatio < 1.0)
        assertEquals(r.unitNGN / r.fxNGNperUSD, r.unitCNY / r.fxCNYperUSD, 1e-6)
        assertEquals(3, r.sensitivity.size)
        assertEquals(3, r.sensitivity[0].cells.size)
    }

    @Test
    fun higherPrice_meansHigherProfit() {
        val lo = Calc.compute(Calc.Inputs(targetPriceNGN = 12000.0))
        val hi = Calc.compute(Calc.Inputs(targetPriceNGN = 20000.0))
        assertTrue(hi.netProfitNGN > lo.netProfitNGN)
    }

    @Test
    fun higherReturns_meansHigherReturnCost() {
        val low = Calc.compute(Calc.Inputs(returnRate = 0.05))
        val high = Calc.compute(Calc.Inputs(returnRate = 0.30))
        assertTrue(high.returnCostNGN > low.returnCostNGN)
    }

    @Test
    fun devaluation_increasesLandedNaira() {
        val base = Calc.compute(Calc.Inputs(fxNGNperUSD = 1500.0))
        val deval = Calc.compute(Calc.Inputs(fxNGNperUSD = 1800.0))
        assertTrue(deval.unitNGN > base.unitNGN)
    }

    @Test
    fun certification_schemesDetected() {
        val e = Calc.compute(Calc.Inputs(dutyKey = "electronics", commKey = "electronics"))
        assertTrue(e.certSchemes.contains("SONCAP"))
        val c = Calc.compute(Calc.Inputs(dutyKey = "cosmetics", commKey = "beauty"))
        assertTrue(c.certSchemes.contains("NAFDAC"))
    }

    @Test
    fun allMarkets_produceSaneLocalCurrencyResult() {
        for (m in Calc.Markets.all) {
            val r = Calc.compute(
                Calc.Inputs(
                    market = m,
                    fxNGNperUSD = m.fxLocalPerUSD,
                    certUSD = m.defaultCertUSD,
                    clearanceUSD = m.defaultClearanceUSD,
                )
            )
            assertTrue("${m.id} 落地成本应 > 0", r.unitNGN > 0)
            assertTrue("${m.id} 净利润应 > 0", r.netProfitNGN > 0)
            assertEquals(m.currencyCode, r.currencyCode)
            assertEquals(m.currencySymbol, r.currencySymbol)
            assertEquals(m.id, r.marketId)
        }
    }

    @Test
    fun kenya_usesIdfRdlAndVat16() {
        val m = Calc.Markets.KENYA
        val c = Calc.computeImport(1000.0, 0.0, 0.25, 0.0, m)
        assertEquals(35.0, c.idf, 1e-6)
        assertEquals(20.0, c.rdl, 1e-6)
        assertEquals(0.0, c.ciss, 1e-6)
        assertEquals((1000.0 + 250.0 + 35.0 + 20.0) * 0.16, c.vat, 1e-6)
    }

    @Test
    fun southAfrica_usesVat15AndNoNigeriaLevies() {
        val m = Calc.Markets.SOUTH_AFRICA
        val c = Calc.computeImport(1000.0, 0.0, 0.20, 0.0, m)
        assertEquals(0.0, c.idf, 1e-6)
        assertEquals(0.0, c.rdl, 1e-6)
        assertEquals(0.0, c.ciss, 1e-6)
        assertEquals((1000.0 + 200.0) * 0.15, c.vat, 1e-6)
    }
}
