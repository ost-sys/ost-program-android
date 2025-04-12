package com.ost.application.data.model // Или другой подходящий пакет

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel // Уровень коррекции ошибок

object QrCodeHelper {

    fun generateQrCodeWithLogoBitmap(
        content: String,
        size: Int,
        logo: Bitmap? = null,
        logoSizePercentage: Int = 20, // Логотип занимает 20% ширины/высоты
        logoMargin: Int = 6 // Небольшой отступ вокруг лого
    ): Bitmap? {
        return try {
            // Настройки для генератора QR-кода
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            // Уровень коррекции ошибок (H - самый высокий, позволяет логотипу перекрывать больше)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1 // Минимальные поля вокруг самого QR

            // Генерация матрицы QR-кода
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            // Создание Bitmap из матрицы
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565) // Используем RGB_565 для экономии памяти
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            // Если логотип не предоставлен, возвращаем чистый QR
            if (logo == null) {
                return bmp
            }

            // --- Добавление логотипа ---
            val logoTargetSize = (size * logoSizePercentage / 100f).toInt()
            val scaledLogo = scaleBitmap(logo, logoTargetSize - logoMargin * 2) ?: return bmp // Масштабируем лого с учетом отступа

            // Координаты для центрирования логотипа
            val logoX = (size - scaledLogo.width) / 2f
            val logoY = (size - scaledLogo.height) / 2f

            // Рисуем на Canvas
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Рисуем белый прямоугольник (или круг) под логотипом с отступом
            val bgRectSize = scaledLogo.width + logoMargin * 2
            val bgRectX = (size - bgRectSize) / 2f
            val bgRectY = (size - bgRectSize) / 2f
            paint.color = Color.WHITE
            // Можно сделать круглый фон:
            // canvas.drawCircle(size / 2f, size / 2f, bgRectSize / 2f, paint)
            // Или квадратный:
            canvas.drawRect(bgRectX, bgRectY, bgRectX + bgRectSize, bgRectY + bgRectSize, paint)


            // Рисуем масштабированный логотип поверх белого фона
            canvas.drawBitmap(scaledLogo, logoX, logoY, null)

            bmp // Возвращаем Bitmap с логотипом

        } catch (e: Exception) {
            e.printStackTrace()
            null // Возвращаем null при ошибке
        }
    }

    // Вспомогательная функция для масштабирования Bitmap
    private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap? {
        if (targetSize <= 0) return null
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val scaleWidth = targetSize.toFloat() / width
        val scaleHeight = targetSize.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight) // Масштабируем пропорционально

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}