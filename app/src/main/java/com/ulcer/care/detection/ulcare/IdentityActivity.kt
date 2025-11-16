@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ulcer.care.detection.ulcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.RadioButtonDefaults.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

class IdentityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tidak menggunakan ActionBar (layout full Compose)
        supportActionBar?.hide()

        setContent {
            ULCARETheme {
                IdentityScreen(
                    onSubmit = { name, gender ->
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_PATIENT_NAME, name)
                                putExtra(MainActivity.EXTRA_PATIENT_GENDER, gender) // "L" atau "P"
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun IdentityScreen(
    onSubmit: (String, String) -> Unit,
    defaultName: String = "",
    defaultGender: String? = null
) {
    var name by remember { mutableStateOf(defaultName) }
    var gender by remember { mutableStateOf(defaultGender) } // "L" / "P"
    var error by remember { mutableStateOf<String?>(null) }

    // Warna utama yang dipakai
    val black = Color(0xFF000000)
    val surfaceBg = Color(0xFFF6F7F8)
    val ulcareGreen = Color(0xFF3DB3AE)

    Surface(color = surfaceBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Headline
            Spacer(Modifier.height(8.dp))
            Text(
                "Selamat Datang ke ULCare",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = black
            )
            Spacer(Modifier.height(6.dp))

            // Kartu "Biodata Diri"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Biodata Diri", fontWeight = FontWeight.Bold, color = black)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Nama Lengkap", color = black.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = black,
                            unfocusedTextColor = black,
                            disabledTextColor = black,
                            cursorColor = black,
                            focusedBorderColor = black,
                            unfocusedBorderColor = black,
                            focusedLabelColor = black,
                            unfocusedLabelColor = black
                        )
                    )
                }
            }

            // Kartu "Jenis Kelamin" -> FULL WIDTH
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Jenis Kelamin", fontWeight = FontWeight.SemiBold, color = black)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == "L",
                                onClick = { gender = "L" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = black,
                                    unselectedColor = black.copy(alpha = 0.5f),
                                    disabledSelectedColor = black.copy(alpha = 0.3f),
                                    disabledUnselectedColor = black.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Laki-laki", color = black)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == "P",
                                onClick = { gender = "P" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = black,
                                    unselectedColor = black.copy(alpha = 0.5f),
                                    disabledSelectedColor = black.copy(alpha = 0.3f),
                                    disabledUnselectedColor = black.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Perempuan", color = black)
                        }
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            // --- Tombol Selanjutnya ---
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            Button(
                onClick = {
                    when {
                        name.isBlank() -> error = "Nama wajib diisi."
                        gender == null -> error = "Pilih jenis kelamin."
                        else -> {
                            error = null
                            onSubmit(name.trim(), gender!!)
                        }
                    }
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPressed) Color.White else ulcareGreen,
                    contentColor = if (isPressed) ulcareGreen else Color.White
                ),
                border = if (isPressed) BorderStroke(2.dp, ulcareGreen) else null
            ) {
                Text("Selanjutnya", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/*  PREVIEWS  */

@Preview(showBackground = true, name = "Identity – Kosong")
@Composable
private fun PreviewIdentity_Empty() {
    ULCARETheme {
        IdentityScreen(
            onSubmit = { _, _ -> },
            defaultName = "",
            defaultGender = null
        )
    }
}

@Preview(showBackground = true, name = "Identity – Terisi (Contoh)")
@Composable
private fun PreviewIdentity_Filled() {
    ULCARETheme {
        IdentityScreen(
            onSubmit = { _, _ -> },
            defaultName = "Rika Rachmawati",
            defaultGender = "P"
        )
    }
}