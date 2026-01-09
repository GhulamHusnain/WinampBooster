package com.gosash.winampbooster

import android.content.Context
import android.media.*
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PcmBoostPlayer(private val ctx: Context) {

    private var uri: Uri? = null
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null

    private var sampleRate = 44100
    private var channelCount = 2
    private var totalDurationUs: Long = 0L
    private var currentPosUs: Long = 0L

    private val playing = AtomicBoolean(false)
    private val loaded = AtomicBoolean(false)
    private var worker: Thread? = null

    @Volatile private var gainDb: Float = 0f // 0..12
    @Volatile private var limiterEnabled: Boolean = true

    fun load(u: Uri) {
        stop()
        releaseInternal()
        uri = u

        extractor = MediaExtractor().apply {
            ctx.contentResolver.openAssetFileDescriptor(u, "r")?.use { afd ->
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        }

        val ex = extractor ?: return
        var audioTrackIndex = -1
        var format: MediaFormat? = null

        for (i in 0 until ex.trackCount) {
            val f = ex.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = f
                break
            }
        }
        require(audioTrackIndex >= 0 && format != null) { "No audio track found." }

        ex.selectTrack(audioTrackIndex)

        val mime = format!!.getString(MediaFormat.KEY_MIME)!!
        sampleRate = format!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        channelCount = format!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        totalDurationUs = if (format!!.containsKey(MediaFormat.KEY_DURATION)) format!!.getLong(MediaFormat.KEY_DURATION) else 0L

        codec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
        }

        val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig)
                .build(),
            max(minBuf, 2 * minBuf),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        loaded.set(true)
    }

    fun play() {
        if (!loaded.get()) return
        if (playing.get()) return

        playing.set(true)
        audioTrack?.play()
        worker = Thread { decodeLoop() }.also { it.start() }
    }

    fun pause() {
        playing.set(false)
        audioTrack?.pause()
    }

    fun stop() {
        playing.set(false)
        try { worker?.join(300) } catch (_: Throwable) {}
        worker = null
        try { audioTrack?.pause() } catch (_: Throwable) {}
        try { audioTrack?.flush() } catch (_: Throwable) {}
        currentPosUs = 0L
        extractor?.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        try { codec?.flush() } catch (_: Throwable) {}
    }

    fun seekToMs(ms: Long) {
        val targetUs = ms * 1000L
        currentPosUs = targetUs
        extractor?.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        try { codec?.flush() } catch (_: Throwable) {}
        try { audioTrack?.flush() } catch (_: Throwable) {}
    }

    fun setGainDb(db: Float) { gainDb = db.coerceIn(0f, 12f) }
    fun setLimiterEnabled(enabled: Boolean) { limiterEnabled = enabled }

    fun isPlaying(): Boolean = playing.get()
    fun durationMs(): Long = totalDurationUs / 1000L
    fun positionMs(): Long = currentPosUs / 1000L

    private fun decodeLoop() {
        val ex = extractor ?: return
        val c = codec ?: return
        val at = audioTrack ?: return

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (playing.get() && !outputDone) {
            if (!inputDone) {
                val inIndex = c.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val inputBuf = c.getInputBuffer(inIndex)!!
                    val sampleSize = ex.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        c.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val presTime = ex.sampleTime
                        c.queueInputBuffer(inIndex, 0, sampleSize, presTime, 0)
                        ex.advance()
                    }
                }
            }

            val outIndex = c.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outIndex >= 0 -> {
                    val outBuf = c.getOutputBuffer(outIndex) ?: run {
                        c.releaseOutputBuffer(outIndex, false)
                        continue
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }

                    val pcm = ByteArray(bufferInfo.size)
                    outBuf.position(bufferInfo.offset)
                    outBuf.limit(bufferInfo.offset + bufferInfo.size)
                    outBuf.get(pcm)
                    outBuf.clear()

                    val processed = processPcm16(pcm)
                    at.write(processed, 0, processed.size)

                    currentPosUs = bufferInfo.presentationTimeUs
                    c.releaseOutputBuffer(outIndex, false)
                }

                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
            }
        }
    }

    private fun processPcm16(input: ByteArray): ByteArray {
        val bb = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        val samples = ShortArray(input.size / 2)
        bb.asShortBuffer().get(samples)

        val gain = 10f.pow(gainDb / 20f)

        for (i in samples.indices) {
            var x = samples[i].toInt()
            x = (x * gain).toInt()

            x = if (limiterEnabled) {
                val limit = (Short.MAX_VALUE * 0.90f).toInt()
                softClip(x, limit)
            } else {
                x.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            }

            samples[i] = x.toShort()
        }

        val out = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.asShortBuffer().put(samples)
        return out.array()
    }

    private fun softClip(xIn: Int, limit: Int): Int {
        val sign = if (xIn < 0) -1 else 1
        val ax = abs(xIn)
        if (ax <= limit) return xIn

        val range = Short.MAX_VALUE.toInt()
        val over = ax - limit
        val denom = max(1, range - limit)
        val t = over.toFloat() / denom.toFloat()
        val k = 6.0f
        val shaped = limit + ((range - limit) * (1f - kotlin.math.exp(-k * t))).toInt()
        return min(shaped, range) * sign
    }

    fun release() {
        stop()
        releaseInternal()
    }

    private fun releaseInternal() {
        try { codec?.stop() } catch (_: Throwable) {}
        try { codec?.release() } catch (_: Throwable) {}
        codec = null

        try { extractor?.release() } catch (_: Throwable) {}
        extractor = null

        try { audioTrack?.release() } catch (_: Throwable) {}
        audioTrack = null

        loaded.set(false)
    }
}
