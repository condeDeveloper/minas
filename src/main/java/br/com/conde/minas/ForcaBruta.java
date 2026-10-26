package br.com.conde.minas;

import java.util.ArrayList;
import java.util.List;

/**
 * O juiz burro: monta todo campo possivel e confere cada um inteiro.
 *
 * <p>Nao sabe o que e fronteira, nao separa pedaco nenhum e nao usa nenhuma das
 * contas da {@link Enumeracao}. Percorre todos os jeitos de espalhar as minas que
 * faltam pelas casas fechadas e, para cada jeito, confere TODAS as casas abertas,
 * uma a uma. O que sobrar e a verdade.
 *
 * <p>Nao serve para jogar: num tabuleiro de verdade sao bilhoes de jeitos. Serve
 * para dizer se a {@link Enumeracao} esta certa, e para isso tabuleiro pequeno
 * basta.
 */
public final class ForcaBruta {

    /** Acima disso a conta nao termina em tempo nenhum. */
    public static final int LIMITE_DE_CASAS_FECHADAS = 24;

    private final Visao visao;
    private final int[] fechadas;
    private final long modelos;
    private final long[] comMina;

    public ForcaBruta(Visao visao) {
        this.visao = visao;
        List<Integer> emAberto = new ArrayList<>();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (visao.emAberto(casa)) {
                emAberto.add(casa);
            }
        }
        if (emAberto.size() > LIMITE_DE_CASAS_FECHADAS) {
            throw new IllegalArgumentException("a forca bruta so vai ate "
                    + LIMITE_DE_CASAS_FECHADAS + " casas fechadas, e esta visao tem "
                    + emAberto.size());
        }
        this.fechadas = new int[emAberto.size()];
        for (int i = 0; i < fechadas.length; i++) {
            fechadas[i] = emAberto.get(i);
        }
        this.comMina = new long[visao.casas()];
        long quantos = 0;
        int faltam = visao.minasRestantes();
        for (long jeito = 0; jeito < (1L << fechadas.length); jeito++) {
            if (Long.bitCount(jeito) != faltam || !fecha(jeito)) {
                continue;
            }
            quantos++;
            for (int i = 0; i < fechadas.length; i++) {
                if ((jeito & (1L << i)) != 0) {
                    comMina[fechadas[i]]++;
                }
            }
        }
        this.modelos = quantos;
    }

    /** Este jeito de espalhar as minas explica todos os numeros da tela? */
    private boolean fecha(long jeito) {
        boolean[] temMina = new boolean[visao.casas()];
        for (int i = 0; i < fechadas.length; i++) {
            if ((jeito & (1L << i)) != 0) {
                temMina[fechadas[i]] = true;
            }
        }
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (visao.bandeira(casa)) {
                temMina[casa] = true;
            }
        }
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (!visao.aberta(casa)) {
                continue;
            }
            int quantas = 0;
            for (int vizinho : visao.vizinhos(casa)) {
                if (temMina[vizinho]) {
                    quantas++;
                }
            }
            if (quantas != visao.numero(casa)) {
                return false;
            }
        }
        return true;
    }

    public long modelos() {
        return modelos;
    }

    public long modelosComMina(int casa) {
        return comMina[casa];
    }

    public double probabilidade(int casa) {
        if (!visao.emAberto(casa) || modelos == 0) {
            return Double.NaN;
        }
        return (double) comMina[casa] / modelos;
    }

    public Deducao deducao() {
        List<Integer> minas = new ArrayList<>();
        List<Integer> livres = new ArrayList<>();
        for (int casa : fechadas) {
            if (comMina[casa] == modelos) {
                minas.add(casa);
            } else if (comMina[casa] == 0) {
                livres.add(casa);
            }
        }
        return new Deducao(minas, livres);
    }

    public int casasFechadas() {
        return fechadas.length;
    }
}
