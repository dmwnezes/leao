package com.dmwnezes.leao

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Azul = Color(0xFF0A3D91)
private val Vermelho = Color(0xFFE1251B)
private val Fundo = Color(0xFFF4F6FA)
private val Suave = Color(0xFF6B7280)

private val tricolor = Brush.horizontalGradient(
    0f to Vermelho, 0.33f to Vermelho, 0.33f to Color.White, 0.66f to Color.White, 0.66f to Azul, 1f to Azul,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Azul, secondary = Vermelho, background = Fundo, surface = Color.White,
                    secondaryContainer = Azul.copy(alpha = 0.12f),
                ),
            ) { App() }
        }
    }
}

@Composable
private fun App() {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf<String?>(null) }
    BackHandler(aberto != null) { aberto = null }

    Scaffold(
        containerColor = Fundo,
        topBar = { Topo() },
        bottomBar = {
            if (aberto == null) NavigationBar(containerColor = Color.White) {
                NavigationBarItem(aba == 0, { aba = 0 }, icon = { Text("⚽", fontSize = 20.sp) }, label = { Text("Jogos") })
                NavigationBarItem(aba == 1, { aba = 1 }, icon = { Text("📰", fontSize = 20.sp) }, label = { Text("Notícias") })
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                aberto != null -> TelaPartida(aberto!!) { aberto = null }
                aba == 0 -> TelaJogos { aberto = it }
                else -> TelaNoticias()
            }
        }
    }
}

@Composable
private fun Topo() {
    Column(Modifier.background(Azul).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(logoFortaleza, null, Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Leão", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Fortaleza Esporte Clube", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(tricolor))
    }
}

private const val logoFortaleza = "https://a.espncdn.com/i/teamlogos/soccer/500/$FORTALEZA_ID.png"

// ---------- Jogos ----------

@Composable
private fun TelaJogos(abrir: (String) -> Unit) {
    var jogos by remember { mutableStateOf<List<Jogo>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(recarga) {
        erro = null
        runCatching { withContext(Dispatchers.IO) { carregarJogos() } }
            .onSuccess { jogos = it }.onFailure { erro = "Sem conexão com a ESPN" }
        // Enquanto houver jogo rolando, atualiza o placar a cada 30 s.
        while (true) {
            val agora = Instant.now()
            val vivos = jogos.orEmpty().filter { possivelmenteAoVivo(it, agora) }
            if (vivos.isEmpty()) break
            for (v in vivos) {
                runCatching { withContext(Dispatchers.IO) { carregarPartida(v.id).jogo } }
                    .onSuccess { novo -> jogos = jogos?.map { if (it.id == novo.id) novo.copy(competicao = it.competicao, estadio = it.estadio) else it } }
            }
            delay(30_000)
        }
    }

    val lista = jogos
    if (lista == null) { Carregando(erro) { recarga++ }; return }

    val agora = Instant.now()
    val aoVivo = lista.firstOrNull { it.estado == "in" }
    val proximos = lista.filter { it.estado == "pre" && it.inicio.isAfter(agora.minus(Duration.ofHours(3))) }
    val resultados = lista.filter { it.estado == "post" }.reversed()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (erro != null) item { Aviso(erro!!) { recarga++ } }
        if (aoVivo != null) item { CartaoDestaque("AO VIVO", aoVivo, vivo = true) { abrir(aoVivo.id) } }
        proximos.firstOrNull()?.takeIf { aoVivo == null }?.let { p -> item { CartaoDestaque("PRÓXIMO JOGO", p) { abrir(p.id) } } }
        resultados.firstOrNull()?.let { r -> item { CartaoDestaque("ÚLTIMO JOGO", r) { abrir(r.id) } } }
        secao("Próximos jogos", proximos.drop(if (aoVivo == null) 1 else 0), abrir)
        secao("Resultados", resultados.drop(1).take(20), abrir)
        item {
            TextButton({ recarga++ }, Modifier.fillMaxWidth()) { Text("Atualizar") }
        }
    }
}

private fun possivelmenteAoVivo(j: Jogo, agora: Instant) =
    j.estado == "in" || (j.estado == "pre" && !j.inicio.isAfter(agora.plusSeconds(600)) && j.inicio.isAfter(agora.minus(Duration.ofHours(4))))

