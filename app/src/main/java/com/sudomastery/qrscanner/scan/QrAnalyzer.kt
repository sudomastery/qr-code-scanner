package com.sudomastery.qrscanner.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Single ML Kit scanner shared by the camera analyzer and gallery imports,
 * so the format configuration and hit selection live in one place and no
 * per-use detector instances are leaked.
 */
object BarcodeReader {

    val client: BarcodeScanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    fun firstHit(barcodes: List<Barcode>): Barcode? =
        barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
}

/**
 * Feeds camera frames to ML Kit. Reports the first barcode with a value and
 * drops frames while one is still being processed to keep the preview smooth.
 */
class QrAnalyzer(
    private val onResult: (rawValue: String, format: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeReader.client

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image =
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val hit = BarcodeReader.firstHit(barcodes)
                if (hit != null) {
                    onResult(hit.rawValue!!, hit.format)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

fun barcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_ITF -> "ITF"
    else -> "UNKNOWN"
}
