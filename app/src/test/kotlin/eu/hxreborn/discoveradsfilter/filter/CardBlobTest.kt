package eu.hxreborn.discoveradsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class CardBlobTest {
    @Test
    fun `reads the headline and url that follow their labels`() {
        val blob =
            blob(
                "Card Title" to "Confirmado por la UE",
                "Card URL" to "https://www.infobae.com/a/b/",
            )

        val card = CardBlob.parse(blob)

        assertEquals("Confirmado por la UE", card.headline)
        assertEquals("infobae.com", card.source)
    }

    @Test
    fun `reads a headline longer than a single byte varint`() {
        val long =
            "Los expertos coinciden en que dormir con el aire acondicionado " +
                "gasta menos de lo que crees"
        val blob = blob("Card Title" to long)

        assertEquals(long, CardBlob.parse(blob).headline)
    }

    @Test
    fun `keeps multibyte characters intact`() {
        val blob = blob("Card Title" to "La Universidad de Zaragoza adelanta la vuelta a las aulas")

        assertEquals(
            "La Universidad de Zaragoza adelanta la vuelta a las aulas",
            CardBlob.parse(blob).headline,
        )
    }

    @Test
    fun `returns nothing when the label is absent`() {
        val card = CardBlob.parse(blob("Other Label" to "value"))

        assertNull(card.headline)
        assertNull(card.source)
        assertEquals(true, card.isEmpty)
    }

    @Test
    fun `skips a label that is not followed by a length delimited field`() {
        val truncated = "Card Title".toByteArray() + byteArrayOf(0x08, 0x01)

        assertNull(CardBlob.parse(truncated).headline)
    }

    @Test
    fun `ignores a length that runs past the end of the blob`() {
        val overrun = "Card Title".toByteArray() + byteArrayOf(0x12, 0x40) + "short".toByteArray()

        assertNull(CardBlob.parse(overrun).headline)
    }

    @Test
    fun `takes the second label when the first is malformed`() {
        val blob =
            "Card Title".toByteArray() + byteArrayOf(0x08, 0x01) +
                "Card Title".toByteArray() + field("Real headline")

        assertEquals("Real headline", CardBlob.parse(blob).headline)
    }

    @Test
    fun `strips www and the path from the source`() {
        assertEquals(
            "infobae.com",
            CardBlob.hostOf("https://www.infobae.com/tecno/2026/08/27/guia/"),
        )
        assertEquals("heraldo.es", CardBlob.hostOf("https://heraldo.es"))
        assertEquals("blog.google", CardBlob.hostOf("https://blog.google/products/gemini?x=1"))
        assertNull(CardBlob.hostOf("not a url"))
    }

    private fun blob(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x0a, 0x03, 0x70, 0x61, 0x64))
        entries.forEach { (label, value) ->
            out.write(label.toByteArray())
            out.write(field(value))
        }
        return out.toByteArray()
    }

    private fun field(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        out.write(0x12)
        var remaining = bytes.size
        while (true) {
            val byte = remaining and 0x7f
            remaining = remaining ushr 7
            if (remaining == 0) {
                out.write(byte)
                break
            }
            out.write(byte or 0x80)
        }
        out.write(bytes)
        return out.toByteArray()
    }
}
