package com.netpluspay.netposbarcodesdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.netpluspay.netposbarcodesdk.databinding.ActivityBarcodeScannerBinding
import com.netpluspay.netposbarcodesdk.domain.NetPosQRCodeFoundListener
import com.netpluspay.netposbarcodesdk.domain.PermissionHandler
import com.netpluspay.netposbarcodesdk.usecases.PermissionHandlerImpl
import com.netpluspay.netposbarcodesdk.util.showSnackBar
import com.netpluspay.netposbarcodesdk.util.vibrateThePhone
import java.io.IOException

class CodeScannerActivity :
    AppCompatActivity(),
    PermissionHandler by PermissionHandlerImpl(),
    NetPosQRCodeFoundListener {
    private lateinit var detector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var surfaceView: SurfaceView
    private lateinit var binding: ActivityBarcodeScannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_barcode_scanner)
        surfaceView = binding.surfaceView
        detector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(this, detector)
            .setRequestedPreviewSize(binding.root.height, binding.root.width)
            .setAutoFocusEnabled(true)
            .build()
    }

    companion object {
        internal fun startActivity(context: Context, launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(context, CodeScannerActivity::class.java)
            launcher.launch(intent)
        }

        internal fun startActivity(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, CodeScannerActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun onResume() {
        super.onResume()
        permissionHandler(
            this,
            this,
            Manifest.permission.CAMERA,
            PERMISSION_REQUEST_CODE,
            R.string.camera_perm_rationale,
        ) {
            startCameraGoogleVision()
        }
    }

    private fun startCameraGoogleVision() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@CodeScannerActivity,
                            Manifest.permission.CAMERA,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    cameraSource.start(p0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@CodeScannerActivity,
                            Manifest.permission.CAMERA,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    cameraSource.start(p0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        detector.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {
                onErrorFound(getString(R.string.scanner_closed))
                Toast.makeText(
                    this@CodeScannerActivity,
                    getString(R.string.scanner_closed),
                    Toast.LENGTH_SHORT,
                )
                    .show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode?>) {
                val barcodes: SparseArray<Barcode?> = detections.detectedItems
                if (barcodes.size() > 0) {
                    val qrCodeText = barcodes.valueAt(0)?.rawValue
                    runOnUiThread {
                        qrCodeText?.let {
                            cameraSource.stop()
                            onQrCodeFound(it)
                        }
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        cameraSource.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, getString(R.string.perm_granted), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, getString(R.string.perm_denied), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showSnackBar(binding.root, getString(R.string.scanning_not_completed))
        onErrorFound(getString(R.string.scanning_not_completed))
    }

    override fun onQrCodeFound(text: String) {
        vibrateThePhone(this@CodeScannerActivity)
        val data = Intent().apply {
            putExtra(NETPOS_BARCODE_SCANNER_RESULT, text)
        }
        setResult(RESULT_OK, data)
    }

    override fun onErrorFound(errorMessage: String) {
        val data = Intent().apply {
            putExtra(NETPOS_BARCODE_SCANNER_ERROR, errorMessage)
        }
        setResult(RESULT_CANCELED, data)
    }
}
