package br.com.conde.minas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * O campo de verdade: onde as minas estao mesmo.
 *
 * <p>Quem joga nunca ve isto. Serve para o {@link Jogo} revelar numero por numero
 * e para os testes conferirem se o que foi provado bate com o que havia embaixo.
 *
 * <p>Uma casa e um numero so: {@code linha * largura + coluna}.
 */
public final class Campo {

    private final int largura;
    private final int altura;
    private final boolean[] mina;
    private final int[][] vizinhos;
    private final int[] contagem;

    private Campo(int largura, int altura, boolean[] mina) {
        this.largura = largura;
        this.altura = altura;
        this.mina = mina;
        this.vizinhos = new int[mina.length][];
        this.contagem = new int[mina.length];
        for (int casa = 0; casa < mina.length; casa++) {
            vizinhos[casa] = vizinhosDe(casa, largura, altura);
            int quantas = 0;
            for (int vizinho : vizinhos[casa]) {
                if (mina[vizinho]) {
                    quantas++;
                }
            }
            contagem[casa] = quantas;
        }
    }

    /** Um campo com as minas exatamente nas casas dadas. */
    public static Campo de(int largura, int altura, Collection<Integer> minas) {
        boolean[] tem = new boolean[largura * altura];
        for (int casa : minas) {
            tem[casa] = true;
        }
        return new Campo(largura, altura, tem);
    }

    /**
     * Um campo sorteado com a primeira casa e os oito vizinhos dela sem mina, que e
     * a regra de todo campo minado moderno: o primeiro clique sempre abre uma area.
     */
    public static Campo sorteado(int largura, int altura, int minas, int primeira, Random sorte) {
        int casas = largura * altura;
        List<Integer> livres = new ArrayList<>(casas);
        boolean[] proibida = new boolean[casas];
        proibida[primeira] = true;
        for (int vizinho : vizinhosDe(primeira, largura, altura)) {
            proibida[vizinho] = true;
        }
        for (int casa = 0; casa < casas; casa++) {
            if (!proibida[casa]) {
                livres.add(casa);
            }
        }
        if (minas > livres.size()) {
            throw new IllegalArgumentException("minas demais para " + largura + "x" + altura
                    + " com a primeira casa aberta: " + minas + " em " + livres.size());
        }
        boolean[] tem = new boolean[casas];
        for (int posta = 0; posta < minas; posta++) {
            int sorteada = posta + sorte.nextInt(livres.size() - posta);
            int casa = livres.get(sorteada);
            livres.set(sorteada, livres.get(posta));
            livres.set(posta, casa);
            tem[casa] = true;
        }
        return new Campo(largura, altura, tem);
    }

    private static int[] vizinhosDe(int casa, int largura, int altura) {
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
        return mina.length;
    }

    public int[] vizinhos(int casa) {
        return vizinhos[casa];
    }

    public boolean temMina(int casa) {
        return mina[casa];
    }

    /** Quantas minas encostam na casa: o numero que aparece quando ela abre. */
    public int contagem(int casa) {
        return contagem[casa];
    }

    public int minas() {
        int quantas = 0;
        for (boolean tem : mina) {
            if (tem) {
                quantas++;
            }
        }
        return quantas;
    }

    /** As casas sem mina, que precisam todas abrir para a partida ser ganha. */
    public int seguras() {
        return casas() - minas();
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder();
        for (int linha = 0; linha < altura; linha++) {
            for (int coluna = 0; coluna < largura; coluna++) {
                int casa = linha * largura + coluna;
                texto.append(mina[casa] ? '*' : (char) ('0' + contagem[casa]));
            }
            texto.append('\n');
        }
        return texto.toString();
    }
}
