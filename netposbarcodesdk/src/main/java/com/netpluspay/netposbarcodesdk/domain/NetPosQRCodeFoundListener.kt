package com.netpluspay.netposbarcodesdk.domain

internal interface NetPosQRCodeFoundListener {
    fun onQrCodeFound(text: String)
    fun onErrorFound(errorMessage: String)
}
