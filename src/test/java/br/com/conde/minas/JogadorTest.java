package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JogadorTest {

    private static Campo campo(int partida) {
        return Campo.sorteado(9, 9, 10, 40, new Random(1000L + partida));
    }

    @Test
    @DisplayName("partida sem chute nenhum e sempre partida ganha")
    void semChuteEVitoria() {
        for (Jogador.Jeito jeito : Jogador.Jeito.values()) {
            for (int partida = 0; partida < 80; partida++) {
                Jogador.Resultado fim = Jogador.joga(campo(partida), 40, jeito,
                        new Random(7000L + partida));
                if (fim.semChute()) {
                    assertThat(fim.ganhou()).as(jeito + ", partida " + partida).isTrue();
                }
            }
        }
    }

    @Test
    @DisplayName("quem ganha abriu toda casa sem mina")
    void ganharEAbrirTudo() {
        for (int partida = 0; partida < 80; partida++) {
            Jogador.Resultado fim = Jogador.joga(campo(partida), 40,
                    Jogador.Jeito.ENUMERACAO_E_CHUTE_BOM, new Random(7000L + partida));
            if (fim.ganhou()) {
                assertThat(fim.abertas()).isEqualTo(fim.casasSeguras());
            } else {
                assertThat(fim.chutesQueExplodiram()).isPositive();
            }
        }
    }

    @Test
    @DisplayName("a enumeracao ganha mais que a regra local no mesmo conjunto de partidas")
    void aEnumeracaoGanhaMais() {
        int local = 0;
        int completa = 0;
        for (int partida = 0; partida < 150; partida++) {
            if (Jogador.joga(campo(partida), 40, Jogador.Jeito.REGRA_LOCAL,
                    new Random(7000L + partida)).ganhou()) {
                local++;
            }
            if (Jogador.joga(campo(partida), 40, Jogador.Jeito.ENUMERACAO_E_CHUTE_BOM,
                    new Random(7000L + partida)).ganhou()) {
                completa++;
            }
        }
        assertThat(completa).isGreaterThan(local);
    }

    @Test
    @DisplayName("os dois jeitos de enumerar provam a mesma coisa: so o chute muda")
    void oChuteNaoMudaADeducao() {
        for (int partida = 0; partida < 60; partida++) {
            Jogador.Resultado comChuteCego = Jogador.joga(campo(partida), 40,
                    Jogador.Jeito.ENUMERACAO, new Random(7000L + partida));
            Jogador.Resultado comChuteBom = Jogador.joga(campo(partida), 40,
                    Jogador.Jeito.ENUMERACAO_E_CHUTE_BOM, new Random(7000L + partida));
            assertThat(comChuteCego.semChute()).isEqualTo(comChuteBom.semChute());
        }
    }

    @Test
    @DisplayName("quem so olha um numero por vez chuta muito mais")
    void aRegraLocalChutaMais() {
        int chutesLocais = 0;
        int chutesCompletos = 0;
        for (int partida = 0; partida < 80; partida++) {
            chutesLocais += Jogador.joga(campo(partida), 40, Jogador.Jeito.REGRA_LOCAL,
                    new Random(7000L + partida)).chutes();
            chutesCompletos += Jogador.joga(campo(partida), 40, Jogador.Jeito.ENUMERACAO,
                    new Random(7000L + partida)).chutes();
        }
        assertThat(chutesLocais).isGreaterThan(3 * chutesCompletos);
    }

    @Test
    @DisplayName("o jeito escolhido decide o que da para provar numa mesma visao")
    void deduzPorJeito() {
        Visao visao = Visao.deTexto(2, "...", "121");
        assertThat(Jogador.deduz(visao, Jogador.Jeito.REGRA_LOCAL).vazia()).isTrue();
        assertThat(Jogador.deduz(visao, Jogador.Jeito.REGRA_COM_TOTAL).vazia()).isTrue();
        assertThat(Jogador.deduz(visao, Jogador.Jeito.ENUMERACAO).quantas()).isEqualTo(3);
        assertThat(Jogador.deduz(visao, Jogador.Jeito.ENUMERACAO_E_CHUTE_BOM).quantas())
                .isEqualTo(3);
    }
}
