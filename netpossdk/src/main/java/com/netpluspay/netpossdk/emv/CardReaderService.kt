package com.netpluspay.netpossdk.emv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.netpluspay.netpossdk.core.AndroidTerminalCardReaderFactory
import com.netpluspay.netpossdk.emv.constant.AppConstants.DEFAULT_PIN_ERROR_CODE
import com.netpluspay.netpossdk.emv.constant.AppConstants.isPinErrorCode
import com.netpluspay.netpossdk.emv.data.TransactionData
import com.netpluspay.netpossdk.errors.POSException
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.netpluspay.netpossdk.utils.GlobalData
import com.netpluspay.netpossdk.utils.tlv.*
import com.netpluspay.netpossdk.view.MaterialDialog
import com.netpluspay.netpossdk.view.PasswordDialog
import com.pos.sdk.emvcore.IPosEmvCoreListener
import com.pos.sdk.emvcore.POIEmvCoreManager
import com.pos.sdk.emvcore.POIEmvCoreManager.*
import com.pos.sdk.emvcore.PosEmvErrCode
import com.pos.sdk.security.POIHsmManage
import com.pos.sdk.utils.PosUtils
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.util.*
import java.util.concurrent.CountDownLatch

class CardReaderService @JvmOverloads constructor(
    activity: Activity,
    private val modes: List<Int> = listOf(
        DEV_ICC,
        DEV_PICC
    ),
    private val timeout: Int = 45,
    private val keyMode: Int = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK
) : AndroidTerminalCardReaderFactory<Observable<CardReaderEvent>>() {
    private val logTag = CardReaderService::class.java.simpleName
    private var transactionData = TransactionData()
    private lateinit var emitter: ObservableEmitter<CardReaderEvent>
    private var isOnline = false
    private lateinit var passwordDialog: PasswordDialog
    private var isUpdate = false
    private var cardType = 0
    private lateinit var reqData: Bundle
    private val emvCoreManager: POIEmvCoreManager = getDefault()
    private lateinit var cardPinBlock: String
    private val emvCoreListener: IPosEmvCoreListener.Stub = object : IPosEmvCoreListener.Stub() {
        override fun onEmvProcess(cardMode: Int) {
            if (cardMode == DEV_ICC || cardMode == DEV_MAG || cardMode == DEV_PICC) {
                cardType = cardMode
                when (cardMode) {
                    DEV_ICC -> {
                        // tvMessage1.setText("Icc Card Trans")
                        Log.d("TAG", "ICC Card trans")
                    }
                    DEV_MAG -> {
                        Log.d("TAG", "Mag Card trans")
                        // tvMessage1.setText("Mag Card Trans")
                    }
                    DEV_PICC -> {
                        Log.d("TAG", "PICC Card trans")
                        // tvMessage1.setText("Picc Card Trans")
                    }
                }
                if (emitter.isDisposed.not()) {
                    Log.d("ON_EMV_PROCESS", "Called")
                    emitter.onNext(CardReaderEvent.CardDetected(cardMode))
                }
                // tvMessage2.setText("Processing")
                transactionData.cardType = cardType
            } else {
                when (cardMode) {
                    PosEmvErrCode.EMV_TIMEOUT -> {
                        Log.d(logTag, "Detection Card Timeout")
                        transEnd(cardMode, "Card Detection Timeout")
                        return
                    }
                    PosEmvErrCode.EMV_CANCEL -> {
                        Log.d(logTag, "Transaction Cancel")
                        transEnd(cardMode, "Transaction Canceled")
                        return
                    }
                    PosEmvErrCode.EMV_MULTI_PICC -> {
                        Log.d(logTag, "Multiple cards , Present a single card")
                        transEnd(cardMode, "Multiple cards, Present a single card")
                        return
                    }
                    PosEmvErrCode.EMV_FALLBACK -> {
                        transEnd(cardMode, "Could not read card")
                    }
                    PosEmvErrCode.EMV_ICC_INTERFACE -> {
                        scanForCard()
                        isUpdate = true
                    }
                }
            }
        }

        override fun onSelectApplication(appNameList: MutableList<String>, isFirstSelect: Boolean) {
            val appNames: Array<String> = appNameList.toTypedArray()
            val mDialog = MaterialDialog(activity)
            mDialog.showListConfirmChoseDialog(
                "Select Application",
                appNames
            ) { position ->
                emvCoreManager.onSetSelAppResponse(position)
            }
        }

        override fun onConfirmCardInfo(p0: Int, p1: String?) {
            emvCoreManager.onSetConfirmCardInfo(true)
        }

        override fun onPiccKernelMode(p0: Int) {
        }

        override fun onPiccSecondTapCard() {
        }

        override fun onRequestInputPin(p0: Bundle?) {
            callPinPadDialog(activity, p0) {}
        }

        override fun onRequestOnlineProcess(dataBundle: Bundle) {
            isOnline = true
            var buff = dataBundle.getByteArray(EmvOnlineRequestConstraints.EMVDATA)
            Log.d(logTag, "Emv Data :" + HexUtil.toHexString(buff))

            val tlvParser = BerTlvParser()
            val tlvs: BerTlvs = tlvParser.parse(buff)
            for (tlv in tlvs.list) {
                Log.d(
                    logTag,
                    "Emv Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue
                )
            }

            val result = dataBundle.getInt(
                EmvOnlineRequestConstraints.ENCRYPTRESULT,
                PosEmvErrCode.EMV_UNENCRYPTED
            )
            when (result) {
                PosEmvErrCode.EMV_OK -> {
                    val encryptData =
                        dataBundle.getByteArray(EmvOnlineRequestConstraints.ENCRYPTDATA)
                    val encryptTlvParser = BerTlvParser()
                    val encryptTlvs: BerTlvs = encryptTlvParser.parse(encryptData)
                    for (tlv in encryptTlvs.list) {
                        Log.d(
                            logTag,
                            "Emv Encrypt Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue
                        )
                    }
                    val tlvBuilder = BerTlvBuilder()
                    for (tlv in tlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    for (tlv in encryptTlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    buff = tlvBuilder.buildArray()
                }
                PosEmvErrCode.EMV_UNENCRYPTED -> {
                }
                else -> {
                    Log.e(logTag, "EncryptResult :$result")
                }
            }
            transactionData.transData = buff
            val onlineInput = Bundle()
            onlineInput.putInt(
                EmvOnlineResultConstraints.REQUESTAC,
                GlobalData.getTransOnlineResult()
            )
            emvCoreManager.onSetOnlineResponse(onlineInput)
        }

        override fun onTransactionResult(result: Int, resultData: Bundle?) {
            var buff = resultData?.getByteArray(EmvProcessResultConstraints.EMVDATA)
            Log.d(logTag, "Emv Data :" + HexUtil.toHexString(buff))

            val tlvParser = BerTlvParser()
            val tlvs: BerTlvs = tlvParser.parse(buff)
            for (tlv in tlvs.list) {
                Log.d(logTag, "Emv Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue)
            }

            val encryptResult = resultData?.getInt(
                EmvOnlineRequestConstraints.ENCRYPTRESULT,
                PosEmvErrCode.EMV_UNENCRYPTED
            )
            when (encryptResult) {
                PosEmvErrCode.EMV_OK -> {
                    val encryptData =
                        resultData.getByteArray(EmvOnlineRequestConstraints.ENCRYPTDATA)
                    val encryptTlvParser = BerTlvParser()
                    val encryptTlvs = encryptTlvParser.parse(encryptData)
                    for (tlv in encryptTlvs.list) {
                        Log.d(
                            logTag,
                            "Emv Encrypt Tag :" + tlv.tag.toString() + "    Emv Value :" + tlv.hexValue
                        )
                    }
                    val tlvBuilder = BerTlvBuilder()
                    for (tlv in tlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    for (tlv in encryptTlvs.list) {
                        tlvBuilder.addBerTlv(tlv)
                    }
                    buff = tlvBuilder.buildArray()
                }
                PosEmvErrCode.EMV_UNENCRYPTED -> {
                }
                else -> {
                    Log.d(logTag, "EncryptResult :$encryptResult")
                }
            }

            val scriptBuff = resultData?.getByteArray(EmvProcessResultConstraints.SCRIPTRESULT)
            if (scriptBuff != null) {
                Log.d(logTag, "Script :" + PosUtils.bytesToHexString(scriptBuff))
            }

            when (val cvm = resultData?.getInt(EmvProcessResultConstraints.CVM)) {
                EmvProcessResultConstraints.CVM_SIGNATURE -> Log.d(logTag, "Cvm :CVM_SIGNATURE")
                EmvProcessResultConstraints.CVM_CONFIRMATION_CODE_VERIFIED -> Log.d(
                    logTag,
                    "Cvm :CVM_CONFIRMATION_CODE_VERIFIED"
                )
                EmvProcessResultConstraints.CVM_NO_CVM -> Log.d(logTag, "Cvm :CVM_NO_CVM")
                EmvProcessResultConstraints.CVM_SEE_PHONE -> {
                    transEnd(cvm, "CVM: CVM_NO_CVM")
                    return
                }
            }

            if (result == PosEmvErrCode.EMV_APP_EMPTY) {
                transEnd(result, "AID Empty")
                return
            }

            if (!isOnline) {
                transactionData.transData = buff
            }

            transactionData.transState = result

            when (result) {
                PosEmvErrCode.EMV_APPROVED_ONLINE, PosEmvErrCode.EMV_APPROVED -> if (isOnline) {
                    transactionData.transState = PosEmvErrCode.EMV_APPROVED_ONLINE
                } else {
                    transactionData.transState = PosEmvErrCode.EMV_APPROVED
                }
            }
            if (emitter.isDisposed.not()) {
                if (cardType == DEV_PICC) {
                    val aidTag = BerTag("84")
                    val cardPanTag = BerTag("5A")
                    val cardPan = tlvs.find(cardPanTag).hexValue
                    val isVisaCard = tlvs.find(aidTag).hexValue == "A0000000031010"
                    if (isVisaCard) {
                        val modifiedBundle =
                            resultData?.let { modifyBundleForVisaContactless(it, cardPan) }
                        val isIcSlot = cardType == DEV_ICC
                        val pinPadResult = handleVisaContactlessImpl(
                            activity,
                            isIcSlot,
                            modifiedBundle!!,
                            keyMode
                        )
                        val pinBlockValue = pinPadResult.first
                        val errorResultCode = pinPadResult.second
                        val isTherePinError = isPinErrorCode(errorResultCode)

                        if (isTherePinError) {
                            pinPadErrorCallBack(errorResultCode)
                            return
                        } else {
                            val cardReadResult = CardReadResult(result, transactionData).apply {
                                encryptedPinBlock = pinBlockValue
                            }
                            emitter.onNext(
                                CardReaderEvent.CardRead(
                                    cardReadResult
                                )
                            )
                        }
                    } else {
                        handleOnNextEmissionForCardResult(result)
                    }
                } else {
                    handleOnNextEmissionForCardResult(result)
                }
                emitter.onComplete()
                emvCoreManager.stopTransaction()
            }
        }
    }

    private fun handleVisaContactlessImpl(
        activity: Activity,
        iccSlot: Boolean,
        bundle: Bundle,
        keyMode: Int
    ): Pair<String, Int> {
        val latch = CountDownLatch(1)
        var pinBlockValue = ""
        var errorResult = DEFAULT_PIN_ERROR_CODE
        Handler(Looper.getMainLooper())
            .post {
                val pinPadDialog = instantiatePasswordDialog(activity, iccSlot, bundle, keyMode) {}
                pinPadDialog.passwordDialog.setOnDismissListener {
                    pinBlockValue = pinPadDialog.pinBlockValue
                    errorResult = pinPadDialog.transactionErrorCode
                    latch.countDown()
                }
                pinPadDialog.showDialog()
            }
        latch.await()

        return Pair(pinBlockValue, errorResult)
    }

    private fun handleOnNextEmissionForCardResult(result: Int) {
        emitter.onNext(
            CardReaderEvent.CardRead(
                CardReadResult(
                    result,
                    transactionData
                ).apply {
                    if (::cardPinBlock.isInitialized.not()) {
                        transEnd(-1, "Did not request pinblock")
                        return
                    }
                    encryptedPinBlock = cardPinBlock
                }
            )
        )
    }

    private fun instantiatePasswordDialog(
        activity: Activity,
        iccSlot: Boolean,
        bundle: Bundle?,
        keyMode: Int,
        callback: () -> Unit
    ) =
        PasswordDialog(activity, iccSlot, bundle, DeviceConfig.TPKIndex, keyMode).apply {
            setPinListener(object : PasswordDialog.Listener {
                override fun onConfirm(
                    verifyResult: Int,
                    pinBlock: ByteArray?,
                    pinKsn: ByteArray?
                ) {
                    cardPinBlock = ""
                    pinBlock?.let {
                        cardPinBlock =
                            HexUtil.toHexString(it).toLowerCase(Locale.getDefault())
                        callback()
                    }
                    emvCoreManager.onSetConfirmPin(Bundle())
                }

                override fun onError(verifyResult: Int, pinTryCntOut: Int) {
                    pinPadErrorCallBack(verifyResult)
                }
            })
        }

    private fun pinPadErrorCallBack(errorCode: Int) {
        val pinErrorMessage = when (errorCode) {
            EmvPinConstraints.EMV_VERIFY_PIN_ERROR -> {
                "Pin Verification Failed"
            }
            EmvPinConstraints.EMV_VERIFY_NO_PASSWORD -> {
                "Pin was not entered"
            }
            EmvPinConstraints.EMV_VERIFY_NO_PINPAD -> {
                "No PinPad"
            }
            EmvPinConstraints.EMV_VERIFY_PIN_BLOCK -> {
                "PinBlock Error"
            }
            else -> {
                "Unexpected PinPad Error"
            }
        }
        transEnd(errorCode, message = pinErrorMessage)
    }

    private fun callPinPadDialog(activity: Activity, p0: Bundle?, callback: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            val isIcSlot = cardType == DEV_ICC
            passwordDialog = instantiatePasswordDialog(activity, isIcSlot, p0, keyMode) {}
            passwordDialog.showDialog()
        }
    }

    fun initiateICCCardPayment() = initiateICCCardPayment(0, 0)

    override fun initiateICCCardPayment(
        p0: Long,
        p1: Long
    ): Observable<CardReaderEvent> {
        reqData = Bundle().apply {
            putInt(EmvTransDataConstraints.TRANSTYPE, EMV_GOODS)
            putInt(EmvTransDataConstraints.TRANSAMT, p0.toInt())
            putInt(EmvTransDataConstraints.CASHBACKAMT, p1.toInt())
            putInt(EmvTransDataConstraints.TRANSTIMEOUTMS, timeout)
            var transMode = 0
            if (modes.isEmpty()) {
                Log.e("initiateICCCardPayment", "No mode selected")
                throw POSException(0, "No CardMode selected")
            }
            modes.forEach {
                transMode = transMode or it
            }
            putInt(EmvTransDataConstraints.TRANSMODE, transMode)
        }
        transactionData.apply {
            transType = EMV_GOODS
            transAmount = java.lang.Double.valueOf(p0.toDouble())
            transCashAmount = java.lang.Double.valueOf(p1.toDouble())
            transState = PosEmvErrCode.EMV_OTHER_ERROR
        }
        return Observable.create {
            emitter = it
            emitter.setCancellable {
                emvCoreManager.stopTransaction()
            }
            scanForCard()
        }
    }

    override fun scanForCard() {
        val ret = emvCoreManager.startTransaction(reqData, emvCoreListener)
        when {
            PosEmvErrCode.EMV_CANCEL == ret -> {
                transEnd(ret, "start Transaction cancel")
            }
            PosEmvErrCode.EXCEPTION_ERR == ret -> {
                transEnd(ret, "start Transaction exception error")
            }
            PosEmvErrCode.EMV_ENCRYPT_ERROR == ret -> {
                transEnd(ret, "start Transaction encrypt error")
            }
        }
    }

    fun transEnd(errorCode: Int = PosEmvErrCode.EMV_CANCEL, message: String = "POS Cancel Error") {
        emvCoreManager.stopTransaction()
        if (emitter.isDisposed.not()) {
            emitter.onError(POSException(errorCode, message))
        }
    }

    override fun showPinPad() {
        Log.e(logTag, "Redundant")
    }

    override fun readCard() {
        Log.e("TAG", "Unnecessary")
    }

    override fun updateICCardData(p0: Int) {
        Log.e("TAG", "Unnecessary")
    }

    private fun modifyBundleForVisaContactless(
        bundle: Bundle,
        cardPan: String
    ): Bundle = bundle.apply {
        putInt(EmvPinConstraints.PINTYPE, 34)
        putString(EmvPinConstraints.PINCARD, cardPan)
        putBoolean(EmvPinConstraints.PINALLOWBYPASS, false)
        putInt(EmvPinConstraints.PINOFFTRYCNT, 0)
    }
}