private fun LazyListScope.secao(titulo: String, jogos: List<Jogo>, abrir: (String) -> Unit) {
    if (jogos.isEmpty()) return
    item { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp)) }
    items(jogos, key = { titulo + it.id }) { LinhaJogo(it) { abrir(it.id) } }
}

@Composable
private fun CartaoDestaque(rotulo: String, j: Jogo, vivo: Boolean = false, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (vivo) Azul else Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        val cor = if (vivo) Color.White else Color.Black
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vivo) Pulso()
                Text(rotulo, color = if (vivo) Color.White else Vermelho, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
            Text("${j.competicao}${j.estadio?.let { " · $it" } ?: ""}", color = cor.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Placar(j, cor)
        }
    }
}

@Composable
private fun Placar(j: Jogo, cor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Escudo(j.casa, cor, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            if (j.golsCasa != null) {
                Text("${j.golsCasa}  ×  ${j.golsFora}", color = cor, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(j.relogio, color = cor.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(hora(j.inicio), color = cor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(dia(j.inicio), color = cor.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(contagem(j.inicio), color = Vermelho, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Escudo(j.fora, cor, Modifier.weight(1f))
    }
}

@Composable
private fun Escudo(t: Time, cor: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(t.logo, t.nome, Modifier.size(56.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            t.nome, color = cor, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2,
            fontWeight = if (t.id == FORTALEZA_ID) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun LinhaJogo(j: Jogo, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(64.dp)) {
                Text(DateTimeFormatter.ofPattern("dd/MM", ptBR).format(j.inicio.atZone(fuso)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(j.competicao, color = Suave, fontSize = 10.sp, maxLines = 2, lineHeight = 12.sp)
            }
            Column(Modifier.weight(1f)) {
                LinhaTime(j.casa, j.golsCasa)
                LinhaTime(j.fora, j.golsFora)
            }
            Text(
                if (j.estado == "pre") hora(j.inicio) else resultado(j) ?: j.relogio,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = when (resultado(j)) { "V" -> Color(0xFF15803D); "D" -> Vermelho; else -> Suave },
            )
        }
    }
}

@Composable
private fun LinhaTime(t: Time, gols: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        AsyncImage(t.logo, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(t.nome, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (t.id == FORTALEZA_ID) FontWeight.Bold else FontWeight.Normal)
        if (gols != null) Text(gols, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
    }
}

/** V / E / D do ponto de vista do Fortaleza; null se não terminou. */
fun resultado(j: Jogo): String? {
    if (j.estado != "post") return null
    val c = j.golsCasa?.toIntOrNull() ?: return null
    val f = j.golsFora?.toIntOrNull() ?: return null
    val nosso = if (j.casa.id == FORTALEZA_ID) c - f else f - c
    return when { nosso > 0 -> "V"; nosso < 0 -> "D"; else -> "E" }
}

// ---------- Partida (lances) ----------

@Composable
private fun TelaPartida(id: String, voltar: () -> Unit) {
    var partida by remember { mutableStateOf<Partida?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }

    LaunchedEffect(id, recarga) {
        while (true) {
            runCatching { withContext(Dispatchers.IO) { carregarPartida(id) } }
                .onSuccess { partida = it; erro = null }.onFailure { erro = "Sem conexão com a ESPN" }
            val p = partida ?: break
            if (!possivelmenteAoVivo(p.jogo, Instant.now())) break
            delay(30_000)
        }
    }

    val p = partida
    if (p == null) { Carregando(erro) { recarga++ }; return }
    val vivo = p.jogo.estado == "in"

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(voltar) { Text("← Voltar") } }
        if (erro != null) item { Aviso(erro!!) { recarga++ } }
        item { CartaoDestaque(if (vivo) "AO VIVO" else if (p.jogo.estado == "pre") "PRÓXIMO JOGO" else "ENCERRADO", p.jogo, vivo) {} }
        item {
            Text("Lances", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
            if (vivo) Text("Atualiza sozinho a cada 30 s", color = Suave, fontSize = 12.sp)
        }
        if (p.lances.isEmpty()) item {
            Text(if (p.jogo.estado == "pre") "A bola ainda não rolou." else "Nenhum lance registrado ainda.", color = Suave)
        }
        // Mais recente primeiro durante o jogo; em ordem depois.
        items(if (vivo) p.lances.reversed() else p.lances) { LinhaLance(it, p.jogo) }
    }
}

@Composable
private fun LinhaLance(l: Lance, j: Jogo) {
    val nosso = l.time == j.casa.nome && j.casa.id == FORTALEZA_ID || l.time == j.fora.nome && j.fora.id == FORTALEZA_ID ||
        l.time == "Fortaleza"
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (l.titulo.startsWith("Gol") && nosso) Azul.copy(alpha = 0.08f) else Color.White),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l.minuto, Modifier.width(52.dp), fontWeight = FontWeight.Bold, color = Azul, fontSize = 14.sp)
            Text(l.icone, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(listOfNotNull(l.titulo, l.time).joinToString(" · "), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                l.detalhe?.let { Text(it, color = Suave, fontSize = 13.sp) }
            }
        }
    }
}

// ---------- Notícias ----------

@Composable
private fun TelaNoticias() {
    var noticias by remember { mutableStateOf<List<Noticia>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    LaunchedEffect(recarga) {
        erro = null
        runCatching { withContext(Dispatchers.IO) { carregarNoticias() } }
            .onSuccess { noticias = it }.onFailure { erro = "Não deu para buscar as notícias" }
    }

    val lista = noticias
    if (lista == null) { Carregando(erro) { recarga++ }; return }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (erro != null) item { Aviso(erro!!) { recarga++ } }
        items(lista, key = { it.link }) { n ->
            Card(
                Modifier.fillMaxWidth().clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.link))) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(Vermelho))
                    Column(Modifier.padding(14.dp)) {
                        Text(n.titulo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(listOfNotNull(n.fonte, n.data?.let { tempoAtras(it) }).joinToString(" · "), color = Suave, fontSize = 12.sp)
                    }
                }
            }
        }
        item { TextButton({ recarga++ }, Modifier.fillMaxWidth()) { Text("Atualizar") } }
    }
}

// ---------- Comuns ----------

@Composable
private fun Carregando(erro: String?, tentar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (erro == null) CircularProgressIndicator()
        else Aviso(erro, tentar)
    }
}

