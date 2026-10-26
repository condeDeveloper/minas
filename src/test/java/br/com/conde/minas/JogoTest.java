package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JogoTest {

    @Test
    @DisplayName("abrir uma casa de numero zero abre a area inteira em volta")
    void abreEmCascata() {
        Campo campo = Campo.de(5, 5, List.of(24));
        Jogo jogo = new Jogo(campo);
        jogo.abre(0);
        assertThat(jogo.abertas()).as("so a mina e os vizinhos dela ficam").isEqualTo(24);
        assertThat(jogo.ganhou()).isTrue();
    }

    @Test
    @DisplayName("abrir uma casa com numero abre so ela")
    void abreSoUma() {
        Campo campo = Campo.de(3, 3, List.of(0));
        Jogo jogo = new Jogo(campo);
        jogo.abre(4);
        assertThat(jogo.abertas()).isEqualTo(1);
        assertThat(jogo.visao().numero(4)).isEqualTo(1);
        assertThat(jogo.ganhou()).isFalse();
    }

    @Test
    @DisplayName("pisar na mina acaba a partida e nada mais abre")
    void explode() {
        Campo campo = Campo.de(3, 3, List.of(0));
        Jogo jogo = new Jogo(campo);
        assertThat(jogo.abre(0)).isFalse();
        assertThat(jogo.explodiu()).isTrue();
        assertThat(jogo.abre(8)).isFalse();
        assertThat(jogo.abertas()).isZero();
        assertThat(jogo.ganhou()).isFalse();
    }

    @Test
    @DisplayName("a bandeira so entra em casa fechada e some quando a casa abre")
    void bandeira() {
        Campo campo = Campo.de(3, 3, List.of(0));
        Jogo jogo = new Jogo(campo);
        jogo.marca(4);
        assertThat(jogo.visao().bandeira(4)).isTrue();
        jogo.abre(4);
        assertThat(jogo.visao().bandeira(4)).isFalse();
        jogo.marca(4);
        assertThat(jogo.visao().bandeira(4)).as("casa aberta nao recebe bandeira").isFalse();
    }

    @Test
    @DisplayName("a partida so e ganha quando toda casa sem mina esta aberta")
    void ganhou() {
        Campo campo = Campo.de(2, 2, List.of(3));
        Jogo jogo = new Jogo(campo);
        jogo.abre(0);
        assertThat(jogo.ganhou()).isFalse();
        jogo.abre(1);
        jogo.abre(2);
        assertThat(jogo.abertas()).isEqualTo(3);
        assertThat(jogo.ganhou()).isTrue();
    }

    @Test
    @DisplayName("a visao da partida anuncia o total de minas do campo")
    void aVisaoSabeOTotal() {
        Campo campo = Campo.sorteado(9, 9, 10, 40, new Random(3));
        Jogo jogo = new Jogo(campo);
        jogo.abre(40);
        assertThat(jogo.visao().totalDeMinas()).isEqualTo(10);
        assertThat(jogo.visao().minasRestantes()).isEqualTo(10);
    }
}
