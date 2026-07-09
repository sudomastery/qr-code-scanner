package com.sudomastery.qrscanner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sudomastery.qrscanner.parsing.ScanContent
import com.sudomastery.qrscanner.scan.QrAnalyzer
import com.sudomastery.qrscanner.scan.barcodeFormatName
import com.sudomastery.qrscanner.util.Actions
import java.util.concurrent.Executors

private const val RESCAN_COOLDOWN_MS = 2500L

@Composable
fun ScanScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var currentResult by remember { mutableStateOf<ScanContent?>(null) }
    var lastValue by remember { mutableStateOf("") }
    var lastValueAt by remember { mutableStateOf(0L) }
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val scanPaused = currentResult != null

    fun handleHit(raw: String, formatName: String) {
        if (currentResult != null) return
        val now = System.currentTimeMillis()
        if (raw == lastValue && now - lastValueAt < RESCAN_COOLDOWN_MS) {
            lastValueAt = now
            return
        }
        lastValue = raw
        lastValueAt = now

        if (settings.vibrateOnScan) Actions.vibrate(context)
        if (settings.beepOnScan) Actions.beep()
        viewModel.recordScan(raw, formatName)

        val content = ScanContent.parse(raw)
        if (settings.autoOpenLinks && content is ScanContent.Url) {
            val target = if (settings.removeTrackers) content.cleaned else content.raw
            Actions.openUrl(context, target)
        } else {
            currentResult = content
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
            ).process(image).addOnSuccessListener { barcodes ->
                val hit = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
                if (hit != null) {
                    handleHit(hit.rawValue!!, barcodeFormatName(hit.format))
                } else {
                    android.widget.Toast
                        .makeText(context, "No code found in image", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                paused = scanPaused,
                onCameraReady = { camera = it },
                onHit = { raw, format -> handleHit(raw, barcodeFormatName(format)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            ScannerOverlay(modifier = Modifier.fillMaxSize())

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        torchOn = !torchOn
                        camera?.cameraControl?.enableTorch(torchOn)
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (torchOn) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                    )
                ) {
                    Icon(
                        if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Toggle flashlight"
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                    )
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "Scan from image")
                }
            }
        } else {
            PermissionRequest(onRequest = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })
        }

        currentResult?.let { content ->
            ResultSheet(
                content = content,
                settings = settings,
                onDismiss = { currentResult = null }
            )
        }
    }
}

@Composable
private fun CameraPreview(
    paused: Boolean,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
    onHit: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    // Read through state so the analyzer callback sees the current value
    val pausedState = remember { mutableStateOf(paused) }
    pausedState.value = paused

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
                val cam = cameraRef ?: return@detectTransformGestures
                val current = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                cam.cameraControl.setZoomRatio(
                    (current * zoom).coerceIn(
                        cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                        cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                    )
                )
            }
        },
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(
                    executor,
                    QrAnalyzer { raw, format ->
                        if (!pausedState.value) {
                            previewView.post { onHit(raw, format) }
                        }
                    }
                )
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                cameraRef = camera
                onCameraReady(camera)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Transparent)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Transparent)
            )
        }
        Text(
            text = "Point at a QR code",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Camera access is needed to scan codes",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )
        Button(onClick = onRequest) {
            Text("Grant camera access")
        }
    }
}
