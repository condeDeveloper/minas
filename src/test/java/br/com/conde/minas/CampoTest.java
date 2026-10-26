package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampoTest {

    @Test
    @DisplayName("o numero de uma casa e quantas minas encostam nela")
    void contagem() {
        Campo campo = Campo.de(3, 3, List.of(0, 4));
        assertThat(campo.contagem(1)).as("encosta nas duas").isEqualTo(2);
        assertThat(campo.contagem(8)).as("encosta so no meio").isEqualTo(1);
        assertThat(campo.contagem(0)).as("a propria mina conta a vizinhanca").isEqualTo(1);
        assertThat(campo.minas()).isEqualTo(2);
        assertThat(campo.seguras()).isEqualTo(7);
    }

    @Test
    @DisplayName("as casas do canto tem tres vizinhos e as do meio tem oito")
    void vizinhos() {
        Campo campo = Campo.de(4, 4, List.of());
        assertThat(campo.vizinhos(0)).hasSize(3);
        assertThat(campo.vizinhos(1)).hasSize(5);
        assertThat(campo.vizinhos(5)).hasSize(8);
        assertThat(campo.vizinhos(15)).hasSize(3);
    }

    @Test
    @DisplayName("o sorteio deixa a primeira casa e os oito vizinhos dela sem mina")
    void aPrimeiraCasaESegura() {
        for (int semente = 0; semente < 200; semente++) {
            Campo campo = Campo.sorteado(9, 9, 10, 40, new Random(semente));
            assertThat(campo.temMina(40)).isFalse();
            for (int vizinho : campo.vizinhos(40)) {
                assertThat(campo.temMina(vizinho)).as("semente " + semente).isFalse();
            }
            assertThat(campo.minas()).isEqualTo(10);
            assertThat(campo.contagem(40)).as("a primeira jogada sempre abre uma area").isZero();
        }
    }

    @Test
    @DisplayName("o sorteio reclama quando nao cabe tanta mina")
    void minasDemais() {
        assertThatThrownBy(() -> Campo.sorteado(5, 5, 20, 12, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minas demais");
    }

    @Test
    @DisplayName("o mesmo numero de semente sorteia o mesmo campo")
    void oSorteioSeRepete() {
        Campo um = Campo.sorteado(16, 16, 40, 136, new Random(42));
        Campo outro = Campo.sorteado(16, 16, 40, 136, new Random(42));
        for (int casa = 0; casa < um.casas(); casa++) {
            assertThat(um.temMina(casa)).isEqualTo(outro.temMina(casa));
        }
    }

    @Test
    @DisplayName("o campo sai desenhado com asterisco na mina")
    void desenho() {
        Campo campo = Campo.de(3, 2, List.of(0));
        assertThat(campo.toString()).isEqualTo("*10\n110\n");
    }
}
