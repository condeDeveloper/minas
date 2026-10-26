package br.com.conde.minas;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O juiz completo: enumera TODAS as distribuicoes de minas que combinam com o que
 * esta na tela e conta em quantas delas cada casa e mina.
 *
 * <p>Casa que e mina em todas esta provada mina. Casa que nao e mina em nenhuma
 * esta provada livre. O resto tem uma probabilidade exata, que sai de uma contagem
 * inteira, sem ponto flutuante no meio do caminho.
 *
 * <p>As casas fechadas que encostam em numero se agrupam em pedacos independentes,
 * e cada pedaco e enumerado sozinho. Depois os pedacos se juntam com as casas do
 * escuro atraves do total de minas que o jogo anuncia, e e esse ultimo passo que
 * decide a maior parte dos fins de partida.
 */
public final class Enumeracao {

    private final Visao visao;
    private final boolean usaTotal;
    private final List<int[]> componentes = new ArrayList<>();
    private final List<long[]> porQuantidade = new ArrayList<>();
    private final List<long[][]> comMinaPorQuantidade = new ArrayList<>();
    private final int[] longe;
    private final BigInteger[] comMina;
    private BigInteger total = BigInteger.ZERO;
    private long atribuicoes;

    /** Enumera usando tambem o total de minas que o jogo anuncia. */
    public Enumeracao(Visao visao) {
        this(visao, true);
    }

    public Enumeracao(Visao visao, boolean usaTotal) {
        this.visao = visao;
        this.usaTotal = usaTotal;
        this.longe = visao.longe();
        this.comMina = new BigInteger[visao.casas()];
        montaComponentes();
        for (int[] casas : componentes) {
            enumeraComponente(casas);
        }
        juntaTudo();
    }

    private void montaComponentes() {
        List<Visao.Restricao> contas = visao.restricoes();
        Map<Integer, Integer> paiDe = new HashMap<>();
        for (Visao.Restricao conta : contas) {
            int[] casas = conta.casas();
            for (int casa : casas) {
                paiDe.putIfAbsent(casa, casa);
            }
            for (int i = 1; i < casas.length; i++) {
                junta(paiDe, casas[0], casas[i]);
            }
        }
        Map<Integer, List<Integer>> grupos = new HashMap<>();
        for (int casa : paiDe.keySet()) {
            grupos.computeIfAbsent(raiz(paiDe, casa), chave -> new ArrayList<>()).add(casa);
        }
        for (List<Integer> grupo : grupos.values()) {
            grupo.sort(null);
            int[] casas = new int[grupo.size()];
            for (int i = 0; i < casas.length; i++) {
                casas[i] = grupo.get(i);
            }
            componentes.add(casas);
        }
        componentes.sort((a, b) -> Integer.compare(a[0], b[0]));
    }

    private static int raiz(Map<Integer, Integer> paiDe, int casa) {
        int atual = casa;
        while (paiDe.get(atual) != atual) {
            atual = paiDe.get(atual);
        }
        return atual;
    }

    private static void junta(Map<Integer, Integer> paiDe, int uma, int outra) {
        int raizUma = raiz(paiDe, uma);
        int raizOutra = raiz(paiDe, outra);
        if (raizUma != raizOutra) {
            paiDe.put(raizOutra, raizUma);
        }
    }

    private void enumeraComponente(int[] casas) {
        Map<Integer, Integer> local = new HashMap<>();
        for (int i = 0; i < casas.length; i++) {
            local.put(casas[i], i);
        }
        List<int[]> contas = new ArrayList<>();
        List<Integer> somas = new ArrayList<>();
        for (Visao.Restricao conta : visao.restricoes()) {
            int[] dentro = new int[conta.casas().length];
            int quantas = 0;
            for (int casa : conta.casas()) {
                Integer indice = local.get(casa);
                if (indice != null) {
                    dentro[quantas++] = indice;
                }
            }
            if (quantas == conta.casas().length && quantas > 0) {
                contas.add(java.util.Arrays.copyOf(dentro, quantas));
                somas.add(conta.soma());
            }
        }

        int[][] emQuais = new int[casas.length][];
        int[] quantasContas = new int[casas.length];
        for (int[] conta : contas) {
            for (int indice : conta) {
                quantasContas[indice]++;
            }
        }
        for (int i = 0; i < casas.length; i++) {
            emQuais[i] = new int[quantasContas[i]];
            quantasContas[i] = 0;
        }
        for (int c = 0; c < contas.size(); c++) {
            for (int indice : contas.get(c)) {
                emQuais[indice][quantasContas[indice]++] = c;
            }
        }

        int[] restante = new int[contas.size()];
        int[] faltam = new int[contas.size()];
        for (int c = 0; c < contas.size(); c++) {
            restante[c] = somas.get(c);
            faltam[c] = contas.get(c).length;
        }

        long[] porK = new long[casas.length + 1];
        long[][] comMinaK = new long[casas.length][casas.length + 1];
        int[] valor = new int[casas.length];
        desce(0, 0, casas, valor, emQuais, restante, faltam, porK, comMinaK);
        porQuantidade.add(porK);
        comMinaPorQuantidade.add(comMinaK);
    }

