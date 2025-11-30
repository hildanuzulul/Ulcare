@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.R.attr.fontWeight
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.appcompat.app.AppCompatDelegate
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme

const val EXTRA_IMAGE_URI     = "extra_image_uri"
const val EXTRA_CLASS         = "extra_class"       // dari ULCARE.tflite
const val EXTRA_DETAILS       = "extra_details"     // boleh kosong -> akan diisi otomatis
const val EXTRA_ACTION        = "extra_action"      // boleh kosong -> akan diisi otomatis
const val EXTRA_PATIENT_NAME  = "extra_patient_name"
const val EXTRA_PATIENT_GENDER= "extra_patient_gender" // "L"/"P"

class ResultActivityS : ComponentActivity() {

    companion object {
        fun newIntent(
            context: Context,
            imageUri: String?,
            classification: String,
            details: String,
            action: String,
            patientName: String? = null,
            patientGender: String? = null
        ): Intent = Intent(context, ResultActivityS::class.java).apply {
            putExtra(EXTRA_IMAGE_URI, imageUri)
            putExtra(EXTRA_CLASS, classification)
            putExtra(EXTRA_DETAILS, details)
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_PATIENT_NAME, patientName)
            putExtra(EXTRA_PATIENT_GENDER, patientGender)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri      = intent.getStringExtra(EXTRA_IMAGE_URI)
        val rawClass      = intent.getStringExtra(EXTRA_CLASS).orEmpty()
        val passedDetails = intent.getStringExtra(EXTRA_DETAILS).orEmpty()
        val passedAction  = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        val patientName   = intent.getStringExtra(EXTRA_PATIENT_NAME)
        val patientGender = intent.getStringExtra(EXTRA_PATIENT_GENDER)

        // Normalisasi label klasifikasi untuk display
        val prettyClass = normalizeForDisplay(rawClass)

        // Isi otomatis details/tindakan jika kosong
        val (finalDetails, finalAction) = if (passedDetails.isBlank() && passedAction.isBlank()) {
            mapDetailsAction(rawClass)
        } else passedDetails to passedAction

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            ULCARETheme {
                ResultScreen(
                    imageUri       = imageUri,
                    classification = prettyClass,
                    details        = finalDetails,
                    tindakan       = finalAction,
                    patientName    = patientName,
                    patientGender  = patientGender,
                    onBack         = { finish() }
                )
            }
        }
    }
}


/**
 * Label dari model (contoh: "light", "light-medium", "Medium - Urgent", dsb)
 * lalu kembalikan pair Details & Tindakan sesuai tabel yang diminta.
 */
