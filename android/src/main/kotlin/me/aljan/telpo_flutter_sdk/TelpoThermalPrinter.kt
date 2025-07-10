package me.aljan.telpo_flutter_sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.telpo.tps550.api.printer.UsbThermalPrinter
import java.util.*

class TelpoThermalPrinter(activity: TelpoFlutterSdkPlugin) {
    private val TAG = "TelpoThermalPrinter"
    private var mUsbThermalPrinter: UsbThermalPrinter? = null
    private var utils: Utils
    private val context: Context = activity.context
    private var errorResult: String? = null
    private var result: MethodChannelResultWrapper? = null

    private var noPaper = false
    private var printDataArray: ArrayList<Map<String, Any>> = ArrayList()

    // HandlerCodes
    private val NOPAPER = 3
    private val LOWBATTERY = 4
    private val PRINT = 9
    private val CANCELPROMPT = 10
    private val PRINTERR = 11
    private val OVERHEAT = 12
    private val DEVICETRANSMITDATA = 13

    // StatusCodes
    private val STATUS_OK = 0
    private val STATUS_NO_PAPER = 16
    private val STATUS_OVER_HEAT = 2 // Printer engine is overheating
    private val STATUS_OVER_FLOW = 3 // Printer's cache is full
    private val STATUS_UNKNOWN = 4

    // Exceptions
    private val NOPAPEREXCEPTION = "com.telpo.tps550.api.printer.NoPaperException"
    private val OVERHEATEXCEPTION = "com.telpo.tps550.api.printer.OverHeatException"
    private val DEVICETRANSMITDATAEXCEPTION =
        "com.telpo.tps550.api.printer.DeviceTransmitDataException"

    init {
        mUsbThermalPrinter = UsbThermalPrinter(context)
        utils = Utils()
    }

    @SuppressLint("HandlerLeak")
    inner class PrintHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                NOPAPER -> {
                    result?.error("3", "No paper, please put paper in and retry", null)
                    return
                }

                LOWBATTERY -> {
                    result?.error("4", "Low battery", null)
                    return
                }

                PRINT -> {
                    Print().start()
                }

                CANCELPROMPT -> {
                    Log.d(TAG, "Cancel", null)
                }

                OVERHEAT -> {
                    result?.error("12", "Overheat error", null)
                    return
                }

