package com.example.util

/**
 * Pure-Kotlin standard QR Code generator (Byte mode, Error Correction Level L, Versions 1..6).
 * Zero external dependencies, works completely offline.
 */
object QrCodeGenerator {

    data class QrMatrix(val size: Int, val modules: BooleanArray) {
        fun get(x: Int, y: Int): Boolean = modules[y * size + x]
    }

    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val dataCodewords: Int,
        val alignmentCenters: IntArray
    )

    // Level L capacities (covers up to 134 UTF-8 bytes cleanly)
    private val versions = listOf(
        VersionSpec(1, 26, 7, 1, 19, intArrayOf()),
        VersionSpec(2, 44, 10, 1, 34, intArrayOf(6, 18)),
        VersionSpec(3, 70, 15, 1, 55, intArrayOf(6, 22)),
        VersionSpec(4, 100, 20, 1, 80, intArrayOf(6, 26)),
        VersionSpec(5, 134, 26, 1, 108, intArrayOf(6, 30)),
        VersionSpec(6, 172, 18, 2, 136, intArrayOf(6, 34))
    )

    fun encode(content: String): QrMatrix {
        val rawBytes = content.ifEmpty { "https://widgetlite.org" }.toByteArray(Charsets.UTF_8)
        val spec = versions.firstOrNull { rawBytes.size <= it.dataCodewords - 2 } ?: versions.last()
        val maxPayload = spec.dataCodewords - 2
        val dataBytes = if (rawBytes.size > maxPayload) rawBytes.copyOf(maxPayload) else rawBytes

        val bits = ArrayList<Int>(spec.dataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Byte mode indicator (0100) + 8-bit character count (versions 1..9)
        appendBits(0b0100, 4)
        appendBits(dataBytes.size, 8)
        for (b in dataBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val totalDataBits = spec.dataCodewords * 8
        val terminator = minOf(4, totalDataBits - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        var padToggle = true
        while (bits.size < totalDataBits) {
            appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val dataCodewords = IntArray(spec.dataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i * 8 + b]
            }
            v
        }

        // Split into blocks & compute Reed-Solomon ECC
        val blockSize = spec.dataCodewords / spec.numBlocks
        val dataBlocks = Array(spec.numBlocks) { b ->
            dataCodewords.copyOfRange(b * blockSize, (b + 1) * blockSize)
        }
        val ecBlocks = Array(spec.numBlocks) { b ->
            reedSolomonRemainder(dataBlocks[b], spec.ecCodewordsPerBlock)
        }

        val interleaved = IntArray(spec.totalCodewords)
        var idx = 0
        for (i in 0 until blockSize) {
            for (b in 0 until spec.numBlocks) {
                interleaved[idx++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (b in 0 until spec.numBlocks) {
                interleaved[idx++] = ecBlocks[b][i]
            }
        }

        val size = spec.version * 4 + 17
        val modules = BooleanArray(size * size)
        val isFunction = BooleanArray(size * size)

        fun setFunc(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until size && y in 0 until size) {
                modules[y * size + x] = dark
                isFunction[y * size + x] = true
            }
        }

        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val x = cx + dx
                    val y = cy + dy
                    if (x in 0 until size && y in 0 until size) {
                        val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        val dark = dist != 2 && dist != 4
                        setFunc(x, y, dark)
                    }
                }
            }
        }

        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunc(i, 6, i % 2 == 0)
            setFunc(6, i, i % 2 == 0)
        }

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                if (isFunction[cy * size + cx]) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        setFunc(cx + dx, cy + dy, dist != 1)
                    }
                }
            }
        }

        // Dark module + reserve format info
        setFunc(8, size - 8, true)
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunc(size - 1 - i, 8, false)
            setFunc(8, size - 1 - i, false)
        }

        // Place data bits in zig-zag
        var bitIndex = 0
        val totalCodewordBits = interleaved.size * 8
        var upward = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right--
            for (vert in 0 until size) {
                val y = if (upward) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y * size + x]) {
                        val dark = if (bitIndex < totalCodewordBits) {
                            val cw = interleaved[bitIndex ushr 3]
                            ((cw ushr (7 - (bitIndex and 7))) and 1) == 1
                        } else false
                        modules[y * size + x] = dark
                        bitIndex++
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // Apply mask 0 ((x + y) % 2 == 0) or best mask among 0..3
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestModules = modules

        for (mask in 0..7) {
            val candidate = modules.copyOf()
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (!isFunction[y * size + x] && shouldMask(mask, x, y)) {
                        candidate[y * size + x] = !candidate[y * size + x]
                    }
                }
            }
            applyFormatBits(candidate, size, mask)
            val penalty = computePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestModules = candidate
            }
        }
        applyFormatBits(bestModules, size, bestMask)
        return QrMatrix(size, bestModules)
    }

    private fun shouldMask(mask: Int, x: Int, y: Int): Boolean = when (mask) {
        0 -> (x + y) % 2 == 0
        1 -> y % 2 == 0
        2 -> x % 3 == 0
        3 -> (x + y) % 3 == 0
        4 -> ((y / 2) + (x / 3)) % 2 == 0
        5 -> (x * y) % 2 + (x * y) % 3 == 0
        6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
        else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
    }

    private fun applyFormatBits(modules: BooleanArray, size: Int, mask: Int) {
        // EC level L = 01
        val data = (0b01 shl 3) or mask
        var rem = data
        repeat(10) { rem = (rem shl 1) xor (((rem ushr 9) and 1) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) modules[i * size + 8] = ((bits ushr i) and 1) == 1
        modules[7 * size + 8] = ((bits ushr 6) and 1) == 1
        modules[8 * size + 8] = ((bits ushr 7) and 1) == 1
        modules[8 * size + 7] = ((bits ushr 8) and 1) == 1
        for (i in 9..14) modules[8 * size + (14 - i)] = ((bits ushr i) and 1) == 1

        for (i in 0..7) modules[8 * size + (size - 1 - i)] = ((bits ushr i) and 1) == 1
        for (i in 8..14) modules[(size - 15 + i) * size + 8] = ((bits ushr i) and 1) == 1
        modules[(size - 8) * size + 8] = true
    }

    private fun computePenalty(modules: BooleanArray, size: Int): Int {
        var penalty = 0
        for (y in 0 until size) {
            var runLen = 1
            for (x in 1 until size) {
                if (modules[y * size + x] == modules[y * size + x - 1]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (x in 0 until size) {
            var runLen = 1
            for (y in 1 until size) {
                if (modules[y * size + x] == modules[(y - 1) * size + x]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        return penalty
    }

    private fun reedSolomonRemainder(data: IntArray, degree: Int): IntArray {
        val gen = IntArray(degree)
        gen[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                gen[j] = gfMultiply(gen[j], root)
                if (j + 1 < degree) gen[j] = gen[j] xor gen[j + 1]
            }
            root = gfMultiply(root, 2)
        }
        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            for (j in 0 until degree) {
                result[j] = result[j] xor gfMultiply(gen[j], factor)
            }
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var a = x
        var b = y
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            if (((b ushr i) and 1) != 0) z = z xor a
        }
        return z
    }
}
