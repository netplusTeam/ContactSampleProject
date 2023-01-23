package com.netpluspay.netpossdk.errors

import java.lang.Exception

open class POSException(val errorCode: Int, private val errorMessage: String) : Exception() {
    override fun getLocalizedMessage(): String = errorMessage

    override val message: String
        get() = errorMessage
}
