package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ForcaBrutaTest {

    @Test
    @DisplayName("a forca bruta conta os mesmos campos que a enumeracao")
    void mesmaContagem() {
        Visao visao = Visao.deTexto(1, ".1..", "....", "....");
        ForcaBruta forca = new ForcaBruta(visao);
        assertThat(forca.modelos()).isEqualTo(5);
        assertThat(new Enumeracao(visao).modelos()).isEqualTo(BigInteger.valueOf(5));
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (visao.emAberto(casa)) {
                assertThat(forca.probabilidade(casa))
                        .isCloseTo(new Enumeracao(visao).probabilidade(casa), within(1e-12));
            }
        }
    }

    @Test
    @DisplayName("a forca bruta confere cada casa aberta, nao so as da fronteira")
    void confereTudo() {
        // O 0 do canto de baixo nao faz fronteira com nada, mas proibe mina do lado.
        Visao visao = Visao.deTexto(1, "...", "..1", "..0");
        ForcaBruta forca = new ForcaBruta(visao);
        assertThat(forca.deducao().livres()).contains(4, 7);
        assertThat(forca.modelos()).isEqualTo(new Enumeracao(visao).modelos().longValueExact());
    }

    @Test
    @DisplayName("acima de 24 casas fechadas a forca bruta se recusa")
    void limite() {
        assertThatThrownBy(() -> new ForcaBruta(Visao.deTexto(5, ".....", ".....", ".....",
                ".....", ".....", ".....")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24 casas fechadas");
    }

    @Test
    @DisplayName("os dois metodos concordam casa por casa em centenas de posicoes de verdade")
    void osDoisConcordam() {
        int comparadas = 0;
        long casas = 0;
        for (int partida = 0; partida < 60; partida++) {
            Campo campo = Campo.sorteado(6, 6, 6, 21, new Random(1000L + partida));
            Jogo jogo = new Jogo(campo);
            jogo.abre(21);
            while (!jogo.ganhou() && !jogo.explodiu()) {
                Visao visao = jogo.visao();
                Enumeracao enumeracao = new Enumeracao(visao);
                if (visao.emAberto() <= 20) {
                    comparadas++;
                    ForcaBruta forca = new ForcaBruta(visao);
                    assertThat(enumeracao.modelos())
                            .as("partida " + partida)
                            .isEqualTo(BigInteger.valueOf(forca.modelos()));
                    assertThat(enumeracao.deducao().minas()).isEqualTo(forca.deducao().minas());
                    assertThat(enumeracao.deducao().livres()).isEqualTo(forca.deducao().livres());
                    for (int casa = 0; casa < visao.casas(); casa++) {
                        if (visao.emAberto(casa)) {
                            casas++;
                            assertThat(enumeracao.probabilidade(casa))
                                    .as("partida " + partida + ", casa " + casa)
                                    .isCloseTo(forca.probabilidade(casa), within(1e-9));
                        }
                    }
                }
                Deducao deducao = enumeracao.deducao();
                for (int casa : deducao.minas()) {
                    jogo.marca(casa);
                }
                if (deducao.livres().length == 0) {
                    int chute = enumeracao.melhorChute();
                    if (chute < 0) {
                        break;
                    }
                    jogo.abre(chute);
                } else {
                    for (int casa : deducao.livres()) {
                        jogo.abre(casa);
                    }
                }
            }
        }
        assertThat(comparadas).isGreaterThan(50);
        assertThat(casas).isGreaterThan(500);
    }
}
