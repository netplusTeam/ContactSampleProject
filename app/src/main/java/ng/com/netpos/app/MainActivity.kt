package ng.com.netpos.app

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.netpluspay.netposbarcodesdk.NetPosBarcodeSdk
import com.netpluspay.netposbarcodesdk.RESULT_CODE_TEXT
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.netpluspay.netpossdk.utils.TerminalParameters
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val disposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*NetPosSdk.writeTpkKey(DeviceConfig.TPKIndex, "6943DD4434E0B3C0D808D0FE2A590CD9")
        findViewById<Button>(R.id.scan_card).setOnClickListener {
            showCardDialog(this, 200, 0, disposable) { result ->
                Timber.e("Hello 👋")
                Log.e("TAG", "Hello 👋")
                //Log.e("TAG", "ICC Subset: ${result.nibssIccSubset}")
                Timber.e("ICC Subset: ${result.nibssIccSubset}")
                Timber.e(result.applicationPANSequenceNumber)
                Timber.e(result.track2Data)
                Timber.e(result.encryptedPinBlock)
                findViewById<TextView>(R.id.scan_result).text = result.toString()
            }
        }*/
//        val resultLauncher =
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK) {
//                    // There are no request codes
//                    val data: Intent? = result.data
//                    data?.let {
//                        if (it.hasExtra(RESULT_CODE_TEXT)){
//                            val text = it.getStringExtra(RESULT_CODE_TEXT)
//                            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            }
        //NetPosBarcodeSdk.startScan(this, resultLauncher, 100)
        NetPosBarcodeSdk.startScan(this, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            if (it.hasExtra(RESULT_CODE_TEXT)) {
                val text = it.getStringExtra(RESULT_CODE_TEXT)
                Timber.e("QR text: $text")
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
        }

    }
}