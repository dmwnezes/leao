package com.dmwnezes.leao

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// shortcut: API pública não oficial da ESPN, sem chave; se mudar o formato, ajustar aqui.
const val FORTALEZA_ID = "6272"
private const val BASE = "https://site.api.espn.com/apis/site/v2/sports/soccer/all"

data class Time(val id: String, val nome: String, val logo: String?)

data class Jogo(
    val id: String,
    val competicao: String,
    val inicio: Instant,
    val estado: String, // pre, in, post
    val relogio: String,
    val casa: Time,
    val fora: Time,
    val golsCasa: String?,
    val golsFora: String?,
    val estadio: String?,
)

data class Lance(val minuto: String, val icone: String, val titulo: String, val detalhe: String?, val time: String?)

data class Partida(val jogo: Jogo, val lances: List<Lance>)

private val http = OkHttpClient()

fun baixar(url: String): String =
    http.newCall(Request.Builder().url(url).header("User-Agent", "Leao/1.0").build()).execute().use {
        check(it.isSuccessful) { "HTTP ${it.code}" }
        it.body!!.string()
    }

/** Resultados + próximos jogos, em ordem de data. */
fun carregarJogos(): List<Jogo> {
    val passados = lerAgenda(baixar("$BASE/teams/$FORTALEZA_ID/schedule"))
    val futuros = lerAgenda(baixar("$BASE/teams/$FORTALEZA_ID/schedule?fixture=true"))
    return (passados + futuros).associateBy { it.id }.values.sortedBy { it.inicio }
}

fun carregarPartida(id: String): Partida = lerResumo(baixar("$BASE/summary?event=$id"))

fun lerAgenda(json: String): List<Jogo> {
    val evs = JSONObject(json).optJSONArray("events") ?: return emptyList()
    return (0 until evs.length()).map { i ->
        val e = evs.getJSONObject(i)
        val liga = e.optJSONObject("league")
        lerCompeticao(
            e.getJSONArray("competitions").getJSONObject(0),
            nomeCompeticao(liga?.optString("slug").orEmpty(), liga?.optString("name").orEmpty()),
        )
    }
}

fun lerResumo(json: String): Partida {
    val raiz = JSONObject(json)
    val h = raiz.getJSONObject("header")
    val liga = h.optJSONObject("league")
    val jogo = lerCompeticao(
        h.getJSONArray("competitions").getJSONObject(0),
        nomeCompeticao(liga?.optString("slug").orEmpty(), liga?.optString("name").orEmpty()),
    )
    val ke = raiz.optJSONArray("keyEvents")
    val lances = (0 until (ke?.length() ?: 0)).mapNotNull { lerLance(ke!!.getJSONObject(it)) }
    // O estádio vem em gameInfo no resumo, não no cabeçalho.
    val estadio = raiz.optJSONObject("gameInfo")?.optJSONObject("venue")?.optString("fullName")?.ifBlank { null }
    return Partida(jogo.copy(estadio = jogo.estadio ?: estadio), lances)
}

private fun lerCompeticao(c: JSONObject, competicao: String): Jogo {
    val cs = c.getJSONArray("competitors")
    val lados = (0 until cs.length()).map { cs.getJSONObject(it) }
    val casa = lados.first { it.optString("homeAway") == "home" }
    val fora = lados.first { it.optString("homeAway") == "away" }
    val status = c.getJSONObject("status")
    val estado = status.getJSONObject("type").optString("state", "pre")
    return Jogo(
        id = c.getString("id"),
        competicao = competicao,
        inicio = lerData(c.getString("date")),
        estado = estado,
        relogio = relogio(status),
        casa = lerTime(casa),
        fora = lerTime(fora),
        golsCasa = if (estado == "pre") null else gols(casa),
        golsFora = if (estado == "pre") null else gols(fora),
        estadio = c.optJSONObject("venue")?.optString("fullName")?.ifBlank { null },
    )
}

private fun lerTime(x: JSONObject): Time {
    val t = x.getJSONObject("team")
    val logo = t.optJSONArray("logos")?.optJSONObject(0)?.optString("href") ?: t.optString("logo").ifBlank { null }
    return Time(t.optString("id"), t.optString("shortDisplayName").ifBlank { t.optString("displayName") }, logo)
}

// A agenda manda o placar como objeto; o resumo, como texto.
private fun gols(x: JSONObject): String? =
    x.optJSONObject("score")?.optString("displayValue") ?: x.optString("score").ifBlank { null }

// ESPN manda "2026-10-16T23:30Z" (sem segundos); Instant.parse não aceita.
fun lerData(s: String): Instant = OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]X")).toInstant()

fun nomeCompeticao(slug: String, nome: String) = when (slug) {
    "bra.1" -> "Série A"
    "bra.2" -> "Série B"
    "bra.copa_do_brazil" -> "Copa do Brasil"
    "bra.copa_do_nordeste" -> "Copa do Nordeste"
    "conmebol.sudamericana" -> "Sul-Americana"
    "conmebol.libertadores" -> "Libertadores"
    else -> nome
}

fun relogio(status: JSONObject): String {
    val t = status.getJSONObject("type")
    return when (t.optString("name")) {
        "STATUS_POSTPONED" -> "Adiado"
        "STATUS_CANCELED" -> "Cancelado"
        "STATUS_HALFTIME" -> "Intervalo"
        "STATUS_FULL_TIME" -> "Encerrado"
        "STATUS_FINAL_AET" -> "Fim da prorrogação"
        "STATUS_FINAL_PEN" -> "Fim nos pênaltis"
        else -> when (t.optString("state")) {
            "in" -> if (t.optString("shortDetail") == "HT") "Intervalo" else status.optString("displayClock")
            "post" -> "Encerrado"
            else -> ""
        }
    }
}

private fun lerLance(k: JSONObject): Lance? {
    val tipo = k.optJSONObject("type")?.optString("type").orEmpty()
    val nomes = k.optJSONArray("participants")?.let { p ->
        (0 until p.length()).map { p.getJSONObject(it).optJSONObject("athlete")?.optString("displayName").orEmpty() }
    }.orEmpty().filter { it.isNotBlank() }
    val minuto = k.optJSONObject("clock")?.optString("displayValue").orEmpty()
    val time = k.optJSONObject("team")?.optString("displayName")?.ifBlank { null }
    val gol = k.optBoolean("scoringPlay")
    return when {
        tipo == "own-goal" -> Lance(minuto, "⚽", "Gol contra", nomes.firstOrNull(), time)
        gol -> Lance(
            minuto, "⚽", if (tipo.contains("penalty")) "Gol de pênalti" else "Gol",
            listOfNotNull(nomes.getOrNull(0), nomes.getOrNull(1)?.let { "assist. $it" }).joinToString(" · ").ifBlank { null },
            time,
        )
        tipo == "yellow-card" -> Lance(minuto, "🟨", "Cartão amarelo", nomes.firstOrNull(), time)
        tipo == "red-card" -> Lance(minuto, "🟥", "Cartão vermelho", nomes.firstOrNull(), time)
        tipo == "substitution" -> Lance(
            minuto, "🔁", "Substituição",
            listOfNotNull(nomes.getOrNull(0)?.let { "entra $it" }, nomes.getOrNull(1)?.let { "sai $it" }).joinToString(" · ").ifBlank { null },
            time,
        )
        tipo.contains("penalty") && tipo.contains("miss") -> Lance(minuto, "❌", "Pênalti perdido", nomes.firstOrNull(), time)
        else -> null
    }
}
