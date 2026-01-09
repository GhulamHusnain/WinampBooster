package com.gosash.winampbooster

import kotlin.math.roundToInt

/**
 * PCM booster utilities.
 * This file is written to compile cleanly on GitHub Actions without experimental Kotlin flags.
 * It also includes "alias" method names so older code references won't break compilation.
 */
class PcmBoostPlayer {

    // ---- Core implementations ----

    fun boost16(input: ShortArray, boostFactor: Float): ShortArray {
        val out = ShortArray(input.size)
        for (i in input.indices) {
            val v = (input[i].toInt() * boostFactor).roundToInt()
            out[i] = clamp16(v)
        }
        return out
    }

    fun boost8(input: ByteArray, boostFactor: Float): ByteArray {
        val out = ByteArray(input.size)
        for (i in input.indices) {
            val v = (input[i].toInt() * boostFactor).roundToInt()
            out[i] = clamp8(v)
        }
        return out
    }

    fun limit16(input: ShortArray, maxLevel: Short): ShortArray {
        val out = ShortArray(input.size)
        val max = maxLevel.toInt()
        for (i in input.indices) {
            val v = input[i].toInt()
            out[i] = when {
                v > max -> maxLevel
                v < -max -> (-max).toShort()
                else -> input[i]
            }
        }
        return out
    }

    // ---- Compatibility / alias methods (so other files compile) ----
    // If your app calls any of these older names, they will still work.

    fun boost(input: ShortArray, factor: Float): ShortArray = boost16(input, factor)
    fun boost(input: ByteArray, factor: Float): ByteArray = boost8(input, factor)

    fun applyBoost(input: ShortArray, factor: Float): ShortArray = boost16(input, factor)
    fun applyBoost(input: ByteArray, factor: Float): ByteArray = boost8(input, factor)

    fun boostPcm16(input: ShortArray, factor: Float): ShortArray = boost16(input, factor)
    fun boostPcm8(input: ByteArray, factor: Float): ByteArray = boost8(input, factor)

    fun processPcm16(input: ShortArray, factor: Float): ShortArray = boost16(input, factor)
    fun processPcm8(input: ByteArray, factor: Float): ByteArray = boost8(input, factor)

    fun applyLimiter(input: ShortArray, maxLevel: Short): ShortArray = limit16(input, maxLevel)

    // ---- Helpers ----

    private fun clamp16(v: Int): Short {
        return when {
            v > Short.MAX_VALUE.toInt() -> Short.MAX_VALUE
            v < Short.MIN_VALUE.toInt() -> Short.MIN_VALUE
            else -> v.toShort()
        }
    }

    private fun clamp8(v: Int): Byte {
        return when {
            v > Byte.MAX_VALUE.toInt() -> Byte.MAX_VALUE
            v < Byte.MIN_VALUE.toInt() -> Byte.MIN_VALUE
            else -> v.toByte()
        }
    }

    companion object {
        // Static-style helpers in case code calls PcmBoostPlayer.boostPcm16(...)
        fun boostPcm16(input: ShortArray, factor: Float): ShortArray = PcmBoostPlayer().boost16(input, factor)
        fun boostPcm8(input: ByteArray, factor: Float): ByteArray = PcmBoostPlayer().boost8(input, factor)
        fun limit(input: ShortArray, maxLevel: Short): ShortArray = PcmBoostPlayer().limit16(input, maxLevel)
    }
}
