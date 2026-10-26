package br.com.conde.minas;

import java.util.ArrayDeque;
import java.util.Deque;

/** Uma partida: o campo embaixo, a visao em cima e as jogadas no meio. */
public final class Jogo {

    private final Campo campo;
    private final int[] numero;
    private final boolean[] bandeira;
    private boolean explodiu;
    private int abertas;

    public Jogo(Campo campo) {
        this.campo = campo;
        this.numero = new int[campo.casas()];
        this.bandeira = new boolean[campo.casas()];
        java.util.Arrays.fill(numero, -1);
    }

    /**
     * Abre a casa. Se o numero dela for zero, abre a vizinhanca inteira junto, que
     * e o que faz a primeira jogada descobrir metade do tabuleiro.
     *
     * @return false se tinha mina embaixo
     */
    public boolean abre(int casa) {
        if (numero[casa] >= 0 || explodiu) {
            return !explodiu;
        }
        if (campo.temMina(casa)) {
            explodiu = true;
            return false;
        }
        Deque<Integer> pilha = new ArrayDeque<>();
        pilha.push(casa);
        while (!pilha.isEmpty()) {
            int atual = pilha.pop();
            if (numero[atual] >= 0) {
                continue;
            }
            numero[atual] = campo.contagem(atual);
            bandeira[atual] = false;
            abertas++;
            if (numero[atual] == 0) {
                for (int vizinho : campo.vizinhos(atual)) {
                    if (numero[vizinho] < 0 && !campo.temMina(vizinho)) {
                        pilha.push(vizinho);
                    }
                }
            }
        }
        return true;
    }

    public void marca(int casa) {
        if (numero[casa] < 0) {
            bandeira[casa] = true;
        }
    }

    public boolean explodiu() {
        return explodiu;
    }

    public boolean ganhou() {
        return !explodiu && abertas == campo.seguras();
    }

    public int abertas() {
        return abertas;
    }

    public Campo campo() {
        return campo;
    }

    public Visao visao() {
        return new Visao(campo.largura(), campo.altura(), numero, bandeira, campo.minas());
    }
}
