@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.dhaval2404.imagepicker.ImagePicker
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
// ★ status bar control
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PATIENT_NAME   = "extra_patient_name"
        const val EXTRA_PATIENT_GENDER = "extra_patient_gender" // "L" / "P"
    }

    private var patientName: String? = null
    private var patientGender: String? = null

    private lateinit var cameraExecutor: ExecutorService

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val readMediaImagesPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ambil data identitas (bila ada)
        patientName   = intent.getStringExtra(EXTRA_PATIENT_NAME)
        patientGender = intent.getStringExtra(EXTRA_PATIENT_GENDER)

        // ★ Paksa status bar "tetap terang" tetapi ikon PUTIH (karena kamera gelap)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Izin kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Izin akses gambar (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                readMediaImagesPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            // ★ Pakai ULCARETheme yang sudah kamu set agar SELALU LIGHT/PUTIH
            // (pastikan di Theme.kt kamu memaksa lightColorScheme, tanpa mengikuti system)
            ULCARETheme {
                CameraScreen(
                    onBackToIdentity = {
                        startActivity(Intent(this@MainActivity, IdentityActivity::class.java))
                        finish()
                    },
                    onOpenConfirm = { uri ->
                        startActivity(
                            ConfirmImageActivity.newIntent(
                                context = this@MainActivity,
                                imageUri = uri
                                // kalau ConfirmImageActivity menerima nama & gender, kirimkan juga di sini
                                // .putExtra(...) versi builder sesuai implementasi kamu
                            )
                        )
                    },
                    onOpenAbout = {
                        startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }
}

//  UI

@Composable
fun CameraScreen(
    onBackToIdentity: () -> Unit,
    onOpenConfirm: (Uri) -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var flashOn by remember { mutableStateOf(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(context) }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri? = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                onOpenConfirm(uri)
            }
        }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        val capture = ImageCapture.Builder()
            .setFlashMode(if (flashOn) FLASH_MODE_ON else FLASH_MODE_OFF)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                capture
            )
            imageCapture = capture
        } catch (_: Exception) { /* no-op */ }
    }

    LaunchedEffect(flashOn) {
        imageCapture?.flashMode = if (flashOn) FLASH_MODE_ON else FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flashOn)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Preview kamera
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        // Top bar: Back, Flash, About
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToIdentity) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { flashOn = !flashOn }) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = "Flash",
                    tint = if (flashOn) Color.Yellow else Color.White
                )
            }
            IconButton(onClick = onOpenAbout) {
                Icon(Icons.Default.MoreVert, contentDescription = "About", tint = Color.White)
            }
        }

        // Bottom controls: Gallery + Capture
        BottomBar(
            onGallery = {
                ImagePicker.with(context as ComponentActivity)
                    .galleryOnly()
                    .compress(1024)
                    .createIntent { intent -> galleryLauncher.launch(intent) }
            },
            onCapture = {
                val ic = imageCapture ?: return@BottomBar

                val outputDir = context.getExternalFilesDir("Pictures") ?: context.filesDir
                val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(System.currentTimeMillis())
                val photoFile = File(outputDir, "ULCARE_$time.jpg")

                val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                ic.takePicture(
                    output,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("ULCARE_CAMERA", "Gagal ambil gambar: ${exc.message}", exc)
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = Uri.fromFile(photoFile)
                            onOpenConfirm(savedUri)
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun BottomBar(
    onGallery: () -> Unit,
    onCapture: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(IntrinsicSize.Min)
            ) {
                // Tombol Galeri
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onGallery() }
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Gallery",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Gallery",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Tombol Capture
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .align(Alignment.Center)
                        .clickable { onCapture() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// preview
@Preview(showBackground = true, name = "Camera – Preview")
@Composable
private fun CameraScreenPreview() {
    ULCARETheme {
        CameraScreen(
            onBackToIdentity = {},
            onOpenConfirm = { },
            onOpenAbout = { }
        )
    }
}
