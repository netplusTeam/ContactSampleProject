package com.netpluspay.netpossdk.emv.constant

import com.pos.sdk.emvcore.POIEmvCoreManager

object AppConstants {
    /**
     * This is a module level error code not the one coming from kozen library
     * */
    const val DEFAULT_PIN_ERROR_CODE = -789

    /**
     * @param errorResultCode : Int
     * @return Boolean
     * This is the work around for the visa contactless error, because we just manually generated the emv tags for visa contactless
     * hence i am only interested in only the error codes of the EmvPinConstraints class checked below
     * Hence if it is not any of the four below, then we assume that error did not occur, but if we are testing for other cards
     * e.g Mastercard if we get error codes that is different from the four being checked below, we report it as an "Unexpected error"
     * in fact, we don't even use this method to check for error at all
     * */
    fun isPinErrorCode(errorResultCode: Int): Boolean =
        errorResultCode != DEFAULT_PIN_ERROR_CODE && (errorResultCode == POIEmvCoreManager.EmvPinConstraints.EMV_VERIFY_PIN_ERROR || errorResultCode == POIEmvCoreManager.EmvPinConstraints.EMV_VERIFY_NO_PASSWORD || errorResultCode == POIEmvCoreManager.EmvPinConstraints.EMV_VERIFY_NO_PINPAD || errorResultCode == POIEmvCoreManager.EmvPinConstraints.EMV_VERIFY_PIN_BLOCK)
}