@Composable
private fun Aviso(texto: String, tentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto, color = Vermelho)
        TextButton(tentar) { Text("Tentar de novo") }
    }
}

@Composable
private fun Pulso() {
    val a by rememberInfiniteTransition(label = "pulso").animateFloat(
        1f, 0.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a",
    )
    Box(Modifier.padding(end = 6.dp).size(8.dp).alpha(a).clip(CircleShape).background(Vermelho))
}

private val ptBR = Locale("pt", "BR")
private val fuso get() = ZoneId.systemDefault()
private fun hora(i: Instant) = DateTimeFormatter.ofPattern("HH:mm").format(i.atZone(fuso))
private fun dia(i: Instant) = DateTimeFormatter.ofPattern("EEE, dd/MM", ptBR).format(i.atZone(fuso))

fun contagem(i: Instant, agora: Instant = Instant.now(), zona: ZoneId = fuso): String {
    val d = Duration.between(agora, i)
    val dias = ChronoUnit.DAYS.between(agora.atZone(zona).toLocalDate(), i.atZone(zona).toLocalDate())
    return when {
        d.isNegative -> "começando"
        dias >= 2 -> "em $dias dias"
        dias == 1L -> "amanhã"
        d.toHours() >= 1 -> "hoje, em ${d.toHours()} h"
        else -> "em ${d.toMinutes()} min"
    }
}

fun tempoAtras(i: Instant, agora: Instant = Instant.now()): String {
    val d = Duration.between(i, agora)
    return when {
        d.toMinutes() < 60 -> "há ${d.toMinutes().coerceAtLeast(1)} min"
        d.toHours() < 24 -> "há ${d.toHours()} h"
        d.toDays() == 1L -> "ontem"
        else -> "há ${d.toDays()} dias"
    }
}
