@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ulcer.care.detection.ulcare.tflite.TfliteRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmImageActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_PATIENT_NAME = "extra_patient_name"
        private const val EXTRA_PATIENT_GENDER = "extra_patient_gender" // "L"/"P"

        fun newIntent(
            context: Context,
            imageUri: Uri,
            patientName: String? = null,
            patientGender: String? = null
        ): Intent = Intent(context, ConfirmImageActivity::class.java).apply {
            putExtra(EXTRA_URI, imageUri.toString())
            putExtra(EXTRA_PATIENT_NAME, patientName)
            putExtra(EXTRA_PATIENT_GENDER, patientGender)
        }
    }

    // simpan supaya mudah dipakai saat navigasi ke Result
    private var pName: String? = null
    private var pGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // non edge-to-edge, AppBar solid
        WindowCompat.setDecorFitsSystemWindows(window, true)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        val imageUri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        pName = intent.getStringExtra(EXTRA_PATIENT_NAME)
        pGender = intent.getStringExtra(EXTRA_PATIENT_GENDER)

        setContent {
            val scheme = MaterialTheme.colorScheme
            SideEffect { window.statusBarColor = scheme.surface.toArgb() }

            MaterialTheme {
                ConfirmScreen(
                    imageUri = imageUri,
                    onClose = { finish() },
                    onConfirm = { uri ->
                        if (uri != null) runModelThenNavigate(uri)
                    }
                )
            }
        }
    }

    private fun runModelThenNavigate(imageUri: Uri) {
        lifecycleScope.launch {
            var errorMsg: String? = null
            try {
                // Decode bitmap di IO thread
                val bmp = withContext(Dispatchers.IO) {
                    TfliteRunner.decodeBitmapSmall(this@ConfirmImageActivity, imageUri)
                }

                // Jalankan model ULCARE.tflite (interpreter manual)
                val res = withContext(Dispatchers.Default) {
                    TfliteRunner.runWithInterpreter(this@ConfirmImageActivity, bmp)
                }

                // Validasi minimal: 5 kelas & semua finite
                val raw = res.raw
                val ok = raw.isNotEmpty() && raw.size >= 5 && raw.all { it.isFinite() }
                if (!ok) {
                    errorMsg = buildString {
                        appendLine("Output model tidak sesuai spesifikasi.")
                        appendLine("- Panjang output ≥ 5 (5 kelas target)")
                        appendLine("- Semua nilai harus finite (bukan NaN/Inf)")
                        append("Panjang sekarang: ${raw.size}")
                    }
                } else {
                    // Kirim hanya informasi yang berasal dari ULCARE.tflite:
                    // - classification: dari model
                    // - details/action: kosong (tidak ada di tflite)
                    startActivity(
                        ResultActivityS.newIntent(
                            context         = this@ConfirmImageActivity,
                            imageUri        = imageUri.toString(),
                            classification  = res.classification,
                            details         = "",              // kosong sesuai permintaan
                            action          = "",              // kosong sesuai permintaan
                            patientName     = pName,
                            patientGender   = pGender
                        )
                    )
                    finish()
                }
            } catch (e: Exception) {
                errorMsg = "Gagal menjalankan model ULCARE.tflite: ${e.message}"
            }

            errorMsg?.let { msg ->
                setContent { MaterialTheme { ErrorDialog(msg) { finish() } } }
            }
        }
    }
}

/* ---------------- UI ---------------- */

@Composable
private fun ConfirmScreen(
    imageUri: Uri?,
    onClose: () -> Unit,
    onConfirm: (Uri?) -> Unit
) {
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Konfirmasi Gambar") },
                navigationIcon = {
                    IconButton(onClick = { if (!loading) onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!loading) {
                            loading = true
                            onConfirm(imageUri)
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "OK")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .size(1080) // batasi agar ringan
                        .build(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Gambar tidak ditemukan")
            }

            if (loading) CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tidak Dapat Memproses") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

/* ---------------- Preview ---------------- */

@Preview(showBackground = true, name = "Confirm – kosong")
@Composable
private fun Prev_Confirm_Empty() {
    MaterialTheme {
        ConfirmScreen(imageUri = null, onClose = {}, onConfirm = {})
    }
}

@Preview(showBackground = true, name = "Confirm – dummy uri")
@Composable
private fun Prev_Confirm_WithImage() {
    val fake = remember { Uri.parse("file:///android_asset/preview.jpg") }
    MaterialTheme {
        ConfirmScreen(imageUri = fake, onClose = {}, onConfirm = {})
    }
}

@Preview(showBackground = true, name = "Error dialog")
@Composable
private fun Prev_Error() {
    MaterialTheme {
        ErrorDialog("Output model tidak sesuai spesifikasi.\nPanjang sekarang: 1") {}
    }
}