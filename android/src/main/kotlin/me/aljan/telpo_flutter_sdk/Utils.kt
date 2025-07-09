package me.aljan.telpo_flutter_sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.telpo.tps550.api.printer.UsbThermalPrinter

enum class PrintType {
    Byte, Text, QR, PDF, WalkPaper, EscPos
}

class Utils {
    fun createByteImage(bytes: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun getAlignment(alignment: String?): Int {
        return when (alignment) {
            "left" -> {
                UsbThermalPrinter.ALGIN_LEFT
            }
            "center" -> {
                UsbThermalPrinter.ALGIN_MIDDLE
            }
            "right" -> {
                UsbThermalPrinter.ALGIN_RIGHT
            }
            else -> {
                UsbThermalPrinter.ALGIN_LEFT
            }
        }
    }

    fun getFontSize(fontSize: String): Int {
        return when (fontSize) {
            "size18" -> 18
            "size20" -> 20
            "size24" -> 24
            "size34" -> 34
            "size40" -> 40
            "size44" -> 44
            "size54" -> 54
            "size64" -> 64
            else -> 18
        }
    }

    fun getPrintType(type: String): PrintType {
        return when (type) {
            "byte" -> {
                PrintType.Byte
            }
            "text" -> {
                PrintType.Text
            }
            "qr" -> {
                PrintType.QR
            }
            "escpos" -> {
                PrintType.EscPos
            }
            "pdf" -> {
                PrintType.PDF
            }
            "walkpaper" -> {
                PrintType.WalkPaper
            }
            else -> {
                PrintType.WalkPaper
            }
        }
    }

    fun getIsBold(isBold: String): Boolean {
        return when (isBold) {
            "true" -> {
                true
            }
            else -> {
                false
            }
        }
    }

    fun getWidth(width: String?): Double? {
        if(width.isNullOrEmpty()) return null
        return width.toDoubleOrNull()
    }

}