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

    @Test fun noticiasComFotoSoDoFortaleza() {
        val ge = lerRss(arquivo("ge.xml"), "ge")
        val opovo = lerRss(arquivo("opovo.xml"), "O Povo")
        val n = juntar(listOf(ge, opovo))
        assertTrue(n.size >= 20)
        assertTrue(n.all { ehDoFortaleza(it) })
        assertTrue(n.none { "/times/ceara/" in it.link })
        // Resumos de partida do ge vêm sem foto; o app mostra um quadro tricolor no lugar.
        assertTrue(n.count { it.imagem?.startsWith("https://") == true } >= n.size - 3)
        assertTrue(n.zipWithNext().all { (a, b) -> (a.data ?: Instant.MIN) >= (b.data ?: Instant.MIN) })
        assertTrue(n.any { it.fonte == "ge" } && n.any { it.fonte == "O Povo" })
        val primeira = ge.first()
        assertEquals("Torcida do Fortaleza ainda sonha com o acesso direto à Série A. Protesto foi pacífico", primeira.resumo)
    }

    @Test fun contagemPorDiaDoCalendario() {
        val z = ZoneId.of("America/Fortaleza")
        val agora = Instant.parse("2026-10-14T05:00:00Z") // 02:00 local
        assertEquals("em 2 dias", contagem(Instant.parse("2026-10-16T23:30:00Z"), agora, z))
        assertEquals("amanhã", contagem(Instant.parse("2026-10-15T23:30:00Z"), agora, z))
        assertEquals("hoje, em 3 h", contagem(Instant.parse("2026-10-14T08:00:00Z"), agora, z))
    }
}
