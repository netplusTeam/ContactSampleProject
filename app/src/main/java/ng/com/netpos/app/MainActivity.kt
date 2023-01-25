package ng.com.netpos.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.netpluspay.netposbarcodesdk.RESULT_CODE_TEXT
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.utils.DeviceConfig
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var cardResult: CardReadResult? = null
    private lateinit var btn: Button
    private val compositeDisposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.test_card_reading)
        NetPosSdk.writeTpkKey(DeviceConfig.TPKIndex, "6943DD4434E0B3C0D808D0FE2A590CD9", this)
        btn.setOnClickListener {
            showCardDialog(this, 20, 0, compositeDisposable) { result ->
                findViewById<TextView>(R.id.scan_result).text = result.toString()
            }
        }
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
