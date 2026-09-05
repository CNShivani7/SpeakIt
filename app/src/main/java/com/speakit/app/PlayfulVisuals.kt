package com.speakit.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

val lessonColors = listOf(Color(0xFF7153D6), Color(0xFF007C91), Color(0xFFC45624), Color(0xFF267B4C), Color(0xFFAB3982), Color(0xFF356CC0))

/** Original code-drawn speech buddy; no third-party character artwork. */
@Composable fun SpeechBuddy(accent: Color, listening: Boolean, happy: Boolean) {
    val transition = rememberInfiniteTransition(label = "speech buddy")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if(listening) 950 else 2400, easing = LinearEasing)), label = "buddy motion")
    Canvas(Modifier.size(88.dp).graphicsLayer {
        translationY = sin(phase * 6.283f) * 5f
        rotationZ = sin(phase * 6.283f) * if(happy) 7f else 2f
    }) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFFFD768), w * .48f, Offset(w*.5f,h*.5f))
        drawRoundRect(accent, Offset(w*.16f,h*.18f), Size(w*.68f,h*.60f), CornerRadius(w*.22f))
        drawCircle(accent,w*.10f,Offset(w*.29f,h*.75f))
        val blink = phase > .93f && phase < .985f
        for(x in listOf(.38f,.63f)) {
            if(blink) drawLine(Color.White,Offset(w*(x-.055f),h*.41f),Offset(w*(x+.055f),h*.41f),w*.045f,StrokeCap.Round)
            else drawOval(Color.White,Offset(w*(x-.045f),h*.34f),Size(w*.09f,h*.14f))
        }
        if(listening) drawOval(Color(0xFFFFE0CB),Offset(w*.46f,h*.54f),Size(w*.09f,h*(.06f+.045f*phase)))
        else drawArc(Color.White,0f,180f,false,Offset(w*.38f,h*.46f),Size(w*.25f,h*.16f),style=Stroke(w*.035f,cap=StrokeCap.Round))
        drawCircle(Color(0xFFFF9ABC),w*.045f,Offset(w*.29f,h*.51f))
        drawCircle(Color(0xFFFF9ABC),w*.045f,Offset(w*.74f,h*.51f))
        if(happy) {
            val colors = listOf(Color(0xFFEC6FA3),Color(0xFF2DBBA0),Color(0xFF7153D6),Color(0xFFFFA423))
            for(i in 0..7) {
                val x = ((i*0.137f+.04f)%1f)*w
                val y = ((phase+i*.12f)%1f)*h
                drawCircle(colors[i%4],w*.025f,Offset(x,y))
            }
        }
    }
}
