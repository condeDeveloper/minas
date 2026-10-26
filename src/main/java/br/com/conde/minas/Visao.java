package br.com.conde.minas;

import java.util.ArrayList;
import java.util.List;

/**
 * O que quem joga enxerga: os numeros que ja abriram, as bandeiras e o total de
 * minas que o jogo anuncia. Nada do que esta embaixo das casas fechadas.
 *
 * <p>Toda deducao deste repositorio sai daqui, e so daqui.
 */
public final class Visao {

    /** Uma casa aberta vira uma conta: tantas minas entre estas casas fechadas. */
    public record Restricao(int soma, int[] casas) {

        public Restricao {
            casas = casas.clone();
        }

        @Override
        public int[] casas() {
            return casas.clone();
        }
    }

    private final int largura;
    private final int altura;
    private final int[] numero;
    private final boolean[] bandeira;
    private final int totalDeMinas;
    private final int[][] vizinhos;

    public Visao(int largura, int altura, int[] numero, boolean[] bandeira, int totalDeMinas) {
        this.largura = largura;
        this.altura = altura;
        this.numero = numero.clone();
        this.bandeira = bandeira.clone();
        this.totalDeMinas = totalDeMinas;
        this.vizinhos = new int[numero.length][];
        for (int casa = 0; casa < numero.length; casa++) {
            vizinhos[casa] = vizinhosDe(casa);
        }
    }

    /**
     * Uma visao escrita a mao, uma linha por linha do campo: {@code .} fechada,
     * {@code F} bandeira e os digitos de 0 a 8 nas casas abertas.
     */
    public static Visao deTexto(int totalDeMinas, String... linhas) {
        int altura = linhas.length;
        int largura = linhas[0].length();
        int[] numero = new int[largura * altura];
        boolean[] bandeira = new boolean[largura * altura];
        for (int linha = 0; linha < altura; linha++) {
            if (linhas[linha].length() != largura) {
                throw new IllegalArgumentException("linha " + (linha + 1) + " tem tamanho diferente");
            }
            for (int coluna = 0; coluna < largura; coluna++) {
                char letra = linhas[linha].charAt(coluna);
                int casa = linha * largura + coluna;
                if (letra == '.') {
                    numero[casa] = -1;
                } else if (letra == 'F') {
                    numero[casa] = -1;
                    bandeira[casa] = true;
                } else if (letra >= '0' && letra <= '8') {
                    numero[casa] = letra - '0';
                } else {
                    throw new IllegalArgumentException("letra que nao existe no desenho: " + letra);
                }
            }
        }
        return new Visao(largura, altura, numero, bandeira, totalDeMinas);
    }

    private int[] vizinhosDe(int casa) {
        int linha = casa / largura;
        int coluna = casa % largura;
        int[] fora = new int[8];
        int quantos = 0;
        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) {
                    continue;
                }
                int l = linha + dl;
                int c = coluna + dc;
                if (l >= 0 && l < altura && c >= 0 && c < largura) {
                    fora[quantos++] = l * largura + c;
                }
            }
        }
        return java.util.Arrays.copyOf(fora, quantos);
    }

    public int largura() {
        return largura;
    }

    public int altura() {
        return altura;
    }

    public int casas() {
        return numero.length;
    }

    public int[] vizinhos(int casa) {
        return vizinhos[casa];
    }

    public boolean aberta(int casa) {
        return numero[casa] >= 0;
    }

    public boolean bandeira(int casa) {
        return bandeira[casa];
    }

    /** Fechada e sem bandeira: as casas sobre as quais ainda cabe uma pergunta. */
    public boolean emAberto(int casa) {
        return numero[casa] < 0 && !bandeira[casa];
    }

    public int numero(int casa) {
        return numero[casa];
    }

    public int totalDeMinas() {
        return totalDeMinas;
    }

    public int bandeiras() {
        int quantas = 0;
        for (boolean tem : bandeira) {
            if (tem) {
                quantas++;
            }
        }
        return quantas;
    }

    /** Quantas minas ainda estao soltas, pelo que o jogo anuncia. */
    public int minasRestantes() {
        return totalDeMinas - bandeiras();
    }

    public int emAberto() {
        int quantas = 0;
        for (int casa = 0; casa < casas(); casa++) {
            if (emAberto(casa)) {
                quantas++;
            }
        }
        return quantas;
    }

    /** Uma conta por casa aberta que ainda encosta em casa fechada. */
    public List<Restricao> restricoes() {
        List<Restricao> contas = new ArrayList<>();
        for (int casa = 0; casa < casas(); casa++) {
            if (!aberta(casa)) {
                continue;
            }
            int soma = numero[casa];
            int quantas = 0;
            int[] fechadas = new int[8];
            for (int vizinho : vizinhos[casa]) {
                if (bandeira[vizinho]) {
                    soma--;
                } else if (emAberto(vizinho)) {
                    fechadas[quantas++] = vizinho;
                }
            }
            if (quantas > 0) {
                contas.add(new Restricao(soma, java.util.Arrays.copyOf(fechadas, quantas)));
            }
        }
        return contas;
    }

    /** As casas em aberto que encostam em alguma casa ja aberta. */
    public int[] fronteira() {
        return casasQue(true);
    }

    /** As casas em aberto que nao encostam em numero nenhum: o escuro do campo. */
    public int[] longe() {
        return casasQue(false);
    }

    private int[] casasQue(boolean encostando) {
        List<Integer> casas = new ArrayList<>();
        for (int casa = 0; casa < casas(); casa++) {
            if (!emAberto(casa)) {
                continue;
            }
            boolean encosta = false;
            for (int vizinho : vizinhos[casa]) {
                if (aberta(vizinho)) {
                    encosta = true;
                    break;
                }
            }
            if (encosta == encostando) {
                casas.add(casa);
            }
        }
        int[] fora = new int[casas.size()];
        for (int i = 0; i < fora.length; i++) {
            fora[i] = casas.get(i);
        }
        return fora;
    }

    /** A mesma visao com umas casas abertas e outras marcadas, sem mexer nesta. */
    public Visao com(int[] abrindo, int[] numeros, int[] marcando) {
        int[] novoNumero = numero.clone();
        boolean[] novaBandeira = bandeira.clone();
        for (int i = 0; i < abrindo.length; i++) {
            novoNumero[abrindo[i]] = numeros[i];
        }
        for (int casa : marcando) {
            novaBandeira[casa] = true;
        }
        return new Visao(largura, altura, novoNumero, novaBandeira, totalDeMinas);
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder();
        for (int linha = 0; linha < altura; linha++) {
            for (int coluna = 0; coluna < largura; coluna++) {
                int casa = linha * largura + coluna;
                if (bandeira[casa]) {
                    texto.append('F');
                } else if (numero[casa] < 0) {
                    texto.append('.');
                } else {
                    texto.append((char) ('0' + numero[casa]));
                }
            }
            texto.append('\n');
        }
        return texto.toString();
    }
}
