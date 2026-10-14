package com.dmwnezes.leao

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color.White, background = Noite, surface = Noite,
                    onBackground = Color.White, onSurface = Color.White,
                ),
            ) {
                ProvideTextStyle(LocalTextStyle.current.copy(color = Color.White)) { App() }
            }
        }
    }
}

@Composable
private fun App() {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var aberto by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(aberto != null) { aberto = null }

    Box(Modifier.fillMaxSize().fundoTricolor()) {
        Column(Modifier.fillMaxSize()) {
            Topo()
            Box(Modifier.weight(1f)) {
                when {
                    aberto != null -> TelaPartida(aberto!!) { aberto = null }
                    aba == 0 -> TelaJogos { aberto = it }
                    else -> TelaNoticias()
                }
            }
        }
        if (aberto == null) {
            BarraAbas(aba, { aba = it }, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Topo() {
    Row(
        Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).vidro(16.dp), contentAlignment = Alignment.Center) {
            AsyncImage(logoFortaleza, "Fortaleza", Modifier.size(34.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Leão", fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 28.sp)
            Text("Fortaleza Esporte Clube", color = TextoSuave, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(34.dp).height(5.dp).clip(CircleShape).background(tricolor))
    }
}

@Composable
private fun BarraAbas(aba: Int, trocar: (Int) -> Unit, modifier: Modifier) {
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 48.dp, vertical = 14.dp)
            .vidro(30.dp, intensidade = 1.6f).background(Noite.copy(alpha = 0.35f)).padding(6.dp),
    ) {
        listOf("Jogos" to Icons.Rounded.SportsSoccer, "Notícias" to Icons.Rounded.Newspaper).forEachIndexed { i, (nome, icone) ->
            val ativo = aba == i
            Row(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (ativo) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { trocar(i) }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icone, null, tint = if (ativo) Color.White else TextoSuave, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(nome, color = if (ativo) Color.White else TextoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

private const val logoFortaleza = "https://a.espncdn.com/i/teamlogos/soccer/500/$FORTALEZA_ID.png"
private val espacoBarra = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)

// ---------- Jogos ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelaJogos(abrir: (String) -> Unit) {
    var jogos by remember { mutableStateOf<List<Jogo>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var atualizando by remember { mutableStateOf(false) }

    LaunchedEffect(recarga) {
        atualizando = jogos != null
        runCatching { withContext(Dispatchers.IO) { carregarJogos() } }
            .onSuccess { jogos = it; erro = null }.onFailure { erro = "Sem conexão com a ESPN" }
        atualizando = false
        // Enquanto houver jogo rolando, atualiza o placar a cada 30 s.
        while (true) {
            val vivos = jogos.orEmpty().filter { possivelmenteAoVivo(it, Instant.now()) }
            if (vivos.isEmpty()) break
            for (v in vivos) {
                runCatching { withContext(Dispatchers.IO) { carregarPartida(v.id).jogo } }
                    .onSuccess { novo -> jogos = jogos?.map { if (it.id == novo.id) novo.copy(competicao = it.competicao) else it } }
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
    val destaque = aoVivo ?: proximos.firstOrNull()

    PullToRefreshBox(atualizando, { recarga++ }) {
        LazyColumn(contentPadding = espacoBarra, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (erro != null) item { Aviso(erro!!) { recarga++ } }
            if (destaque != null) item { Destaque(destaque) { abrir(destaque.id) } }
            resultados.firstOrNull()?.let { r ->
                item { TituloSecao("Último jogo") }
                item { LinhaJogo(r) { abrir(r.id) } }
            }
            secao("Próximos jogos", proximos.filter { it != destaque }, abrir)
            secao("Resultados", resultados.drop(1).take(20), abrir)
        }
    }
}

private fun possivelmenteAoVivo(j: Jogo, agora: Instant) =
    j.estado == "in" || (j.estado == "pre" && !j.inicio.isAfter(agora.plusSeconds(600)) && j.inicio.isAfter(agora.minus(Duration.ofHours(4))))

private fun LazyListScope.secao(titulo: String, jogos: List<Jogo>, abrir: (String) -> Unit) {
    if (jogos.isEmpty()) return
    item { TituloSecao(titulo) }
    items(jogos, key = { titulo + it.id }) { LinhaJogo(it) { abrir(it.id) } }
}

@Composable
private fun Destaque(j: Jogo, onClick: (() -> Unit)? = null) {
    val vivo = j.estado == "in"
    Column(
        Modifier.fillMaxWidth()
            .vidro(28.dp, tom = if (vivo) Vermelho else Color.White, intensidade = if (vivo) 1.4f else 1f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (j.estado) {
                "in" -> Etiqueta("AO VIVO", fundo = Vermelho, pulso = true)
                "pre" -> Etiqueta("PRÓXIMO JOGO")
                else -> Etiqueta("ENCERRADO")
            }
            Spacer(Modifier.weight(1f))
            Text(j.competicao, color = TextoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Escudo(j.casa, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                if (j.golsCasa != null) {
                    Text("${j.golsCasa} – ${j.golsFora}", fontSize = 44.sp, fontWeight = FontWeight.Black, lineHeight = 46.sp)
                    Text(j.relogio, color = if (vivo) Color.White else TextoSuave, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(hora(j.inicio), fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
                    Text(dia(j.inicio), color = TextoSuave, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Etiqueta(contagem(j.inicio).uppercase(), fundo = Vermelho.copy(alpha = 0.85f))
                }
            }
            Escudo(j.fora, Modifier.weight(1f))
        }
        j.estadio?.let {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, tint = TextoSuave, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(it, color = TextoSuave, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Escudo(t: Time, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).vidro(36.dp), contentAlignment = Alignment.Center) {
            AsyncImage(t.logo, t.nome, Modifier.size(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t.nome, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2,
            fontWeight = if (t.id == FORTALEZA_ID) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun LinhaJogo(j: Jogo, onClick: () -> Unit) {
    val local = j.inicio.atZone(fuso)
    Row(
        Modifier.fillMaxWidth().vidro(20.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(local.dayOfMonth.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp)
            Text(DateTimeFormatter.ofPattern("MMM", ptBR).format(local).uppercase().trimEnd('.'), color = TextoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.15f)))
        Column(Modifier.weight(1f)) {
            LinhaTime(j.casa, j.golsCasa)
            LinhaTime(j.fora, j.golsFora)
            Text(j.competicao, color = TextoSuave, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(10.dp))
        when (val r = resultado(j)) {
            null -> Text(if (j.estado == "pre") hora(j.inicio) else j.relogio, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            else -> Box(
                Modifier.size(30.dp).clip(CircleShape).background(
                    when (r) { "V" -> Verde; "D" -> Vermelho; else -> Color.White.copy(alpha = 0.25f) },
                ),
                contentAlignment = Alignment.Center,
            ) { Text(r, fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (r == "V") Noite else Color.White) }
        }
    }
}

@Composable
private fun LinhaTime(t: Time, gols: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        AsyncImage(t.logo, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            t.nome, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (t.id == FORTALEZA_ID) FontWeight.Bold else FontWeight.Normal,
        )
        if (gols != null) Text(gols, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelaPartida(id: String, voltar: () -> Unit) {
    var partida by remember { mutableStateOf<Partida?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var atualizando by remember { mutableStateOf(false) }

    LaunchedEffect(id, recarga) {
        atualizando = partida != null
        while (true) {
            runCatching { withContext(Dispatchers.IO) { carregarPartida(id) } }
                .onSuccess { partida = it; erro = null }.onFailure { erro = "Sem conexão com a ESPN" }
            atualizando = false
            val p = partida ?: break
            if (!possivelmenteAoVivo(p.jogo, Instant.now())) break
            delay(30_000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).vidro(22.dp).clickable(onClick = voltar), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Text("Partida", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        val p = partida
        if (p == null) { Carregando(erro) { recarga++ }; return }
        val vivo = p.jogo.estado == "in"

        PullToRefreshBox(atualizando, { recarga++ }) {
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp)) {
                if (erro != null) item { Aviso(erro!!) { recarga++ } }
                item { Destaque(p.jogo) }
                item {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                        TituloSecao("Lances")
                        Spacer(Modifier.weight(1f))
                        if (vivo) Text("atualiza a cada 30 s", color = TextoSuave, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                if (p.lances.isEmpty()) item {
                    Text(
                        if (p.jogo.estado == "pre") "A bola ainda não rolou." else "Nenhum lance registrado ainda.",
                        color = TextoSuave, modifier = Modifier.fillMaxWidth().vidro(20.dp).padding(18.dp),
                    )
                }
                // Mais recente primeiro durante o jogo; em ordem depois.
                val lances = if (vivo) p.lances.reversed() else p.lances
                items(lances.size) { i -> LinhaLance(lances[i], p.jogo, ultimo = i == lances.lastIndex) }
            }
        }
    }
}

@Composable
private fun LinhaLance(l: Lance, j: Jogo, ultimo: Boolean) {
    val time = listOf(j.casa, j.fora).firstOrNull { it.nome == l.time || l.time?.startsWith(it.nome) == true }
    val gol = l.tipo == TipoLance.GOL || l.tipo == TipoLance.GOL_CONTRA
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(
            l.minuto, Modifier.width(46.dp).padding(top = 14.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = if (gol) Color.White else TextoSuave,
        )
        // Linha do tempo: bolinha com o ícone e um fio até o próximo lance.
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 6.dp).size(34.dp).vidro(17.dp, tom = if (gol) AzulClaro else Color.White, intensidade = if (gol) 2f else 1f),
                contentAlignment = Alignment.Center,
            ) { IconeLance(l.tipo) }
            if (!ultimo) Box(Modifier.width(2.dp).weight(1f).background(Color.White.copy(alpha = 0.12f)))
        }
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier.weight(1f).padding(bottom = 10.dp).vidro(18.dp, intensidade = if (gol) 1.5f else 1f).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(l.titulo, fontWeight = FontWeight.Bold, fontSize = if (gol) 16.sp else 14.sp)
                l.detalhe?.let { Text(it, color = TextoSuave, fontSize = 13.sp) }
            }
            time?.let { AsyncImage(it.logo, it.nome, Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun IconeLance(tipo: TipoLance) {
    @Composable fun icone(v: ImageVector, cor: Color = Color.White) = Icon(v, null, tint = cor, modifier = Modifier.size(18.dp))
    @Composable fun cartao(cor: Color) = Box(Modifier.rotate(10f).size(11.dp, 15.dp).clip(RoundedCornerShape(2.dp)).background(cor))
    when (tipo) {
        TipoLance.GOL -> icone(Icons.Rounded.SportsSoccer)
        TipoLance.GOL_CONTRA -> icone(Icons.Rounded.SportsSoccer, Vermelho)
        TipoLance.AMARELO -> cartao(Amarelo)
        TipoLance.VERMELHO -> cartao(Vermelho)
        TipoLance.SUBSTITUICAO -> icone(Icons.Rounded.SwapVert, Verde)
        TipoLance.PENALTI_PERDIDO -> icone(Icons.Rounded.Close, Vermelho)
    }
}

// ---------- Notícias ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelaNoticias() {
    var noticias by remember { mutableStateOf<List<Noticia>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var atualizando by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val abrir = { n: Noticia -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.link))) }

    LaunchedEffect(recarga) {
        atualizando = noticias != null
        runCatching { withContext(Dispatchers.IO) { carregarNoticias() } }
            .onSuccess { noticias = it; erro = null }.onFailure { erro = "Não deu para buscar as notícias" }
        atualizando = false
    }

    val lista = noticias
    if (lista == null) { Carregando(erro) { recarga++ }; return }

    PullToRefreshBox(atualizando, { recarga++ }) {
        LazyColumn(contentPadding = espacoBarra, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (erro != null) item { Aviso(erro!!) { recarga++ } }
            if (lista.isEmpty()) item { Text("Nenhuma notícia do Leão agora.", color = TextoSuave, modifier = Modifier.padding(8.dp)) }
            lista.firstOrNull()?.let { n -> item(key = n.link) { NoticiaPrincipal(n) { abrir(n) } } }
            items(lista.drop(1), key = { it.link }) { n -> NoticiaLinha(n) { abrir(n) } }
        }
    }
}

@Composable
private fun NoticiaPrincipal(n: Noticia, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().vidro(28.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(210.dp)) {
            Foto(n.imagem, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Noite.copy(alpha = 0.85f))))
            Box(Modifier.padding(14.dp)) { Etiqueta(n.fonte.uppercase(), fundo = Noite.copy(alpha = 0.55f)) }
            Text(
                n.titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            n.resumo?.let { Text(it, color = TextoSuave, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            n.data?.let { Text(tempoAtras(it), color = TextoSuave, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun NoticiaLinha(n: Noticia, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().vidro(22.dp).clickable(onClick = onClick).padding(10.dp)) {
        Foto(n.imagem, Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp).heightIn(min = 96.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(n.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(n.fonte, n.data?.let { tempoAtras(it) }).joinToString("  ·  "),
                color = TextoSuave, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun Foto(url: String?, modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Azul, Vermelho.copy(alpha = 0.7f)))), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Newspaper, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

// ---------- Comuns ----------

@Composable
private fun Carregando(erro: String?, tentar: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
        if (erro == null) CircularProgressIndicator(color = Color.White) else Aviso(erro, tentar)
    }
}

@Composable
private fun Aviso(texto: String, tentar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).vidro(20.dp, tom = Vermelho, intensidade = 1.3f).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(texto, fontWeight = FontWeight.SemiBold)
        TextButton(tentar) { Text("Tentar de novo", color = Color.White) }
    }
}

private val ptBR = Locale("pt", "BR")
private val fuso get() = ZoneId.systemDefault()
private fun hora(i: Instant) = DateTimeFormatter.ofPattern("HH:mm").format(i.atZone(fuso))
private fun dia(i: Instant) = DateTimeFormatter.ofPattern("EEEE, dd/MM", ptBR).format(i.atZone(fuso))

fun contagem(i: Instant, agora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): String {
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
