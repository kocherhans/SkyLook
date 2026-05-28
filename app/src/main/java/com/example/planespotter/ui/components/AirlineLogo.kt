package com.example.planespotter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.planespotter.ui.theme.SkyBlue

@Composable
fun AirlineLogo(
    iataCode: String,
    airlineName: String,
    size: Dp = 44.dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    // Reset to avatar whenever the displayed flight changes
    var useAvatar by remember(iataCode) { mutableStateOf(iataCode.isEmpty()) }
    val shape = RoundedCornerShape(cornerRadius)

    if (useAvatar) {
        AirlineLetterAvatar(airlineName, size, cornerRadius, modifier)
    } else {
        AsyncImage(
            model = "https://images.kiwi.com/airlines/64/$iataCode.png",
            contentDescription = airlineName,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.07f)),
            onError = { _ -> useAvatar = true }
        )
    }
}

@Composable
fun AirlineLetterAvatar(
    airlineName: String,
    size: Dp = 44.dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val letter = airlineName.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SkyBlue.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = SkyBlue,
            fontWeight = FontWeight.W700,
            fontSize = (size.value * 0.44f).sp
        )
    }
}
