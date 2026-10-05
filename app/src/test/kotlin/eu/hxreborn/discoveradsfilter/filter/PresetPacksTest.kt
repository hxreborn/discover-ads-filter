package eu.hxreborn.discoveradsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PresetPacksTest {
    private fun pack(name: String): List<NewsRule> {
        val file = File("../presets/$name")
        assertTrue("missing ${file.absolutePath}", file.isFile)
        return NewsRules.decode(file.readText())
    }

    private fun assertAllUsable(rules: List<NewsRule>) {
        val compiled = NewsFilter.compile(rules)
        assertEquals(rules.count { it.enabled }, compiled.size)
    }

    @Test
    fun `english pack decodes and compiles`() {
        val rules = pack("clickbait-en.json")
        assertTrue(rules.isNotEmpty())
        assertAllUsable(rules)
    }

    @Test
    fun `spanish pack decodes and compiles`() {
        val rules = pack("clickbait-es.json")
        assertTrue(rules.isNotEmpty())
        assertAllUsable(rules)
    }

    @Test
    fun `spanish pack matches unaccented headlines`() {
        val compiled = NewsFilter.compile(pack("clickbait-es.json"))
        val cards =
            listOf(
                CardText("Adios a las bombillas halogenas", "infobae.com"),
                CardText("Atencion: cambia la normativa", "tododisca.com"),
                CardText("La DGT confirma la nueva norma", "motor.es"),
                CardText("Maria Perez, experta en nutricion, lo tiene claro", "ok.diario"),
            )
        cards.forEach {
            assertTrue(
                "expected hidden: ${it.headline}",
                NewsFilter.shouldHide(compiled, it),
            )
        }
    }

    @Test
    fun `german pack decodes and compiles`() {
        val rules = pack("clickbait-de.json")
        assertTrue(rules.isNotEmpty())
        assertAllUsable(rules)
    }

    @Test
    fun `german pack matches common headline shapes`() {
        val compiled = NewsFilter.compile(pack("clickbait-de.json"))
        val cards =
            listOf(
                CardText("7 Dinge, die Sie nie tun sollten", "chip.de"),
                CardText("Und es ist nicht, was du denkst", "t-online.de"),
                CardText("Mit diesem geheimen Trick sparen Sie Strom", "focus.de"),
                CardText("Für 50 € bekommen Sie das", "bild.de"),
            )
        cards.forEach {
            assertTrue(
                "expected hidden: ${it.headline}",
                NewsFilter.shouldHide(compiled, it),
            )
        }
    }
}
