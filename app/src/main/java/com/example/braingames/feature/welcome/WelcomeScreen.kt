package com.example.braingames.feature.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.braingames.ui.theme.Orange
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WelcomeScreen(onStart: () -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9DEE3))
    ) {

        /*
        -------------------------
        БОЛЬШАЯ ЗВЕЗДА СПРАВА
        -------------------------
        */

        Star(
            color = Color(0xFFE7C38B),
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-140).dp)
        )

        Star(
            color = Color(0xFFE7C38B),
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = (240).dp)
        )

        Star(
            color = Color(0xFFE7C38B),
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = 90.dp, y = (480).dp)
        )

        /*
        -------------------------
        МАЛЕНЬКИЕ КОНТУРНЫЕ
        -------------------------
        */

        StarOutline(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopStart)
                .offset(x = 100.dp, y = 120.dp)
        )

        StarOutline(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-60).dp, y = (-180).dp)
        )

        /*
        -------------------------
        ЗОЛОТЫЕ ЗВЕЗДЫ
        -------------------------
        */

//        Row(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .offset(y = (-60).dp),
//            horizontalArrangement = Arrangement.spacedBy(18.dp)
//        ) {
//            repeat(6) {
//                Star(
//                    color = Color(0xFFD9B36C),
//                    modifier = Modifier.size(26.dp)
//                )
//            }
//        }

        Star(
            color = Color(0xFFD9B36C),
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.TopStart)
                .offset(x = 60.dp, y = (320).dp)
        )

        Star(
            color = Color(0xFFD9B36C),
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.TopStart)
                .offset(x = 120.dp, y = (300).dp)
        )

        Star(
            color = Color(0xFFD9B36C),
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.TopStart)
                .offset(x = 180.dp, y = (320).dp)
        )

        /*
        -------------------------
        ОСНОВНОЙ КОНТЕНТ
        -------------------------
        */

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Vimo",
                fontSize = 104.sp,
                fontWeight = FontWeight.Black,
                color = Orange
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .fillMaxWidth()
                    .height(70.dp)
                    .shadow(10.dp, RoundedCornerShape(40.dp)),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange
                )
            ) {

                Text(
                    text = "Поехали!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun Star(
    modifier: Modifier = Modifier,
    color: Color
) {

    Canvas(modifier = modifier) {

        val path = Path()

        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius / 2.5

        val centerX = size.width / 2
        val centerY = size.height / 2

        val points = 5

        for (i in 0 until points * 2) {

            val angle = Math.PI / points * i
            val radius = if (i % 2 == 0) outerRadius else innerRadius

            val x = centerX + (cos(angle) * radius.toFloat())
            val y = centerY + (sin(angle) * radius.toFloat())

            if (i == 0)
                path.moveTo(x.toFloat(), y.toFloat())
            else
                path.lineTo(x.toFloat(), y.toFloat())
        }

        path.close()

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}

@Composable
fun StarOutline(
    modifier: Modifier = Modifier
) {

    Canvas(modifier = modifier) {

        val path = Path()

        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius / 2.5

        val centerX = size.width / 2
        val centerY = size.height / 2

        val points = 4

        for (i in 0 until points * 2) {

            val angle = Math.PI / points * i
            val radius = if (i % 2 == 0) outerRadius else innerRadius

            val x = centerX + (cos(angle) * radius.toFloat())
            val y = centerY + (sin(angle) * radius.toFloat())

            if (i == 0)
                path.moveTo(x.toFloat(), y.toFloat())
            else
                path.lineTo(x.toFloat(), y.toFloat())
        }

        path.close()

        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 3f)
        )
    }
}