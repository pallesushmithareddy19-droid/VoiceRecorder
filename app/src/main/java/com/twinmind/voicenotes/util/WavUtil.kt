package com.twinmind.voicenotes.util

import okio.buffer
import okio.sink
import java.io.File

object WavUtil {
    fun pcmToWav(pcm: File, wav: File, sampleRate: Int, channels: Int) {
        val pcmBytes = if (pcm.exists()) pcm.readBytes() else ByteArray(0)
        val dataLen = pcmBytes.size
        val totalDataLen = dataLen + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        fun putStr(off: Int, s: String) { s.toByteArray().copyInto(header, off) }
        fun putIntLE(off: Int, v: Int) {
            header[off] = (v and 0xff).toByte()
            header[off+1] = ((v shr 8) and 0xff).toByte()
            header[off+2] = ((v shr 16) and 0xff).toByte()
            header[off+3] = ((v shr 24) and 0xff).toByte()
        }
        fun putShortLE(off: Int, v: Int) {
            header[off] = (v and 0xff).toByte()
            header[off+1] = ((v shr 8) and 0xff).toByte()
        }

        putStr(0,"RIFF"); putIntLE(4, totalDataLen); putStr(8,"WAVE"); putStr(12,"fmt ")
        putIntLE(16,16); putShortLE(20,1); putShortLE(22,channels); putIntLE(24,sampleRate)
        putIntLE(28,byteRate); putShortLE(32,channels*2); putShortLE(34,16); putStr(36,"data"); putIntLE(40,dataLen)

        wav.sink().buffer().use { sink ->
            sink.write(header)
            if (dataLen > 0) sink.write(pcmBytes)
        }
    }
}
