package com.netpluspay.netposbarcodesdk.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.netpluspay.netposbarcodesdk.LONG_150

fun LifecycleOwner.showSnackBar(rootView: View, message: String) {
    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
}

fun vibrateThePhone(context: Context, duration: Long = LONG_150) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
            VibrationEffect.createOneShot(
                duration,
                VibrationEffect.DEFAULT_AMPLITUDE,
            ),
        )
    } else {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(duration)
    }
}
