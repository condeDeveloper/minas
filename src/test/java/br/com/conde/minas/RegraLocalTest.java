package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegraLocalTest {

    @Test
    @DisplayName("numero igual ao que sobrou fechado: e tudo mina")
    void tudoMina() {
        Deducao deducao = RegraLocal.aplica(Visao.deTexto(3, "3.", ".."));
        assertThat(deducao.minas()).as("o 3 encosta em tres casas fechadas").containsExactly(1, 2, 3);
        assertThat(deducao.livres()).isEmpty();
    }

    @Test
    @DisplayName("numero ja todo marcado: o resto da vizinhanca e livre")
    void tudoLivre() {
        Deducao deducao = RegraLocal.aplica(Visao.deTexto(2, "F..", "1.."));
        assertThat(deducao.livres()).as("so a vizinhanca do 1").containsExactly(1, 4);
        assertThat(deducao.minas()).isEmpty();
    }

    @Test
    @DisplayName("a regra para num ponto fixo: marcar o que ela provou nao prova mais nada")
    void paraNumPontoFixo() {
        int provadas = 0;
        for (int partida = 0; partida < 20; partida++) {
            Campo campo = Campo.sorteado(9, 9, 10, 40, new java.util.Random(900L + partida));
            Jogo jogo = new Jogo(campo);
            jogo.abre(40);
            while (!jogo.ganhou() && !jogo.explodiu()) {
                Visao visao = jogo.visao();
                Deducao uma = RegraLocal.comTotal(visao);
                Visao marcada = visao.com(new int[0], new int[0], uma.minas());
                Deducao outra = RegraLocal.comTotal(marcada);
                assertThat(outra.livres()).as("partida " + partida).containsExactly(uma.livres());
                provadas += uma.quantas();
                if (uma.livres().length == 0) {
                    break;
                }
                for (int casa : uma.minas()) {
                    jogo.marca(casa);
                }
                for (int casa : uma.livres()) {
                    jogo.abre(casa);
                }
            }
        }
        assertThat(provadas).isPositive();
    }

    @Test
    @DisplayName("no 1-2-1 a regra local nao prova nada")
    void oUmDoisUm() {
        Visao visao = Visao.deTexto(2, "...", "121");
        assertThat(RegraLocal.aplica(visao).vazia()).isTrue();
        assertThat(RegraLocal.comTotal(visao).vazia()).isTrue();
    }

    @Test
    @DisplayName("o total de minas fecha a partida quando ja nao falta nenhuma")
    void oTotalZerado() {
        Visao visao = Visao.deTexto(1, "F1.", "...", "...");
        assertThat(RegraLocal.aplica(visao).livres())
                .as("sem o total, so a vizinhanca do 1").containsExactly(2, 3, 4, 5);
        assertThat(RegraLocal.comTotal(visao).livres())
                .as("a unica mina ja esta marcada, entao o resto todo e livre")
                .containsExactly(2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    @DisplayName("o total de minas tambem prova mina quando so sobra o que falta")
    void oTotalCheio() {
        Visao visao = Visao.deTexto(2, "..");
        assertThat(RegraLocal.aplica(visao).vazia()).as("sem numero na tela, sem conta").isTrue();
        assertThat(RegraLocal.comTotal(visao).minas())
                .as("duas minas e duas casas fechadas").containsExactly(0, 1);
    }

    @Test
    @DisplayName("a regra local nunca prova o que a enumeracao nao prova")
    void nuncaPassaDaEnumeracao() {
        for (int partida = 0; partida < 40; partida++) {
            Campo campo = Campo.sorteado(9, 9, 10, 40, new java.util.Random(500L + partida));
            Jogo jogo = new Jogo(campo);
            jogo.abre(40);
            while (!jogo.ganhou() && !jogo.explodiu()) {
                Visao visao = jogo.visao();
                Deducao local = RegraLocal.comTotal(visao);
                Enumeracao enumeracao = new Enumeracao(visao);
                Deducao completa = enumeracao.deducao();
                for (int casa : local.minas()) {
                    assertThat(completa.provaMina(casa)).as("partida " + partida).isTrue();
                }
                for (int casa : local.livres()) {
                    assertThat(completa.provaLivre(casa)).as("partida " + partida).isTrue();
                }
                for (int casa : completa.minas()) {
                    jogo.marca(casa);
                }
                if (completa.livres().length == 0) {
                    break;
                }
                for (int casa : completa.livres()) {
                    jogo.abre(casa);
                }
            }
        }
    }
}
