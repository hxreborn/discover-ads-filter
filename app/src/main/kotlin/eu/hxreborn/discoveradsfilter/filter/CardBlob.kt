package eu.hxreborn.discoveradsfilter.filter

object CardBlob {
    private const val MAX_STRING_BYTES = 4096
    private val titleMarker = "Card Title".toByteArray(Charsets.UTF_8)
    private val urlMarker = "Card URL".toByteArray(Charsets.UTF_8)

    fun parse(blob: ByteArray): CardText =
        CardText(
            headline = stringAfter(blob, titleMarker),
            source = stringAfter(blob, urlMarker)?.let(::hostOf),
        )

    fun hostOf(url: String): String? {
        val scheme = url.indexOf("://")
        if (scheme < 0) return null
        val start = scheme + 3
        var end = url.length
        for (index in start until url.length) {
            val char = url[index]
            if (char == '/' || char == '?' || char == '#') {
                end = index
                break
            }
        }
        return url.substring(start, end).removePrefix("www.").takeIf { it.isNotEmpty() }
    }

    private fun stringAfter(
        blob: ByteArray,
        marker: ByteArray,
    ): String? {
        var from = 0
        while (true) {
            val at = indexOf(blob, marker, from)
            if (at < 0) return null
            val value = readLengthDelimited(blob, at + marker.size)
            if (value != null) return value
            from = at + 1
        }
    }

    private fun readLengthDelimited(
        blob: ByteArray,
        tagIndex: Int,
    ): String? {
        if (tagIndex >= blob.size) return null
        if ((blob[tagIndex].toInt() and 7) != 2) return null
        val length = readVarint(blob, tagIndex + 1) ?: return null
        if (length.value <= 0 || length.value > MAX_STRING_BYTES) return null
        val end = length.next + length.value
        if (end > blob.size || end < length.next) return null
        return String(blob, length.next, length.value, Charsets.UTF_8)
    }

    private class Varint(
        val value: Int,
        val next: Int,
    )

    private fun readVarint(
        blob: ByteArray,
        start: Int,
    ): Varint? {
        var result = 0
        var shift = 0
        var index = start
        while (index < blob.size && shift <= 28) {
            val byte = blob[index].toInt()
            result = result or ((byte and 0x7f) shl shift)
            index++
            if (byte and 0x80 == 0) return Varint(result, index)
            shift += 7
        }
        return null
    }

    private fun indexOf(
        haystack: ByteArray,
        needle: ByteArray,
        from: Int,
    ): Int {
        if (needle.isEmpty() || needle.size > haystack.size) return -1
        outer@ for (start in from..haystack.size - needle.size) {
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) continue@outer
            }
            return start
        }
        return -1
    }
}
