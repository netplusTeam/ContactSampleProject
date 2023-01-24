package com.netpluspay.netpossdk.emv.contract

import com.netpluspay.netpossdk.emv.CardReadResult

interface ListenerForVisaContactlessReader<T> {
    fun doneReadingCard(cardReadResult: CardReadResult)
}
