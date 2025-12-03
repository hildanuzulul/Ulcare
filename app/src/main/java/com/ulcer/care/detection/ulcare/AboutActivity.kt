@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // ActionBar: judul & ikon back hitam, background putih
        val white = Color.parseColor("#FFFFFF")
        val titleSpan = SpannableString("About").apply {
            setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            val up = AppCompatResources.getDrawable(
                this@AboutActivity,
                R.drawable.ic_arrow_back_24
            )?.mutate()
            up?.setTint(Color.BLACK)
            setHomeAsUpIndicator(up)

            setBackgroundDrawable(ColorDrawable(white))
            title = titleSpan
        }

        // Status bar terang (ikon gelap) – konsisten dengan tema putih
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true

        setContent {
            ULCARETheme {
                // Surface utama: background hampir putih, sama seperti IdentityActivity
                androidx.compose.material3.Surface(
                    color = ComposeColor(0xFFF6F7F8)
                ) {
                    AboutScreen()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

/* ------------ Konstanta konten ------------ */
private const val DEV_LINE = "BRIN x ITSK Soepraoen"
private const val EMAIL_ANSHORI = "moanshori@itsk-soepraoen.ac.id"
private const val EMAIL_RIKA = "rika016@brin.go.id"

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionName = remember  {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrElse { "1.0" }
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp)
        ) {
            // === Pengembang ===
            Text(
                "Pengembang",
                style = MaterialTheme.typography.bodyLarge,
                color = ComposeColor.Black
            )
            Text(
                DEV_LINE,
                style = MaterialTheme.typography.bodySmall,
                color = ComposeColor.Black
            )
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // === Kontak (nama saja & bisa diklik) ===
            Text(
                "Kontak",
                style = MaterialTheme.typography.bodyLarge,
                color = ComposeColor.Black
            )
            ContactWithEmail(name = "Rika Rachmawati", email = EMAIL_RIKA)
            ContactWithEmail(name = "Mochammad Anshori", email = EMAIL_ANSHORI)

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

//            hubungi kami
//            ElevatedCard(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { openWhatsApp(context) } // Fungsi untuk membuka WhatsApp
//                    .padding(16.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text(
//                    text = "Hubungi Kami",
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.padding(16.dp),
//                    fontWeight = FontWeight.SemiBold,
//                    textAlign = TextAlign.Center
//                )
//            }

            // === Hubungi Kami ===
            Text(
                "Hubungi Kami",
                style = MaterialTheme.typography.bodyLarge,
                color = ComposeColor.Black
            )

            Text(
                text = "WhatsApp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { openWhatsApp(context) }
            )

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // === Acknowledgment ===
            Text(
                "Acknowledgment",
                style = MaterialTheme.typography.bodyLarge,
                color = ComposeColor.Black
            )
            Text("Kolaborasi Riset BRIN dengan ITSK Soepraoen",
                style = MaterialTheme.typography.bodySmall,
                color = ComposeColor.Black
            )

            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Versi $versionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = ComposeColor.Black
                )
                Text(
                    "© 2025 ULCARE",
                    style = MaterialTheme.typography.labelMedium,
                    color = ComposeColor.Black
                )
            }
        }
    }
}

@Composable
fun ContactWithEmail(name: String, email: String) {
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
    ) {
        // Nama
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = ComposeColor.Black
        )

        // Email (klik, tanpa underline)
        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { openEmail(ctx, email) }
        )
    }
}

private fun openEmail(context: android.content.Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
    }
    context.startActivity(Intent.createChooser(intent, "Kirim email ke $email"))
}

private fun openWhatsApp(context: android.content.Context) {
    val phoneNumber = "6285258346842"
    val text = "Hallo, saya ingin lebih tahu mengenai aplikasi ULCare"
    val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(text)}"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
private fun PrevAbout() {
    ULCARETheme {
        androidx.compose.material3.Surface(
            color = ComposeColor(0xFFF6F7F8)
        ) {
            AboutScreen()
        }
    }
}
