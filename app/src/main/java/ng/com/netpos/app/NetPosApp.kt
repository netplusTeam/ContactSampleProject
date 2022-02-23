package ng.com.netpos.app

import android.app.Application
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.utils.TerminalParameters
import timber.log.Timber

class NetPosApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
//        NetPosSdk.init()
//        NetPosSdk.loadEmvParams(TerminalParameters().apply {
//            terminalCapability = "E0F8C8"
//        })
    }
}