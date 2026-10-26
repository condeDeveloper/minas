package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisaoTest {

    @Test
    @DisplayName("a visao escrita a mao vira casas abertas, fechadas e bandeiras")
    void deTexto() {
        Visao visao = Visao.deTexto(3, "1F.", "...");
        assertThat(visao.largura()).isEqualTo(3);
        assertThat(visao.altura()).isEqualTo(2);
        assertThat(visao.aberta(0)).isTrue();
        assertThat(visao.numero(0)).isEqualTo(1);
        assertThat(visao.bandeira(1)).isTrue();
        assertThat(visao.emAberto(1)).as("casa com bandeira nao esta mais em aberto").isFalse();
        assertThat(visao.emAberto(2)).isTrue();
        assertThat(visao.minasRestantes()).isEqualTo(2);
        assertThat(visao.emAberto()).isEqualTo(4);
    }

    @Test
    @DisplayName("linha de tamanho diferente ou letra que nao existe sao recusadas")
    void desenhoTorto() {
        assertThatThrownBy(() -> Visao.deTexto(1, "...", ".."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamanho diferente");
        assertThatThrownBy(() -> Visao.deTexto(1, "..x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letra que nao existe");
    }

    @Test
    @DisplayName("cada casa aberta vira uma conta, ja descontando as bandeiras")
    void restricoes() {
        Visao visao = Visao.deTexto(2, "...", "2F.");
        List<Visao.Restricao> contas = visao.restricoes();
        assertThat(contas).hasSize(1);
        assertThat(contas.get(0).soma()).as("o 2 com uma bandeira do lado vira 1").isEqualTo(1);
        assertThat(contas.get(0).casas()).containsExactly(0, 1);
    }

    @Test
    @DisplayName("a conta de uma casa aberta sem vizinho fechado nao entra")
    void semVizinhoFechado() {
        assertThat(Visao.deTexto(0, "000", "000").restricoes()).isEmpty();
    }

    @Test
    @DisplayName("fronteira e o que encosta em numero; o resto e escuro")
    void fronteiraEEscuro() {
        Visao visao = Visao.deTexto(1, ".1..", "....", "....");
        assertThat(visao.fronteira()).containsExactly(0, 2, 4, 5, 6);
        assertThat(visao.longe()).containsExactly(3, 7, 8, 9, 10, 11);
        assertThat(visao.fronteira().length + visao.longe().length).isEqualTo(visao.emAberto());
    }

    @Test
    @DisplayName("mexer numa copia nao mexe na visao de onde ela saiu")
    void copiaNaoVaza() {
        Visao visao = Visao.deTexto(2, "...", "...");
        Visao outra = visao.com(new int[] {0}, new int[] {1}, new int[] {5});
        assertThat(outra.aberta(0)).isTrue();
        assertThat(outra.bandeira(5)).isTrue();
        assertThat(visao.aberta(0)).isFalse();
        assertThat(visao.bandeira(5)).isFalse();
    }

    @Test
    @DisplayName("a restricao devolve copia do arranjo, nao o de dentro")
    void restricaoNaoVaza() {
        Visao.Restricao conta = new Visao.Restricao(1, new int[] {3, 4});
        conta.casas()[0] = 99;
        assertThat(conta.casas()).containsExactly(3, 4);
    }

    @Test
    @DisplayName("a visao sai desenhada igual ao desenho que a criou")
    void desenho() {
        String[] linhas = {"1F.", "..2"};
        assertThat(Visao.deTexto(3, linhas).toString()).isEqualTo("1F.\n..2\n");
    }
}