private fun mapDetailsAction(classificationRaw: String): Pair<String, String> {
    val key = normalizeKey(classificationRaw)
    return when (key) {
        "light" -> (
                "Ulkus sangat superfisial (hanya di lapisan kulit atas/epidermis), ukuran sangat kecil, bersih, tidak ada infeksi."
                ) to (
                "1. Perawatan Kaki Harian: Cek kaki, cuci dan keringkan. Gunakan pelembab. Jangan bertelanjang kaki.\n" +
                        "2. Kontrol ke dokter/perawat luka dalam 1-2 bulan."
                )

        "light-medium" -> (
                "Ulkus superfisial. Ulkus terbatas pada kulit (dermis/epidermis), belum mencapai tendon/tulang, tidak ada tanda infeksi"
                ) to (
                "1. Bersihkan Luka: Cuci lembut dengan cairan steril (saline) dan balut dengan balutan modern (misalnya hydrogel untuk melembabkan).\n" +
                        "2. Konsultasi ke dokter/perawat luka dalam 1-2 minggu."
                )

        "medium" -> (
                "Infeksi Lokal Ringan. Ulkus mencapai jaringan subkutan (lemak), mungkin terlihat tendon/ligamen TETAPI tidak terinfeksi tulang atau sendi"
                ) to (
                "1. Wajib konsultasi, JANGAN LAKUKAN Perawatan Mandiri.\n" +
                        "2. Kontrol gula darah secara KETAT\n" +
                        "3. Segera Konsultasi ke Dokter/Klinik untuk membuang jaringan mati dan penanganan infeksi"
                )

        "medium-urgent" -> (
                "Ulkus Dalam dan Terinfeksi Sedang/Berat atau Ancaman Jaringan"
                ) to (
                "1. Rujuk ke Spesialis. Segera konsultasi dengan tim multidisiplin (Endokrinolog, Dokter Bedah Vaskular, Perawat Luka).\n" +
                        "2. Diperlukan tindakan pembersihan luka yang lebih agresif.\n" +
                        "3. Terapi Antibiotik secara KETAT"
                )

        "urgent" -> (
                "Gangren (Kematian Jaringan), Infeksi Sistemik (Sepsis), atau Iskemia Berat."
                ) to (
                "1. Keadaan Darurat Medis (Emergency). Segera bawa ke UGD TERDEKAT dan Rawat Inap\n" +
                        "2. Amputasi mungkin diperlukan untuk menyelamatkan nyawa / anggota gerak"
                )

        else -> "" to "" // jika label di luar daftar, biarkan kosong (sesuai permintaan)
    }
}

/** Ubah variasi label ke kunci konsisten untuk mapping. */
private fun normalizeKey(s: String): String {
    return s
        .trim()
        .lowercase()
        .replace('—', '-') // em dash
        .replace('–', '-') // en dash
        .replace(" ", "")  // hilangkan spasi
}

/** Ubah label agar enak dibaca di UI (pakai spasi di sekitar tanda hubung). */
private fun normalizeForDisplay(s: String): String {
    val key = normalizeKey(s)
    return when (key) {
        "light" -> "Light"
        "light-medium" -> "Light - Medium"
        "medium" -> "Medium"
        "medium-urgent" -> "Medium - Urgent"
        "urgent" -> "Urgent"
        else -> s.ifBlank { "Unknown" }
    }
}

/* ===================== UI ===================== */

@Composable
private fun ResultScreen(
    imageUri: String?,
    classification: String,
    details: String,
    tindakan: String,
    patientName: String?,
    patientGender: String?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .background(MaterialTheme.colorScheme.background)  // Latar belakang putih
        ) {
            // ---- Preview gambar (rounded) ----
            item {
                if (!imageUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Result Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text("Image", style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ---- Identitas Pasien ----
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nama: ${patientName ?: "-"}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    val genderText = when (patientGender?.uppercase()) {
                        "L" -> "Laki-laki"
                        "P" -> "Perempuan"
                        else -> "-"
                    }
                    Text("Jenis kelamin: $genderText", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- Klasifikasi ----
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Klasifikasi luka:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (classification.isBlank()) "—" else classification,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // ---- Details ----
            item {
                Text("Details:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(text = if (details.isBlank()) "—" else details)
                Spacer(Modifier.height(24.dp))
            }

            // ---- Tindakan ----
            item {
                Text("Tindakan:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(text = if (tindakan.isBlank()) "—" else tindakan)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/*  Preview */

@Preview(showBackground = true, name = "Result – Preview")
@Composable
private fun ResultPreview() {
    ULCARETheme {
        ResultScreen(
            imageUri = null,
            classification = "Medium - Urgent",
            details = "Ulkus Dalam dan Terinfeksi Sedang/Berat atau Ancaman Jaringan",
            tindakan = "1. Rujuk ke Spesialis. Segera konsultasi dengan tim multidisiplin (Endokrinolog, Dokter Bedah Vaskular, Perawat Luka).\n2. Diperlukan tindakan pembersihan luka yang lebih agresif.\n3. Terapi Antibiotik secara KETAT",
            patientName = "Budi",
            patientGender = "L",
            onBack = {}
        )
    }
}
