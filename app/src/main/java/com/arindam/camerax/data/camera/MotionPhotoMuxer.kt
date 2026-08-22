package com.arindam.camerax.data.camera

import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Muxes a still JPEG and an MP4 clip into an Android Motion Photo 1.0 file
 * (JPEG primary + XMP + appended video).
 */
object MotionPhotoMuxer {

    fun mux(stillJpeg: File, videoMp4: File, output: File): File {
        val jpeg = stillJpeg.readBytes()
        val video = videoMp4.readBytes()
        require(jpeg.size >= 4 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte()) {
            "Still image is not a JPEG"
        }
        val xmp = xmpPacket(video.size)
        output.writeBytes(insertXmp(jpeg, xmp) + video)
        return output
    }

    fun isMotionPhoto(file: File): Boolean {
        if (!file.extension.equals("jpg", ignoreCase = true) &&
            !file.extension.equals("jpeg", ignoreCase = true)
        ) return false
        val headerSize = minOf(file.length(), 64_000L).toInt()
        if (headerSize <= 0) return false
        val header = ByteArray(headerSize)
        file.inputStream().use { input ->
            var read = 0
            while (read < headerSize) {
                val count = input.read(header, read, headerSize - read)
                if (count <= 0) break
                read += count
            }
        }
        // Require Motion Photo XMP. Trailing bytes after EOI alone are not enough —
        // JPEG Ultra HDR stores a gain map after the primary image and would false-positive.
        if (!String(header, Charsets.ISO_8859_1).contains("Camera:MotionPhoto")) return false
        val video = videoOffset(file) ?: return false
        return video < file.length()
    }

    fun extractVideo(file: File, output: File): File? {
        val offset = videoOffset(file) ?: return null
        file.inputStream().use { input ->
            input.skip(offset)
            output.outputStream().use { input.copyTo(it) }
        }
        return output.takeIf { it.length() > 0 }
    }

    /**
     * Offset of the MP4 appended after the JPEG EOI. Streams the file so a large motion photo
     * cannot OOM the process.
     */
    internal fun videoOffset(file: File): Long? {
        val eoi = jpegEndOffset(file) ?: return null
        if (file.length() - eoi < 8) return null
        return eoi
    }

    private fun jpegEndOffset(file: File): Long? {
        file.inputStream().buffered(64 * 1024).use { input ->
            if (input.read() != 0xFF || input.read() != 0xD8) return null
            var offset = 2L
            while (true) {
                var value = input.read()
                if (value < 0) return null
                offset++
                if (value != 0xFF) continue
                do {
                    value = input.read()
                    if (value < 0) return null
                    offset++
                } while (value == 0xFF)
                when (value) {
                    0xD9 -> return offset
                    0xDA -> {
                        var previous = -1
                        while (true) {
                            val current = input.read()
                            if (current < 0) return null
                            offset++
                            if (previous == 0xFF && current == 0xD9) return offset
                            previous = current
                        }
                    }
                    0x00, 0x01 -> continue
                    in 0xD0..0xD7 -> continue
                    else -> {
                        val lengthHi = input.read()
                        val lengthLo = input.read()
                        if (lengthHi < 0 || lengthLo < 0) return null
                        offset += 2
                        val payload = ((lengthHi shl 8) or lengthLo) - 2
                        if (payload < 0) return null
                        var remaining = payload.toLong()
                        while (remaining > 0) {
                            val skipped = input.skip(remaining)
                            if (skipped <= 0) return null
                            remaining -= skipped
                            offset += skipped
                        }
                    }
                }
            }
        }
    }

    private fun insertXmp(jpeg: ByteArray, xmpXml: String): ByteArray {
        val payload = ByteArrayOutputStream().apply {
            write("http://ns.adobe.com/xap/1.0/".toByteArray())
            write(0)
            write(xmpXml.toByteArray())
        }.toByteArray()
        val segment = ByteArray(4 + payload.size)
        segment[0] = 0xFF.toByte()
        segment[1] = 0xE1.toByte()
        val length = payload.size + 2
        segment[2] = (length shr 8).toByte()
        segment[3] = (length and 0xFF).toByte()
        System.arraycopy(payload, 0, segment, 4, payload.size)
        val insertAt = xmpInsertOffset(jpeg)
        return jpeg.copyOfRange(0, insertAt) + segment + jpeg.copyOfRange(insertAt, jpeg.size)
    }

    private fun xmpInsertOffset(jpeg: ByteArray): Int {
        var index = 2
        while (index + 4 < jpeg.size && jpeg[index] == 0xFF.toByte()) {
            val marker = jpeg[index + 1].toInt() and 0xFF
            if (marker == 0xDA || marker == 0xD9) return index
            if (marker == 0x01 || marker in 0xD0..0xD7) {
                index += 2
                continue
            }
            val segmentLength =
                ((jpeg[index + 2].toInt() and 0xFF) shl 8) or (jpeg[index + 3].toInt() and 0xFF)
            val next = index + 2 + segmentLength
            if (marker != 0xE0 && marker != 0xE1) return index
            index = next
        }
        return index
    }

    private fun xmpPacket(videoLength: Int): String = """
        <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
        <x:xmpmeta xmlns:x="adobe:ns:meta/">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
              xmlns:Camera="http://ns.google.com/photos/1.0/camera/"
              xmlns:Container="http://ns.google.com/photos/1.0/container/"
              xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
              Camera:MotionPhoto="1"
              Camera:MotionPhotoVersion="1"
              Camera:MotionPhotoPresentationTimestampUs="0">
              <Container:Directory>
                <rdf:Seq>
                  <rdf:li rdf:parseType="Resource">
                    <Container:Item Item:Semantic="Primary" Item:Mime="image/jpeg" Item:Length="0" Item:Padding="0"/>
                  </rdf:li>
                  <rdf:li rdf:parseType="Resource">
                    <Container:Item Item:Semantic="MotionPhoto" Item:Mime="video/mp4" Item:Length="$videoLength"/>
                  </rdf:li>
                </rdf:Seq>
              </Container:Directory>
            </rdf:Description>
          </rdf:RDF>
        </x:xmpmeta>
        <?xpacket end="w"?>
    """.trimIndent()
}
