package ng.com.netpos.app


import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.utils.DeviceConfig
import io.reactivex.disposables.CompositeDisposable

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
            showCardDialog(this, 1, 1, compositeDisposable) { result ->
                findViewById<TextView>(R.id.scan_result).text = result.toString()
            }
        }
    }
}
