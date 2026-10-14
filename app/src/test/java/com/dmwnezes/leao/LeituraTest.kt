package com.dmwnezes.leao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LeituraTest {
    private fun arquivo(n: String) = javaClass.classLoader!!.getResource(n).readText()

    @Test fun agendaComResultado() {
        val jogos = lerAgenda(arquivo("schedule.json"))
        assertEquals(2, jogos.size)
        val crb = jogos.last()
        assertEquals("Série B", crb.competicao)
        assertEquals("post", crb.estado)
        assertEquals("Encerrado", crb.relogio)
        assertEquals("Fortaleza", crb.casa.nome)
        assertEquals("2", crb.golsCasa)
        assertEquals("2", crb.golsFora)
        assertEquals("E", resultado(crb))
    }

    @Test fun proximoJogoSemPlacar() {
        val j = lerAgenda(arquivo("fixture.json")).single()
        assertEquals("pre", j.estado)
        assertNull(j.golsCasa)
        assertEquals("Sport", j.casa.nome)
        assertEquals(Instant.parse("2026-10-16T23:30:00Z"), j.inicio)
    }

    @Test fun resumoComLances() {
        val p = lerResumo(arquivo("summary.json"))
        assertEquals("2", p.jogo.golsCasa)
        assertEquals("Série B", p.jogo.competicao)
        assertEquals("Arena Castelão", p.jogo.estadio)
        assertEquals(17, p.lances.size)
        val gol = p.lances[1]
        assertEquals("11'", gol.minuto)
        assertEquals("Gol", gol.titulo)
        assertEquals("Maílton dos Santos · assist. Juan Cantillo", gol.detalhe)
        assertEquals("Gol contra", p.lances[3].titulo)
        assertEquals("entra Rodriguinho · sai Rodrigo Santos", p.lances[4].detalhe)
        assertEquals("Cartão amarelo", p.lances[0].titulo)
    }

    @Test fun noticiasDoGoogle() {
        val n = lerRss(arquivo("news.rss"))
        assertTrue(n.isNotEmpty())
        assertTrue(n.none { it.fonte != null && it.titulo.endsWith(" - ${it.fonte}") })
        assertTrue(n.zipWithNext().all { (a, b) -> (a.data ?: Instant.MIN) >= (b.data ?: Instant.MIN) })
    }

    @Test fun contagemPorDiaDoCalendario() {
        val z = ZoneId.of("America/Fortaleza")
        val agora = Instant.parse("2026-10-14T05:00:00Z") // 02:00 local
        assertEquals("em 2 dias", contagem(Instant.parse("2026-10-16T23:30:00Z"), agora, z))
        assertEquals("amanhã", contagem(Instant.parse("2026-10-15T23:30:00Z"), agora, z))
        assertEquals("hoje, em 3 h", contagem(Instant.parse("2026-10-14T08:00:00Z"), agora, z))
    }
}
