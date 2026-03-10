package com.example.braingames.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.braingames.R

val Orange = androidx.compose.ui.graphics.Color(0xFFFFA500)
val Black = Color(0xFF000000)

@Composable
fun AppTypography(): Typography {
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val montserrat = FontFamily(
        Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider, weight = FontWeight.Bold),
        Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider, weight = FontWeight.Black)
    )

    return Typography(
        headlineLarge = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Black, fontSize = 32.sp, color = Black),
        headlineMedium = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Black),
        titleLarge = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Black),
        titleMedium = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Black),
        bodyLarge = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Black),
        bodyMedium = TextStyle(fontFamily = montserrat, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Black)
    )
}