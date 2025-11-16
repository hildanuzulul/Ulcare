@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ActionBar sama seperti About: judul + tombol back
        supportActionBar?.apply {
            title = "Result"
            setDisplayHomeAsUpEnabled(true)
        }

        // Ambil data dari Intent
        val imageUriStr = intent.getStringExtra(EXTRA_IMAGE_URI)
        val classification = intent.getStringExtra(EXTRA_CLASSIFICATION).orEmpty()
        val details = intent.getStringExtra(EXTRA_DETAILS).orEmpty()
        val tindakan = intent.getStringExtra(EXTRA_TINDAKAN).orEmpty()

        val imageUri = imageUriStr?.let { Uri.parse(it) }

        setContent {
            ULCARETheme {
                ResultScreen(
                    imageUri = imageUri,
                    classification = classification,
                    details = details,
                    tindakan = tindakan
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CLASSIFICATION = "extra_classification"
        const val EXTRA_DETAILS = "extra_details"
        const val EXTRA_TINDAKAN = "extra_tindakan"
    }
}

@Composable
fun ResultScreen(
    imageUri: Uri?,
    classification: String,
    details: String,
    tindakan: String
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp)
        ) {
            // Kartu gambar (aspect 4:3), ada placeholder untuk Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Result Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Preview Image", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            LabelValue("Classification", classification.ifBlank { "—" })
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            LabelValue("Details", details.ifBlank { "—" })
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            LabelValue("Tindakan", tindakan.ifBlank { "—" })

            Spacer(Modifier.weight(1f))

            // Footer versi © agar seragam dengan About
            val context = LocalContext.current
            val versionName = remember {
                runCatching {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrElse { "1.0" }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Versi $versionName", style = MaterialTheme.typography.labelMedium)
                Text("© 2025 ULCARE", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------------- PREVIEWS ---------------- */

@Preview(showBackground = true, name = "Result – No Image")
@Composable
private fun ResultScreenPreview_NoImage() {
    ULCARETheme {
        ResultScreen(
            imageUri = null,
            classification = "Diabetic Foot Ulcer – Grade 2",
            details = "Luka kemerahan, ada inflamasi ringan pada tepi. Perlu kontrol rutin.",
            tindakan = "Bersihkan dengan saline, balut steril, kontrol 2–3 hari."
        )
    }
}

@Preview(showBackground = true, name = "Result – Empty")
@Composable
private fun ResultScreenPreview_Empty() {
    ULCARETheme {
        ResultScreen(
            imageUri = null,
            classification = "",
            details = "",
            tindakan = ""
        )
    }
}