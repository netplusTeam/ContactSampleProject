@file:Suppress("DEPRECATION")

package ng.com.netpos.app

import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.util.Log
import android.widget.Toast
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.emv.CardReaderEvent
import com.netpluspay.netpossdk.emv.CardReaderService
import com.pos.sdk.emvcore.POIEmvCoreManager.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

typealias ReaderListener = (result: CardReadResult) -> Unit

fun showCardDialog(
    context: Activity,
    amount: Long,
    cashBackAmount: Long,
    compositeDisposable: CompositeDisposable,
    readerListener: ReaderListener
) {
    val dialog = ProgressDialog(context)
        .apply {
            setMessage("Waiting for card")
            setCancelable(false)
        }
    val cardService = CardReaderService(context, listOf(DEV_ICC, DEV_PICC))
    val c = cardService.initiateICCCardPayment(
        amount,
        cashBackAmount
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            when (it) {
                is CardReaderEvent.CardRead -> {
                    Log.d("STILL_TROUBLESHOOTING", "Got here")
                    readerListener.invoke(it.data)
                }
                is CardReaderEvent.CardDetected -> {
                    val mode = when (it.mode) {
                        DEV_ICC -> "EMV"
                        DEV_PICC -> "EMV Contactless"
                        else -> "MAGNETIC STRIPE"
                    }
                    dialog.setMessage("Reading Card with $mode Please Wait")
                    Timber.e("Card Detected")
                    Timber.e("Reading Card with $mode Please Wait")
                }
            }
        }, {
            it?.let {
                it.printStackTrace()
                Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                Timber.e(it.localizedMessage ?: "Fatal Error")
                Timber.e(it)
            }
        }, {
            dialog.dismiss()
        })

    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Stop") { d, _ ->
        cardService.transEnd(message = "Stopped")
        d.dismiss()
    }
    dialog.show()
    compositeDisposable.let {
        c.disposeWith(it)
    }
}

fun Disposable.disposeWith(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)
