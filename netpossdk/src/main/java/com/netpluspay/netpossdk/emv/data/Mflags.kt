package com.netpluspay.netpossdk.emv.data // ktlint-disable filename

data class MFlags(
    val pinCard: String,
    val pinType: Int,
    val pinOffTryCnt: Boolean
)
