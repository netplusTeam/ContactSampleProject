package com.netpluspay.netpossdk

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.netpluspay.netpossdk.emv.param.EmvParam
import com.netpluspay.netpossdk.utils.AppSharedPreferences
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.netpluspay.netpossdk.utils.TerminalParameters
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
    fun setGenyzApnConfiguration(activity: Activity) {
        val values = ContentValues()
        values.put("name", "fast.m2m")
        values.put("type", "default,supl")
        activity.contentResolver.insert(Uri.parse("content://telephony/carriers"), values)

        val selection = "current = 1"
        values.clear()
        values.put("apn_id", -1)
        activity.contentResolver.update(
            Uri.parse("content://telephony/carriers"),
            values,
            selection,
            null,
        )
    }

    /*
    fun genzyApnConfiguration(activity: Activity) {
        var id = -1
        val resolver: ContentResolver = activity.contentResolver
        var values = ContentValues()

        values.put("name", "fast.m2m") // choose APN name, like 3G Orange

        values.put("apn", "fast.m2m") // choose APN address, like cellcom.wapu.co.il
        // values.put("mcc", "your operator numeric high part") //for example 242

        // values.put("mnc", "your operator numeric low part") //for example 501

        // values.put("numeric", "your operator numeric") //for example 242501

        var c: Cursor? = null
        try {
            // insert new row to APN table
            val newRow = resolver.insert(APN_TABLE_URI, values)
            if (newRow != null) {
                c = resolver.query(newRow, null, null, null, null)

                // obtain the APN id
                val index: Int = c.getColumnIndex("_id")
                c.moveToFirst()
                id = c.getShort(index)
            }
        } catch (e: java.lang.Exception) {
            e.localizedMessage?.let { Log.d("APN_SETTINGS_ERROR", it) }
        }
        values = ContentValues()
        values.put("apn_id", id)

        try {
            resolver.update(PREFERRED_APN_URI, values, null, null)
        } catch (e: java.lang.Exception) {
            e.localizedMessage?.let { Log.d("ERROR_APN_SETTINGS", it) }
        }
    }
    */

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
        if (Build.MODEL.equals("PRO", true) || Build.MODEL.equals("P3", true)) {
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_PRO)
        } else {
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_MINI)
        }
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
    fun writeTpkKey(keyIndex: Int, keyData: String, context: Context): Int = try {
        val pedKeyInfo = PedKeyInfo(
            0,
            0,
            POIHsmManage.PED_TPK,
            keyIndex,
            0,
            16,
            HexUtil.parseHex(keyData),
        )
        saveInSharedPrefs(keyData, context)
        POIHsmManage.getDefault().PedWriteKey(pedKeyInfo, PedKcvInfo(0, ByteArray(5)))
    } catch (exception: Exception) {
        exception.printStackTrace()
        -1
    }

    private fun saveInSharedPrefs(keyData: String, context: Context) {
        val appPrefs = AppSharedPreferences()
        appPrefs.putString(keyData, context)
    }

    public fun getClearKey(context: Context): String {
        val appPrefs = AppSharedPreferences()
        return appPrefs.getString(context)
    }

    @JvmStatic
    fun writeDukptKey(keyIndex: Int, keyData: String, ksnData: String): Int {
        val key = HexUtil.parseHex(keyData)
        val ksn = HexUtil.parseHex(ksnData)
        return POIHsmManage.getDefault().PedWriteTIK(
            keyIndex,
            0,
            key.size,
            key,
            ksn,
            PedKcvInfo(5, ByteArray(5)),
        )
    }

//    @JvmStatic
//    fun writeTpkKey(keyIndex: Int, encryptedKeyData: String, masterKey: String): Int =
//        writeTpkKey(keyIndex, keyData = TripleDES.decrypt(encryptedKeyData, masterKey))
}
