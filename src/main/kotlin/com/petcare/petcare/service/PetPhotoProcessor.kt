package com.petcare.petcare.service

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class PetPhotoProcessor {
    init {
        ImageIO.scanForPlugins()
    }

    fun toJpeg(file: MultipartFile): ByteArray {
        if (file.isEmpty) {
            throw IllegalArgumentException("사진을 선택하세요.")
        }
        if (file.size > MAX_BYTES) {
            throw IllegalArgumentException(TOO_LARGE)
        }
        val bytes = file.bytes
        if (bytes.size > MAX_BYTES) {
            throw IllegalArgumentException(TOO_LARGE)
        }
        return reencode(bytes)
    }

    fun reencode(bytes: ByteArray): ByteArray {
        if (bytes.size > MAX_BYTES) {
            throw IllegalArgumentException(TOO_LARGE)
        }
        val kind = sniff(bytes)
        val input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            ?: throw IllegalArgumentException(UNREADABLE)
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) {
            input.close()
            throw IllegalArgumentException(UNREADABLE)
        }
        val reader = readers.next()
        try {
            reader.setInput(input, true, true)
            val format = reader.formatName.lowercase()
            if (!kind.accepts(format)) {
                throw IllegalArgumentException(BAD_TYPE)
            }
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width <= 0 || height <= 0 || width > MAX_EDGE || height > MAX_EDGE ||
                width.toLong() * height > MAX_PIXELS
            ) {
                throw IllegalArgumentException("이미지 크기가 너무 큽니다.")
            }
            val decoded = reader.read(0) ?: throw IllegalArgumentException(UNREADABLE)
            return writeJpeg(decoded)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (_: Exception) {
            throw IllegalArgumentException(UNREADABLE)
        } finally {
            reader.dispose()
            input.close()
        }
    }

    private fun writeJpeg(decoded: BufferedImage): ByteArray {
        val rgb = BufferedImage(decoded.width, decoded.height, BufferedImage.TYPE_INT_RGB)
        val graphics = rgb.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, rgb.width, rgb.height)
            graphics.drawImage(decoded, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        val output = ByteArrayOutputStream()
        if (!ImageIO.write(rgb, "jpeg", output) || output.size() < 3) {
            throw IllegalArgumentException(UNREADABLE)
        }
        return output.toByteArray()
    }

    private fun sniff(bytes: ByteArray): Kind {
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return Kind.JPEG
        }
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return Kind.PNG
        }
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return Kind.WEBP
        }
        throw IllegalArgumentException(BAD_TYPE)
    }

    private enum class Kind {
        JPEG, PNG, WEBP;

        fun accepts(format: String): Boolean = when (this) {
            JPEG -> format == "jpeg" || format == "jpg"
            PNG -> format == "png"
            WEBP -> format == "webp"
        }
    }

    companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
        const val TOO_LARGE = "사진 파일은 5MB 이하만 올릴 수 있습니다."
        const val BAD_TYPE = "JPG, PNG, WebP 파일만 올릴 수 있습니다."
        const val UNREADABLE = "이미지 파일을 열 수 없습니다."
        private const val MAX_EDGE = 8000
        private const val MAX_PIXELS = 20_000_000L
    }
}
