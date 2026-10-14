package com.dmwnezes.leao

import org.w3c.dom.Element
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

data class Noticia(
    val titulo: String,
    val resumo: String?,
    val fonte: String,
    val link: String,
    val imagem: String?,
    val data: Instant?,
)

// Feeds com foto. O do ge cobre o Ceará inteiro; o do O Povo, todo o esporte. Filtramos o Fortaleza.
private val FONTES = listOf(
    "ge" to "https://ge.globo.com/rss/ge/ce/",
    "O Povo" to "https://www.opovo.com.br/esportes/rss.xml",
)

fun carregarNoticias(): List<Noticia> {
    val porFonte = FONTES.map { (nome, url) -> runCatching { lerRss(baixar(url), nome) } }
    if (porFonte.all { it.isFailure }) throw porFonte.first().exceptionOrNull()!!
    return juntar(porFonte.mapNotNull { it.getOrNull() })
}

fun juntar(listas: List<List<Noticia>>): List<Noticia> =
    listas.flatten().filter(::ehDoFortaleza).distinctBy { it.link }.sortedByDescending { it.data }.take(40)

fun ehDoFortaleza(n: Noticia) =
    "/times/fortaleza/" in n.link || (Regex("\\bFortaleza\\b").containsMatchIn(n.titulo) && "/times/" !in n.link)

fun lerRss(xml: String, fonte: String): List<Noticia> {
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
    val itens = doc.getElementsByTagName("item")
    return (0 until itens.length).map { i ->
        val it = itens.item(i) as Element
        val titulo = it.texto("title").orEmpty()
        val descricao = it.texto("description").orEmpty()
        val imagem = it.filho("enclosure")?.getAttribute("url")?.ifBlank { null }
            ?: it.filho("content")?.getAttribute("url")?.ifBlank { null }
            ?: Regex("<img[^>]+src=\"([^\"]+)\"").find(descricao + it.texto("encoded").orEmpty())?.groupValues?.get(1)
        val resumo = (it.texto("subtitle") ?: limparHtml(descricao)).take(200).ifBlank { null }?.takeIf { r -> r != titulo }
        Noticia(
            titulo = titulo,
            resumo = resumo,
            fonte = fonte,
            link = it.texto("link").orEmpty(),
            imagem = imagem,
            data = it.texto("pubDate")?.let { d -> runCatching { ZonedDateTime.parse(d, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull() },
        )
    }
}

private fun limparHtml(s: String) = s.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

// Busca o filho pelo nome sem prefixo ("media:content" -> "content"), sem depender de namespace.
private fun Element.filho(nome: String): Element? {
    val nos = childNodes
    for (i in 0 until nos.length) {
        val n = nos.item(i)
        if (n is Element && n.tagName.substringAfter(':') == nome) return n
    }
    return null
}

private fun Element.texto(nome: String) = filho(nome)?.textContent?.trim()?.ifBlank { null }
