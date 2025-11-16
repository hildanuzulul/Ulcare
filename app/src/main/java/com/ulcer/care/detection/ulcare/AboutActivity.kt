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
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
private const val DEV_LINE = "PR Kesmaszi BRIN - ITSK Soepraoen"
private const val EMAIL_RIKA = "rika016@brin.go.id"
private const val EMAIL_ANSHORI = "moanshori@itsk-soepraoen.ac.id"

/* -------------------- UI -------------------- */

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionName = remember {
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
            Spacer(Modifier.height(4.dp))

            ContactNameOnly(name = "Rika Rachmawati", emailTo = EMAIL_RIKA)
            ContactNameOnly(name = "Mochammad Anshori", emailTo = EMAIL_ANSHORI)

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // === Acknowledgment ===
            Text(
                "Acknowledgment",
                style = MaterialTheme.typography.bodyLarge,
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
private fun ContactNameOnly(name: String, emailTo: String) {
    val ctx = LocalContext.current
    Text(
        text = name,
        style = MaterialTheme.typography.bodySmall.merge(
            TextStyle(textDecoration = TextDecoration.Underline)
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clickable { openEmail(ctx, emailTo) }
    )
}

private fun openEmail(context: android.content.Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
    }
    context.startActivity(Intent.createChooser(intent, "Kirim email ke $email"))
}

/* -------------------- Preview -------------------- */
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
