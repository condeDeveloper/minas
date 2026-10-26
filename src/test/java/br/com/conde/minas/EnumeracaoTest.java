package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnumeracaoTest {

    @Test
    @DisplayName("o 1-2-1 tem uma distribuicao so, e ela decide as tres casas")
    void oUmDoisUm() {
        Visao visao = Visao.deTexto(2, "...", "121");
        Enumeracao enumeracao = new Enumeracao(visao);
        assertThat(enumeracao.modelos()).isEqualTo(BigInteger.ONE);
        assertThat(enumeracao.deducao().minas()).containsExactly(0, 2);
        assertThat(enumeracao.deducao().livres()).containsExactly(1);
        assertThat(enumeracao.probabilidade(0)).isEqualTo(1.0);
        assertThat(enumeracao.probabilidade(1)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("sem o total de minas o 1-2-1 continua resolvido: ele nao precisa do total")
    void oUmDoisUmSemOTotal() {
        Visao visao = Visao.deTexto(2, "...", "121");
        assertThat(new Enumeracao(visao, false).deducao().quantas()).isEqualTo(3);
    }

    @Test
    @DisplayName("o total de minas sozinho prova seis casas que nada mais prova")
    void soOTotalProva() {
        Visao visao = Visao.deTexto(1, ".1..", "....", "....");
        assertThat(RegraLocal.comTotal(visao).vazia()).isTrue();
        assertThat(new Enumeracao(visao, false).deducao().vazia()).isTrue();
        Deducao comTotal = new Enumeracao(visao).deducao();
        assertThat(comTotal.livres()).containsExactly(3, 7, 8, 9, 10, 11);
        assertThat(comTotal.minas()).isEmpty();
    }

    @Test
    @DisplayName("com uma mina entre cinco casas, cada uma tem 20% certinhos")
    void probabilidadeExata() {
        Visao visao = Visao.deTexto(1, ".1..", "....", "....");
        Enumeracao enumeracao = new Enumeracao(visao);
        assertThat(enumeracao.modelos()).isEqualTo(BigInteger.valueOf(5));
        for (int casa : visao.fronteira()) {
            assertThat(enumeracao.probabilidade(casa)).isCloseTo(0.20, within(1e-12));
        }
        for (int casa : visao.longe()) {
            assertThat(enumeracao.probabilidade(casa)).isCloseTo(0.0, within(1e-12));
        }
    }

    @Test
    @DisplayName("sem o total, as casas do escuro nao tem probabilidade nenhuma")
    void oEscuroSemOTotal() {
        Visao visao = Visao.deTexto(1, ".1..", "....", "....");
        Enumeracao enumeracao = new Enumeracao(visao, false);
        assertThat(enumeracao.probabilidade(visao.longe()[0])).isNaN();
        assertThat(enumeracao.probabilidade(visao.fronteira()[0])).isCloseTo(0.20, within(1e-12));
    }

    @Test
    @DisplayName("pedacos separados da fronteira sao enumerados separados")
    void componentes() {
        Visao visao = Visao.deTexto(2, ".1..1.", "......");
        Enumeracao enumeracao = new Enumeracao(visao);
        assertThat(enumeracao.componentes()).isEqualTo(2);
        assertThat(enumeracao.maiorComponente()).isEqualTo(5);
    }

    @Test
    @DisplayName("visao que nao fecha com o total anunciado e recusada")
    void visaoImpossivel() {
        assertThatThrownBy(() -> new Enumeracao(Visao.deTexto(9, "..", "1.")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao fecha");
    }

    @Test
    @DisplayName("o melhor chute e a casa de menor chance")
    void melhorChute() {
        Visao visao = Visao.deTexto(2, "...", "12.");
        Enumeracao enumeracao = new Enumeracao(visao);
        int melhor = enumeracao.melhorChute();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (visao.emAberto(casa)) {
                assertThat(enumeracao.probabilidade(casa))
                        .isGreaterThanOrEqualTo(enumeracao.probabilidade(melhor) - 1e-12);
            }
        }
    }

    @Test
    @DisplayName("o que a enumeracao prova esta sempre certo, partida atras de partida")
    void nuncaProvaErrado() {
        int provadas = 0;
        for (int partida = 0; partida < 60; partida++) {
            Campo campo = Campo.sorteado(9, 9, 10, 40, new java.util.Random(300L + partida));
            Jogo jogo = new Jogo(campo);
            jogo.abre(40);
            while (!jogo.ganhou() && !jogo.explodiu()) {
                Visao visao = jogo.visao();
                Enumeracao enumeracao = new Enumeracao(visao);
                Deducao deducao = enumeracao.deducao();
                for (int casa : deducao.minas()) {
                    assertThat(campo.temMina(casa)).as("provou mina na casa " + casa).isTrue();
                    jogo.marca(casa);
                }
                for (int casa : deducao.livres()) {
                    assertThat(campo.temMina(casa)).as("provou livre na casa " + casa).isFalse();
                }
                provadas += deducao.quantas();
                if (deducao.livres().length == 0) {
                    break;
                }
                for (int casa : deducao.livres()) {
                    jogo.abre(casa);
                }
            }
        }
        assertThat(provadas).isGreaterThan(1000);
    }

    @Test
    @DisplayName("a probabilidade de quem nao esta em aberto e NaN")
    void semProbabilidade() {
        Visao visao = Visao.deTexto(2, "F..", "1..");
        Enumeracao enumeracao = new Enumeracao(visao);
        assertThat(enumeracao.probabilidade(0)).as("bandeira").isNaN();
        assertThat(enumeracao.probabilidade(3)).as("casa aberta").isNaN();
    }
}
