package eu.hxreborn.discoveradsfilter.util

import android.fake.FrameworkLookalike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectStringsTest {
    class Node(
        @JvmField val name: String? = null,
        @JvmField val child: Any? = null,
    )

    private fun collect(
        root: Any,
        depth: Int = 6,
        nodes: Int = 500,
        prefix: String? = null,
    ) = ObjectStrings.collect(listOf(root), depth, nodes, prefix)

    @Test
    fun `collects nested strings`() {
        val root = Node("CARD::a", Node("other", Node("CARD::b")))

        assertEquals(listOf("CARD::a", "other", "CARD::b"), collect(root))
    }

    @Test
    fun `prefix keeps only matching strings`() {
        val root = Node("CARD::a", Node("other", Node("CARD::b")))

        assertEquals(listOf("CARD::a", "CARD::b"), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `walks into lists`() {
        val root = Node(child = listOf(Node("CARD::one"), Node("CARD::two")))

        assertEquals(listOf("CARD::one", "CARD::two"), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `walks into maps`() {
        val root = Node(child = mapOf("k" to Node("CARD::mapped")))

        assertEquals(listOf("CARD::mapped"), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `walks into object arrays`() {
        val root = Node(child = arrayOf(Node("CARD::boxed")))

        assertEquals(listOf("CARD::boxed"), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `ignores primitive arrays`() {
        val root = Node("CARD::kept", intArrayOf(1, 2, 3))

        assertEquals(listOf("CARD::kept"), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `does not descend into framework classes`() {
        val root = Node(child = FrameworkLookalike("CARD::framework"))

        assertEquals(emptyList<String>(), collect(root, prefix = "CARD::"))
    }

    @Test
    fun `stops at max depth`() {
        val root = Node(child = Node(child = Node("CARD::deep")))

        assertEquals(emptyList<String>(), collect(root, depth = 2, prefix = "CARD::"))
        assertEquals(listOf("CARD::deep"), collect(root, depth = 6, prefix = "CARD::"))
    }

    @Test
    fun `stops at the node budget`() {
        val root = Node(child = Node(child = Node(child = Node("CARD::late"))))

        assertEquals(emptyList<String>(), collect(root, nodes = 2, prefix = "CARD::"))
    }

    @Test
    fun `a reference cycle terminates`() {
        val inner = Node("CARD::cycle")
        val outer = Node(child = inner)
        Node::class.java
            .getDeclaredField("child")
            .apply { isAccessible = true }
            .set(inner, outer)

        assertTrue(collect(outer, prefix = "CARD::").contains("CARD::cycle"))
    }

    @Test
    fun `deduplicates repeated strings`() {
        val shared = "CARD::same"
        val root = Node(child = listOf(Node(shared), Node(shared)))

        assertEquals(listOf("CARD::same"), collect(root, prefix = "CARD::"))
    }
}
