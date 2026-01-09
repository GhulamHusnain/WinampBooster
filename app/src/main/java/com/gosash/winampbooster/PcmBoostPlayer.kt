package com.gosash.winampbooster

import kotlin.math.roundToInt

class PcmBoostPlayer {

    /**
     * Boost PCM 16-bit audio samples safely
     * @param input ShortArray PCM audio
     * @param boostFactor Float volume multiplier (1.0 = normal, 2.0 = louder)
     */
    fun boost(input: ShortArray, boostFactor: Float): ShortArray {
        val output = ShortArray(input.size)

        for (i in input.indices) {
            val boosted = (input[i] * boostFactor).roundToInt()

            // Clamp to 16-bit PCM range
            output[i] = when {
                boosted > Short.MAX_VALUE -> Short.MAX_VALUE
                boosted < Short.MIN_VALUE -> Short.MIN_VALUE
                else -> boosted.toShort()
            }
        }

        return output
    }

    /**
     * Boost PCM 8-bit audio samples safely
     */
    fun boost8Bit(input: ByteArray, boostFactor: Float): ByteArray {
        val output = ByteArray(input.size)

        for (i in input.indices) {
            val sample = input[i].toInt()
            val boosted = (sample * boostFactor).roundToInt()

            output[i] = when {
                boosted > Byte.MAX_VALUE -> Byte.MAX_VALUE
                boosted < Byte.MIN_VALUE -> Byte.MIN_VALUE
                else -> boosted.toByte()
            }
        }

        return output
    }

    /**
     * Simple limiter (optional)
     */
    fun limit(input: ShortArray, maxLevel: Short): ShortArray {
        val output = ShortArray(input.size)

        for (i in input.indices) {
            val s = input[i]
            output[i] = when {
                s > maxLevel -> maxLevel
                s < -maxLevel -> (-maxLevel).toShort()
                else -> s
            }
        }

        return output
    }
}
