package com.ulcer.care.detection.ulcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ulcer.care.detection.ulcare.ui.theme.ULCARETheme

class SplashScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash system bawaan (Android 12+) tetap dipasang dulu
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Tampilkan logo di tengah layar (ukuran proporsional & besar)
        setContent {
            ULCARETheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ulcare_logo),
                        contentDescription = "ULCARE Logo",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)   // 50% lebar layar
                            .aspectRatio(1f)      // tetap proporsional (persegi)
                    )
                }
            }
        }

        // Tunda 2.2 detik sambil menampilkan logo
        lifecycleScope.launch {
            delay(2200)

            val next = if (UserPrefs.hasIdentity(this@SplashScreenActivity)) {
                MainActivity::class.java
            } else {
                IdentityActivity::class.java
            }

            startActivity(Intent(this@SplashScreenActivity, next))
            finish()
        }
    }
}
