package com.dmwnezes.leao

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Azul = Color(0xFF0A3D91)
val AzulClaro = Color(0xFF3B7BFF)
val Vermelho = Color(0xFFE1251B)
val Noite = Color(0xFF040B1C)
val Verde = Color(0xFF34D399)
val Amarelo = Color(0xFFFACC15)
val TextoSuave = Color.White.copy(alpha = 0.62f)

val tricolor = Brush.horizontalGradient(
    0f to Vermelho, 0.33f to Vermelho, 0.33f to Color.White, 0.66f to Color.White, 0.66f to AzulClaro, 1f to AzulClaro,
)

/** Fundo escuro com manchas de luz azul e vermelha; é o que aparece "atrás do vidro". */
fun Modifier.fundoTricolor() = background(Noite).drawBehind {
    val w = size.width
    val h = size.height
    fun mancha(cor: Color, x: Float, y: Float, r: Float) =
        drawCircle(Brush.radialGradient(listOf(cor, Color.Transparent), Offset(x, y), r), r, Offset(x, y))
    mancha(Azul.copy(alpha = 0.95f), w * 0.05f, h * 0.05f, w * 1.0f)
    mancha(Vermelho.copy(alpha = 0.55f), w * 1.05f, h * 0.38f, w * 0.85f)
    mancha(AzulClaro.copy(alpha = 0.45f), w * 0.1f, h * 0.78f, w * 0.9f)
    mancha(Vermelho.copy(alpha = 0.30f), w * 0.9f, h * 1.0f, w * 0.7f)
}

/** Painel de vidro: preenchimento translúcido em degradê e borda clara que some na diagonal. */
fun Modifier.vidro(raio: Dp = 24.dp, tom: Color = Color.White, intensidade: Float = 1f): Modifier {
    val forma = RoundedCornerShape(raio)
    return clip(forma)
        .background(Brush.linearGradient(listOf(tom.copy(alpha = 0.16f * intensidade), tom.copy(alpha = 0.05f * intensidade))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.04f))), forma)
}

@Composable
fun Etiqueta(texto: String, cor: Color = Color.White, fundo: Color = Color.White.copy(alpha = 0.12f), pulso: Boolean = false) {
    Row(
        Modifier.clip(CircleShape).background(fundo).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pulso) {
            val a by rememberInfiniteTransition(label = "pulso").animateFloat(
                1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a",
            )
            Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(Color.White))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, color = cor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
fun TituloSecao(texto: String) {
    Text(
        texto.uppercase(), color = TextoSuave, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}
