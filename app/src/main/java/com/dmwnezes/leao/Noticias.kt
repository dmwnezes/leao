package com.dmwnezes.leao

import org.w3c.dom.Element
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

data class Noticia(val titulo: String, val fonte: String?, val link: String, val data: Instant?)

private const val RSS =
    "https://news.google.com/rss/search?q=%22Fortaleza+Esporte+Clube%22+OR+%22Fortaleza+EC%22&hl=pt-BR&gl=BR&ceid=BR:pt-419"

fun carregarNoticias(): List<Noticia> = lerRss(baixar(RSS))

fun lerRss(xml: String): List<Noticia> {
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
    val itens = doc.getElementsByTagName("item")
    return (0 until itens.length).map { i ->
        val it = itens.item(i) as Element
        fun tag(n: String) = it.getElementsByTagName(n).item(0)?.textContent?.trim().orEmpty()
        val fonte = tag("source").ifBlank { null }
        // O Google põe " - Fonte" no fim do título.
        val titulo = tag("title").let { t -> if (fonte != null) t.removeSuffix(" - $fonte") else t }
        val data = runCatching { ZonedDateTime.parse(tag("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
        Noticia(titulo, fonte, tag("link"), data)
    }.sortedByDescending { it.data }.take(40)
}
