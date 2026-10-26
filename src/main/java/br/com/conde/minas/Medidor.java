package br.com.conde.minas;

import java.util.Locale;
import java.util.Random;

/** As tabelas do README, uma por argumento. */
public final class Medidor {

    private Medidor() {
    }

    /** As tres dificuldades de sempre: largura, altura, minas, partidas medidas. */
    private static final int[][] NIVEIS = {
        {9, 9, 10, 400},
        {16, 16, 40, 200},
        {30, 16, 99, 100},
    };

    private static final String[] NOMES = {"iniciante", "intermediario", "especialista"};

    public static void main(String[] args) {
        String qual = args.length == 0 ? "tudo" : args[0];
        switch (qual) {
            case "vitorias" -> vitorias();
            case "travadas" -> travadas();
            case "custo" -> custo();
            case "casos" -> casos();
            case "confronto" -> confronto();
            default -> {
                confronto();
                vitorias();
                travadas();
                custo();
                casos();
            }
        }
    }

    private static Campo campoDe(int[] nivel, int partida) {
        Random sorte = new Random(1000L + partida);
        int primeira = nivel[0] * nivel[1] / 2 + nivel[0] / 2;
        return Campo.sorteado(nivel[0], nivel[1], nivel[2], primeira, sorte);
    }

    private static int primeiraCasa(int[] nivel) {
        return nivel[0] * nivel[1] / 2 + nivel[0] / 2;
    }

