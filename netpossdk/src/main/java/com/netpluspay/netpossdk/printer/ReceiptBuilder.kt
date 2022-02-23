package com.netpluspay.netpossdk.printer

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.netpluspay.netpossdk.core.AndroidTerminalReceiptBuilderFactory
import com.netpluspay.netpossdk.errors.POSPrinterException
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.printer.models.BitmapPrintLine
import com.pos.sdk.printer.models.PrintLine
import com.pos.sdk.printer.models.TextPrintLine
import io.reactivex.Single
import java.util.concurrent.TimeUnit


class ReceiptBuilder(private val printerManager: POIPrinterManage) :
    AndroidTerminalReceiptBuilderFactory<ReceiptBuilder, Single<PrinterResponse>>() {

    override fun getThis(): ReceiptBuilder = this


    override fun appendTextEntity(p0: String?) {
        val textPrintLine = TextPrintLine().apply {
            type = PrintLine.TEXT
            content = p0
            position = PrintLine.LEFT
            size = TextPrintLine.FONT_NORMAL
            isBold = false
            isItalic = false
            isInvert = false
        }
        printerManager.addPrintLine(textPrintLine)
    }

    override fun appendTextEntityBold(p0: String?) {
        val textPrintLine = TextPrintLine().apply {
            type = PrintLine.TEXT
            content = p0
            position = PrintLine.LEFT
            size = TextPrintLine.FONT_NORMAL
            isBold = true
            isItalic = false
            isInvert = false
        }
        printerManager.addPrintLine(textPrintLine)
    }

    override fun appendTextEntityFontSixteen(p0: String?) {
        val textPrintLine = TextPrintLine().apply {
            type = PrintLine.TEXT
            content = p0
            position = PrintLine.LEFT
            size = TextPrintLine.FONT_NORMAL
            isBold = false
            isItalic = false
            isInvert = false
        }
        printerManager.addPrintLine(textPrintLine)
    }

    override fun appendTextEntityFontSixteenCenter(p0: String?) {
        val textPrintLine = TextPrintLine().apply {
            type = PrintLine.TEXT
            content = p0
            position = PrintLine.CENTER
            size = TextPrintLine.FONT_LARGE
            isBold = true
            isItalic = false
            isInvert = false
        }
        printerManager.addPrintLine(textPrintLine)
    }

    override fun appendTextEntityCenter(p0: String?) {
        val textPrintLine = TextPrintLine().apply {
            type = PrintLine.TEXT
            content = p0
            position = PrintLine.CENTER
            size = TextPrintLine.FONT_NORMAL
            isBold = false
            isItalic = false
            isInvert = false
        }
        printerManager.addPrintLine(textPrintLine)
    }

    fun print(printerListener: POIPrinterManage.IPrinterListener) {
        build()
        printerManager.beginPrint(printerListener)
    }

    override fun print(): Single<PrinterResponse> =
        Single.create {
            build()
            var hasStartedPrinting = false
            Handler(Looper.getMainLooper()).postDelayed({
                if (hasStartedPrinting.not())
                    it.onError(POSPrinterException(-1, "Took too long to start printing"))
            }, 7000)
            printerManager.beginPrint(object : POIPrinterManage.IPrinterListener {
                override fun onError(p0: Int, p1: String?) {
                    it.onError(POSPrinterException(p0, p1 ?: "Printer Error"))
                }

                override fun onFinish() {
                    it.onSuccess(PrinterResponse())
                }

                override fun onStart() {
                    hasStartedPrinting = true
                    Log.e("TAG", "Printing started")
                }
            })
        }

    override fun appendImageCenter(bitmap: Bitmap) {
        val bitmapPrintLine = BitmapPrintLine()
        bitmapPrintLine.type = PrintLine.BITMAP
        bitmapPrintLine.position = PrintLine.CENTER
//        val bitmap: Bitmap =
//            BitmapFactory.decodeResource(context.resources, R.drawable.ic_netpos_new)
        bitmapPrintLine.bitmap = Bitmap.createScaledBitmap(bitmap, 180, 120, false)
        printerManager.addPrintLine(bitmapPrintLine)
    }

    override fun appendLogo(bitmap: Bitmap) {
        appendImageCenter(bitmap)
    }
}
