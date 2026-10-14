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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Azul = Color(0xFF0A3D91)
val AzulClaro = Color(0xFF3B7BFF)
val Vermelho = Color(0xFFE1251B)
val Noite = Color(0xFF0E1013)
val Cartao = Color(0xFF191C21)
val Borda = Color(0xFF2A2E35)
val Verde = Color(0xFF34D399)
val Amarelo = Color(0xFFFACC15)
val TextoSuave = Color.White.copy(alpha = 0.62f)

val tricolor = Brush.horizontalGradient(
    0f to Vermelho, 0.33f to Vermelho, 0.33f to Color.White, 0.66f to Color.White, 0.66f to AzulClaro, 1f to AzulClaro,
)

fun Modifier.fundo() = background(Noite)

/** Cartão sólido grafite com borda discreta; `tom` tinge o cartão (ex.: vermelho no jogo ao vivo). */
fun Modifier.painel(raio: Dp = 24.dp, tom: Color = Color.White, intensidade: Float = 1f): Modifier {
    val forma = RoundedCornerShape(raio)
    val cor = if (tom == Color.White) Color.White.copy(alpha = 0.04f * (intensidade - 1f).coerceAtLeast(0f)).compositeOver(Cartao)
    else tom.copy(alpha = 0.16f * intensidade).compositeOver(Cartao)
    return clip(forma).background(cor).border(1.dp, Borda, forma)
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