    static void vitorias() {
        System.out.println("== partidas ganhas por jeito ==");
        System.out.println("nivel                 jeito                    ganhas  sem chute  chutes  explodidos");
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            for (Jogador.Jeito jeito : Jogador.Jeito.values()) {
                int ganhas = 0;
                int semChute = 0;
                int chutes = 0;
                int explodiram = 0;
                for (int partida = 0; partida < nivel[3]; partida++) {
                    Jogador.Resultado fim = Jogador.joga(campoDe(nivel, partida),
                            primeiraCasa(nivel), jeito, new Random(7000L + partida));
                    if (fim.ganhou()) {
                        ganhas++;
                    }
                    if (fim.semChute()) {
                        semChute++;
                    }
                    chutes += fim.chutes();
                    explodiram += fim.chutesQueExplodiram();
                }
                System.out.printf(Locale.ROOT, "%-21s %-22s %6d %10d %7d %11d%n",
                        NOMES[n] + " (" + nivel[3] + ")", jeito, ganhas, semChute, chutes,
                        explodiram);
            }
        }
    }

    static void travadas() {
        System.out.println("== posicoes em que a regra local trava ==");
        System.out.println("nivel          posicoes  trava  destravadas  casas a mais  so pelo total  destravadas so pelo total");
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            int posicoes = 0;
            int travou = 0;
            int destravadas = 0;
            int casasAMais = 0;
            int soPeloTotal = 0;
            int destravadasSoPeloTotal = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                Campo campo = campoDe(nivel, partida);
                Jogo jogo = new Jogo(campo);
                jogo.abre(primeiraCasa(nivel));
                while (!jogo.ganhou() && !jogo.explodiu()) {
                    Visao visao = jogo.visao();
                    posicoes++;
                    Deducao local = RegraLocal.aplica(visao);
                    Enumeracao enumeracao = new Enumeracao(visao);
                    Deducao completa = enumeracao.deducao();
                    Deducao semTotal = new Enumeracao(visao, false).deducao();
                    if (local.vazia()) {
                        travou++;
                        if (!completa.vazia()) {
                            destravadas++;
                            if (semTotal.vazia()) {
                                destravadasSoPeloTotal++;
                            }
                        }
                    }
                    casasAMais += completa.quantas() - local.quantas();
                    soPeloTotal += completa.quantas() - semTotal.quantas();
                    for (int casa : completa.minas()) {
                        jogo.marca(casa);
                    }
                    if (completa.livres().length > 0) {
                        for (int casa : completa.livres()) {
                            jogo.abre(casa);
                        }
                    } else {
                        int chute = enumeracao.melhorChute();
                        if (chute < 0) {
                            break;
                        }
                        jogo.abre(chute);
                    }
                }
            }
            System.out.printf(Locale.ROOT, "%-14s %8d %6d %12d %13d %14d %25d%n",
                    NOMES[n], posicoes, travou, destravadas, casasAMais, soPeloTotal,
                    destravadasSoPeloTotal);
        }
    }

    static void custo() {
        System.out.println("== o tamanho da enumeracao ==");
        System.out.println("nivel          posicoes  maior pedaco  maior enumeracao  media de pedacos");
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            int posicoes = 0;
            int maiorPedaco = 0;
            long maiorEnumeracao = 0;
            long pedacos = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                Campo campo = campoDe(nivel, partida);
                Jogo jogo = new Jogo(campo);
                jogo.abre(primeiraCasa(nivel));
                while (!jogo.ganhou() && !jogo.explodiu()) {
                    Visao visao = jogo.visao();
                    Enumeracao enumeracao = new Enumeracao(visao);
                    posicoes++;
                    maiorPedaco = Math.max(maiorPedaco, enumeracao.maiorComponente());
                    maiorEnumeracao = Math.max(maiorEnumeracao, enumeracao.atribuicoes());
                    pedacos += enumeracao.componentes();
                    Deducao completa = enumeracao.deducao();
                    for (int casa : completa.minas()) {
                        jogo.marca(casa);
                    }
                    if (completa.livres().length > 0) {
                        for (int casa : completa.livres()) {
                            jogo.abre(casa);
                        }
                    } else {
                        int chute = enumeracao.melhorChute();
                        if (chute < 0) {
                            break;
                        }
                        jogo.abre(chute);
                    }
                }
            }
            System.out.printf(Locale.ROOT, "%-14s %8d %13d %17d %17.2f%n",
                    NOMES[n], posicoes, maiorPedaco, maiorEnumeracao, (double) pedacos / posicoes);
        }
    }

    static void confronto() {
        System.out.println("== a enumeracao contra a forca bruta ==");
        System.out.println("tabuleiro  partidas  posicoes  comparadas  casas  discordancias  maior varredura");
        int[][] pequenos = {{6, 6, 6, 200}, {7, 7, 8, 200}, {8, 8, 10, 200}};
        for (int[] nivel : pequenos) {
            int primeira = nivel[0] * nivel[1] / 2 + nivel[0] / 2;
            int posicoes = 0;
            int comparadas = 0;
            long casas = 0;
            int discordancias = 0;
            long maiorConta = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                Campo campo = Campo.sorteado(nivel[0], nivel[1], nivel[2], primeira,
                        new Random(1000L + partida));
                Jogo jogo = new Jogo(campo);
                jogo.abre(primeira);
                while (!jogo.ganhou() && !jogo.explodiu()) {
                    Visao visao = jogo.visao();
                    posicoes++;
                    Enumeracao enumeracao = new Enumeracao(visao);
                    if (visao.emAberto() <= 22) {
                        comparadas++;
                        ForcaBruta forca = new ForcaBruta(visao);
                        maiorConta = Math.max(maiorConta, forca.casasFechadas());
                        if (!enumeracao.modelos().equals(
                                java.math.BigInteger.valueOf(forca.modelos()))) {
                            discordancias++;
                        }
                        for (int casa = 0; casa < visao.casas(); casa++) {
                            if (!visao.emAberto(casa)) {
                                continue;
                            }
                            casas++;
                            java.math.BigInteger umLado = enumeracao.modelosComMina(casa)
                                    .multiply(java.math.BigInteger.valueOf(forca.modelos()));
                            java.math.BigInteger outro = java.math.BigInteger
                                    .valueOf(forca.modelosComMina(casa))
                                    .multiply(enumeracao.modelos());
                            if (!umLado.equals(outro)) {
                                discordancias++;
                            }
                        }
                    }
                    Deducao completa = enumeracao.deducao();
                    for (int casa : completa.minas()) {
                        jogo.marca(casa);
                    }
                    if (completa.livres().length > 0) {
                        for (int casa : completa.livres()) {
                            jogo.abre(casa);
                        }
                    } else {
                        int chute = enumeracao.melhorChute();
                        if (chute < 0) {
                            break;
                        }
                        jogo.abre(chute);
                    }
                }
            }
            System.out.printf(Locale.ROOT, "%-10s %8d %9d %11d %6d %14d %14s%n",
                    nivel[0] + "x" + nivel[1] + "/" + nivel[2], nivel[3], posicoes, comparadas,
                    casas, discordancias, "2^" + maiorConta);
        }
    }

    static void casos() {
        System.out.println("== posicoes achadas jogando ==");
        Casos.mostra();
    }
}