    private void desce(int i, int minasUsadas, int[] casas, int[] valor, int[][] emQuais,
            int[] restante, int[] faltam, long[] porK, long[][] comMinaK) {
        if (i == casas.length) {
            atribuicoes++;
            porK[minasUsadas]++;
            for (int c = 0; c < casas.length; c++) {
                if (valor[c] == 1) {
                    comMinaK[c][minasUsadas]++;
                }
            }
            return;
        }
        for (int tentativa = 0; tentativa <= 1; tentativa++) {
            boolean cabe = true;
            for (int c : emQuais[i]) {
                restante[c] -= tentativa;
                faltam[c]--;
                if (restante[c] < 0 || restante[c] > faltam[c]) {
                    cabe = false;
                }
            }
            if (cabe) {
                valor[i] = tentativa;
                desce(i + 1, minasUsadas + tentativa, casas, valor, emQuais, restante, faltam,
                        porK, comMinaK);
                valor[i] = 0;
            }
            for (int c : emQuais[i]) {
                restante[c] += tentativa;
                faltam[c]++;
            }
        }
    }

    private void juntaTudo() {
        for (int casa = 0; casa < visao.casas(); casa++) {
            comMina[casa] = null;
        }
        if (!usaTotal) {
            juntaSemOTotal();
            return;
        }
        int faltam = visao.minasRestantes();
        List<BigInteger[]> polinomios = new ArrayList<>();
        for (long[] porK : porQuantidade) {
            polinomios.add(paraPolinomio(porK, faltam));
        }
        BigInteger[] doEscuro = binomios(longe.length, faltam);
        polinomios.add(doEscuro);

        BigInteger[] tudo = new BigInteger[] {BigInteger.ONE};
        for (BigInteger[] parte : polinomios) {
            tudo = multiplica(tudo, parte, faltam);
        }
        total = faltam < tudo.length ? tudo[faltam] : BigInteger.ZERO;
        if (total.signum() == 0) {
            throw new IllegalStateException("a visao nao fecha com o total de minas anunciado");
        }

        for (int comp = 0; comp < componentes.size(); comp++) {
            BigInteger[] resto = semEssaParte(polinomios, comp, faltam);
            int[] casas = componentes.get(comp);
            long[][] comMinaK = comMinaPorQuantidade.get(comp);
            for (int i = 0; i < casas.length; i++) {
                BigInteger soma = BigInteger.ZERO;
                for (int k = 0; k < comMinaK[i].length && k <= faltam; k++) {
                    if (comMinaK[i][k] == 0) {
                        continue;
                    }
                    BigInteger pesoDoResto = faltam - k < resto.length
                            ? resto[faltam - k] : BigInteger.ZERO;
                    soma = soma.add(BigInteger.valueOf(comMinaK[i][k]).multiply(pesoDoResto));
                }
                comMina[casas[i]] = soma;
            }
        }

        if (longe.length > 0) {
            BigInteger[] resto = semEssaParte(polinomios, polinomios.size() - 1, faltam);
            BigInteger[] menosUma = binomios(longe.length - 1, faltam);
            BigInteger soma = BigInteger.ZERO;
            for (int k = 1; k <= Math.min(longe.length, faltam); k++) {
                BigInteger escolhas = k - 1 < menosUma.length ? menosUma[k - 1] : BigInteger.ZERO;
                BigInteger pesoDoResto = faltam - k < resto.length
                        ? resto[faltam - k] : BigInteger.ZERO;
                soma = soma.add(escolhas.multiply(pesoDoResto));
            }
            for (int casa : longe) {
                comMina[casa] = soma;
            }
        }
    }

