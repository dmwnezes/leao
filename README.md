# Leão

App Android para acompanhar o **Fortaleza Esporte Clube**: placar ao vivo com lances e notícias.

- **Jogos:** destaque para o jogo ao vivo (ou o próximo, com contagem regressiva) e o último resultado. Abaixo, próximos jogos e resultados com V/E/D. Série B, Copa do Brasil e Copa do Nordeste.
- **Ao vivo:** placar e minuto atualizados a cada 30 s enquanto a bola rola.
- **Lances:** toque em qualquer jogo para ver gols (com assistência), gols contra, cartões e substituições (quem entra e quem sai).
- **Notícias:** busca automática no ge e no O Povo, só do Fortaleza, com foto. A mais recente aparece em destaque. Toque para abrir a matéria.
- **Visual:** glassmorphism, com painéis de vidro sobre um fundo com luzes nas cores do clube. Puxe a tela para baixo para atualizar.

## Instalar no celular

**https://github.com/dmwnezes/leao/releases/latest/download/Leao.apk**

O Android vai pedir para permitir a instalação de apps desta fonte. Aceite e instale. Novas versões instalam por cima da anterior.

## De onde vêm os dados

- Jogos e lances: API pública da ESPN (sem chave). Não é oficial, então pode mudar sem aviso.
- Notícias: feeds RSS do ge (Ceará) e do O Povo (Esportes), filtrados pelo Fortaleza.

## Como é gerado

A cada push na `main`, o GitHub Actions roda os testes, gera o APK assinado e publica em **Releases**.