                DEVICETRANSMITDATA -> {
                    result?.error("13", "Device Transmit Data Exception", null)
                }
            }
        }
    }

    fun checkStatus(result: MethodChannelResultWrapper, lowBattery: Boolean) {
        try {
            this.result = result

            when (mUsbThermalPrinter?.checkStatus()) {
                STATUS_OK -> {
                    if (lowBattery) {
                        PrintHandler().sendMessage(
                            PrintHandler().obtainMessage(
                                LOWBATTERY,
                                1,
                                0,
                                null
                            )
                        )
                    } else {
                        result.success("STATUS_OK")
                    }
                }

                STATUS_NO_PAPER -> {
                    result.success("STATUS_NO_PAPER")
                    return
                }

                STATUS_UNKNOWN -> {
                    result.success("STATUS_UNKNOWN")
                    return
                }

                STATUS_OVER_FLOW -> {
                    result.success("STATUS_OVER_FLOW")
                    return
                }

                STATUS_OVER_HEAT -> {
                    result.success("STATUS_OVER_HEAT")
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "$e")
            result.error("CheckStatusException", "$e", null)
            return
        }
    }

    fun connect(): Boolean {
        return try {
            mUsbThermalPrinter?.start(1)
            mUsbThermalPrinter?.reset()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            result?.error("Printer Start Error", e.message, e.stackTrace)
            false
        }
    }

    fun disconnect(): Boolean {
        return try {
            mUsbThermalPrinter?.reset()
            mUsbThermalPrinter?.stop()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            result?.error("Printer Disconnect Error", e.message, e.stackTrace)
            false
        }
    }

    fun print(
        result: MethodChannelResultWrapper,
        printDataArray: ArrayList<Map<String, Any>>,
        lowBattery: Boolean
    ) {
        this.printDataArray = printDataArray
        this.result = result

        if (lowBattery) {
            PrintHandler().sendMessage(PrintHandler().obtainMessage(LOWBATTERY, 1, 0, null))
        }
        //
        else {
            if (noPaper) {
                PrintHandler().sendMessage(PrintHandler().obtainMessage(NOPAPER, 1, 0, null))
            } else {
                PrintHandler().sendMessage(PrintHandler().obtainMessage(PRINT, 1, 0, null))
            }
        }
    }

    private inner class Print : Thread() {
        override fun run() {
            super.run()
            try {
                mUsbThermalPrinter?.reset()
                mUsbThermalPrinter?.setMonoSpace(true);
                mUsbThermalPrinter?.setGray(7);

                for (data in printDataArray) {
                    val type = utils.getPrintType(data["type"].toString())

                    when (type) {
                        PrintType.Text -> {
                            printText(data)
                        }

                        PrintType.EscPos -> {
                            printEscPos(data)
                        }

                        PrintType.Byte -> {
                            printImage(data)
                        }

                        PrintType.QR -> {
                            printQr(data)
                        }

                        PrintType.PDF -> {}
                        PrintType.WalkPaper -> {
                            val step = data["data"].toString().toIntOrNull() ?: 2

                            mUsbThermalPrinter?.walkPaper(step)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorResult = e.toString()

                if (errorResult.equals(NOPAPEREXCEPTION)) {
                    noPaper = true
                }
                //
                else if (errorResult.equals(DEVICETRANSMITDATAEXCEPTION)) {
                    PrintHandler().sendMessage(
                        PrintHandler().obtainMessage(
                            DEVICETRANSMITDATA,
                            1,
                            0,
                            null
                        )
                    )
                }
                //
                else if (errorResult.equals(OVERHEATEXCEPTION)) {
                    PrintHandler().sendMessage(PrintHandler().obtainMessage(OVERHEAT, 1, 0, null))
                }
                //
                else {
                    PrintHandler().sendMessage(PrintHandler().obtainMessage(PRINTERR, 1, 0, null))
                }
            } finally {
                PrintHandler().sendMessage(PrintHandler().obtainMessage(CANCELPROMPT, 1, 0, null))

                if (noPaper) {
                    PrintHandler().sendMessage(PrintHandler().obtainMessage(NOPAPER, 1, 0, null))
                    noPaper = false
                }
                //
                else {
                    mUsbThermalPrinter?.stop()
                }
            }
        }
    }

    private fun printText(data: Map<String, Any>) {
        val text = data["data"].toString()
        val alignment = utils.getAlignment(data["alignment"].toString())
        val fontSize = utils.getFontSize(data["fontSize"].toString())
        val isBold = utils.getIsBold(data["isBold"].toString())

        mUsbThermalPrinter?.setTextSize(fontSize)
        mUsbThermalPrinter?.setAlgin(alignment)
        mUsbThermalPrinter?.setBold(isBold)
        mUsbThermalPrinter?.addString(text)
        mUsbThermalPrinter?.printString()

        result?.success(true)
        return
    }

    private fun printImage(data: Map<String, Any>) {
        val value = data["data"] as ArrayList<*>

        val alignment = utils.getAlignment(data["alignment"].toString())
        val width = utils.getWidth(data["width"].toString()) ?: 220

        mUsbThermalPrinter?.setAlgin(alignment)

        for (bitmap in value) {
            val bytes = bitmap as ByteArray;
            mUsbThermalPrinter?.printLogoRaw(bytes, width.toInt(), bytes.size, false)
        }

        result?.success(true)
        return
    }

    private fun printQr(data: Map<String, Any>) {
        val text = data["data"].toString()
        val alignment = utils.getAlignment(data["alignment"].toString())
        val width = utils.getWidth(data["width"].toString())?:220;

        mUsbThermalPrinter?.setAlgin(alignment)

        val qrImage = CreateCode(text, BarcodeFormat.QR_CODE, width.toInt(), width.toInt());
        mUsbThermalPrinter?.printLogo(qrImage, false)

        result?.success(true)
        return
    }

    private fun printEscPos(data: Map<String, Any>) {
        val value = data["data"] as ArrayList<*>;

        for (item in value) {
            mUsbThermalPrinter?.EscPosCommandExe(item as ByteArray)
        }

        result?.success(true)
        return
    }

    @Throws(WriterException::class)
    fun CreateCode(str: String?, type: BarcodeFormat?, bmpWidth: Int, bmpHeight: Int): Bitmap? {
        val mHashtable = Hashtable<EncodeHintType, String?>()
        mHashtable[EncodeHintType.CHARACTER_SET] = "UTF-8"
        // 生成二维矩阵,编码时要指定大小,不要生成了图片以后再进行缩放,以防模糊导致识别失败
        val matrix = MultiFormatWriter().encode(str, type, bmpWidth, bmpHeight, mHashtable)
        val width = matrix.width
        val height = matrix.height
        // 二维矩阵转为一维像素数组（一直横着排）
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix[x, y]) {
                    pixels[y * width + x] = -0x1000000
                } else {
                    pixels[y * width + x] = -0x1
                }
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // 通过像素数组生成bitmap,具体参考api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun printByte(data: Map<String, Any>) {
        val value = data["data"] as ArrayList<*>

        for (bitmap in value) {
            val bmp = utils.createByteImage(bitmap as ByteArray)

            mUsbThermalPrinter?.printLogo(bmp, false)
        }

        mUsbThermalPrinter?.printString()
        result?.success(true)
        return
    }
}