    private void juntaSemOTotal() {
        BigInteger produto = BigInteger.ONE;
        List<BigInteger> porComponente = new ArrayList<>();
        for (long[] porK : porQuantidade) {
            BigInteger soma = BigInteger.ZERO;
            for (long quantos : porK) {
                soma = soma.add(BigInteger.valueOf(quantos));
            }
            porComponente.add(soma);
            produto = produto.multiply(soma);
        }
        total = produto;
        if (total.signum() == 0) {
            throw new IllegalStateException("a visao nao fecha: ha numero impossivel na tela");
        }
        for (int comp = 0; comp < componentes.size(); comp++) {
            BigInteger resto = produto.divide(porComponente.get(comp));
            int[] casas = componentes.get(comp);
            long[][] comMinaK = comMinaPorQuantidade.get(comp);
            for (int i = 0; i < casas.length; i++) {
                BigInteger soma = BigInteger.ZERO;
                for (long quantos : comMinaK[i]) {
                    soma = soma.add(BigInteger.valueOf(quantos));
                }
                comMina[casas[i]] = soma.multiply(resto);
            }
        }
    }

    private static BigInteger[] semEssaParte(List<BigInteger[]> polinomios, int qual, int limite) {
        BigInteger[] resto = new BigInteger[] {BigInteger.ONE};
        for (int i = 0; i < polinomios.size(); i++) {
            if (i != qual) {
                resto = multiplica(resto, polinomios.get(i), limite);
            }
        }
        return resto;
    }

    private static BigInteger[] paraPolinomio(long[] porK, int limite) {
        int tamanho = Math.min(porK.length, limite + 1);
        BigInteger[] fora = new BigInteger[tamanho];
        for (int k = 0; k < tamanho; k++) {
            fora[k] = BigInteger.valueOf(porK[k]);
        }
        return fora;
    }

    /** Os numeros binomiais C(quantas, k), ate o limite. */
    private static BigInteger[] binomios(int quantas, int limite) {
        if (quantas < 0) {
            return new BigInteger[] {BigInteger.ONE};
        }
        int tamanho = Math.min(quantas, limite) + 1;
        BigInteger[] fora = new BigInteger[tamanho];
        fora[0] = BigInteger.ONE;
        for (int k = 1; k < tamanho; k++) {
            fora[k] = fora[k - 1].multiply(BigInteger.valueOf(quantas - k + 1))
                    .divide(BigInteger.valueOf(k));
        }
        return fora;
    }

    private static BigInteger[] multiplica(BigInteger[] uma, BigInteger[] outra, int limite) {
        int tamanho = Math.min(uma.length + outra.length - 1, limite + 1);
        BigInteger[] fora = new BigInteger[tamanho];
        java.util.Arrays.fill(fora, BigInteger.ZERO);
        for (int i = 0; i < uma.length && i < tamanho; i++) {
            if (uma[i].signum() == 0) {
                continue;
            }
            for (int j = 0; j < outra.length && i + j < tamanho; j++) {
                if (outra[j].signum() != 0) {
                    fora[i + j] = fora[i + j].add(uma[i].multiply(outra[j]));
                }
            }
        }
        return fora;
    }

    /** Quantas distribuicoes de minas combinam com o que esta na tela. */
    public BigInteger modelos() {
        return total;
    }

    /** Em quantas delas esta casa e mina. */
    public BigInteger modelosComMina(int casa) {
        return comMina[casa] == null ? BigInteger.ZERO : comMina[casa];
    }

    /** A chance exata de a casa ser mina, ou NaN quando nada se sabe dela. */
    public double probabilidade(int casa) {
        if (!visao.emAberto(casa) || comMina[casa] == null) {
            return Double.NaN;
        }
        return new java.math.BigDecimal(comMina[casa])
                .divide(new java.math.BigDecimal(total), java.math.MathContext.DECIMAL64)
                .doubleValue();
    }

    /** O que esta provado, sem chance nenhuma de erro. */
    public Deducao deducao() {
        List<Integer> minas = new ArrayList<>();
        List<Integer> livres = new ArrayList<>();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (!visao.emAberto(casa) || comMina[casa] == null) {
                continue;
            }
            if (comMina[casa].equals(total)) {
                minas.add(casa);
            } else if (comMina[casa].signum() == 0) {
                livres.add(casa);
            }
        }
        return new Deducao(minas, livres);
    }

    /** A casa menos perigosa, para quando nao ha o que provar. */
    public int melhorChute() {
        int melhor = -1;
        double menor = Double.MAX_VALUE;
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (!visao.emAberto(casa) || comMina[casa] == null) {
                continue;
            }
            double chance = probabilidade(casa);
            if (chance < menor - 1e-12) {
                menor = chance;
                melhor = casa;
            }
        }
        return melhor;
    }

    public int componentes() {
        return componentes.size();
    }

    public int maiorComponente() {
        int maior = 0;
        for (int[] casas : componentes) {
            maior = Math.max(maior, casas.length);
        }
        return maior;
    }

    /** Quantas atribuicoes completas a enumeracao chegou a montar. */
    public long atribuicoes() {
        return atribuicoes;
    }

    public int casasNoEscuro() {
        return longe.length;
    }
}
