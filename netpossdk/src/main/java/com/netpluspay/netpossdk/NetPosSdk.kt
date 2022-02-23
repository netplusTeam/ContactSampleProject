package com.netpluspay.netpossdk

import android.content.Context
import android.os.Build
import android.util.Log
import com.netpluspay.netpossdk.emv.param.EmvParam
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.netpluspay.netpossdk.utils.TripleDES
import com.netpluspay.netpossdk.utils.tlv.HexUtil
import com.pos.sdk.accessory.POIGeneralAPI
import com.pos.sdk.emvcore.POIEmvCoreManager
import com.pos.sdk.emvcore.PosEmvAid
import com.pos.sdk.emvcore.PosEmvCapk
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.security.POIHsmManage
import com.pos.sdk.security.PedKcvInfo
import com.pos.sdk.security.PedKeyInfo

object NetPosSdk {

    @JvmStatic
    fun init() {
        initTerminalConfiguration()
    }

    @JvmStatic
    fun getDeviceSerial(): String = try {
        POIGeneralAPI.getDefault().getVersion(POIGeneralAPI.VERSION_TYPE_DSN)
    } catch (e: Exception) {
        e.printStackTrace()
        "B000000000000000"
    }

    @JvmStatic
    private fun initTerminalConfiguration() {
        if (Build.MODEL.equals("PRO", true) || Build.MODEL.equals("P3", true))
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_PRO)
        else
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_MINI)
    }


    @JvmStatic
    fun getPrinterManager(context: Context): POIPrinterManage = POIPrinterManage.getDefault(context)

    @JvmStatic
    fun loadEmvParams(terminalParameters: TerminalParameters = TerminalParameters()) {
        EmvParam.loadTerminalParam(terminalParameters)
        EmvParam.loadVisaParam()
        EmvParam.loadMasterCardParam()
        EmvParam.loadVisaDrl()
    }

    @JvmStatic
    fun loadProvidedCapksAndAids() {
        EmvParam.loadAddAids()
        EmvParam.loadAddCapks()
    }

    @JvmStatic
    fun loadAids(aidList: List<PosEmvAid>) {
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        aidList.forEach {
            Log.e("TAG", mEmvCoreManage.EmvAddAid(it).toString())
        }
    }

    @JvmStatic
    fun getAids(): MutableList<PosEmvAid>? {
        return POIEmvCoreManager.getDefault().EmvGetAllAid()
    }

    @JvmStatic
    fun getCapks(): MutableList<PosEmvCapk>? {
        return POIEmvCoreManager.getDefault().EmvGetAllCapk()
    }

    @JvmStatic
    fun clearAids() {
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        mEmvCoreManage.EmvDelAllAid()
    }

    @JvmStatic
    fun clearCapks() {
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        mEmvCoreManage.EmvDelAllCapk()
    }

    @JvmStatic
    fun loadCapks(capkLisk: List<PosEmvCapk>) {
        val mEmvCoreManager = POIEmvCoreManager.getDefault()
        capkLisk.forEach {
            mEmvCoreManager.EmvAddCapk(it)
        }
    }

    @JvmStatic
    fun writeTpkKey(keyIndex: Int, keyData: String): Int = try {
        val pedKeyInfo = PedKeyInfo(
            0, 0, POIHsmManage.PED_TPK, keyIndex, 0, 16,
            HexUtil.parseHex(keyData)
        )
        POIHsmManage.getDefault().PedWriteKey(pedKeyInfo, PedKcvInfo(0, ByteArray(5)))
    } catch (exception: Exception) {
        exception.printStackTrace()
        -1
    }

    @JvmStatic
    fun writeDukptKey(keyIndex: Int, keyData: String, ksnData: String): Int {
        val key = HexUtil.parseHex(keyData)
        val ksn = HexUtil.parseHex(ksnData)
        return POIHsmManage.getDefault().PedWriteTIK(
            keyIndex, 0, key.size, key, ksn,
            PedKcvInfo(5, ByteArray(5))
        )
    }


    @JvmStatic
    fun writeTpkKey(keyIndex: Int, encryptedKeyData: String, masterKey: String) =
        writeTpkKey(keyIndex, keyData = TripleDES.decrypt(encryptedKeyData, masterKey))

}