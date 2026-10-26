package br.com.conde.minas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Joga uma partida inteira sozinho: abre o que esta provado livre, marca o que
 * esta provado mina e, quando nao sobra nada provado, chuta.
 *
 * <p>Os quatro jeitos existem para separar duas coisas que vivem misturadas: o que
 * vem de deduzir melhor e o que vem de chutar melhor.
 */
public final class Jogador {

    public enum Jeito {
        /** So as duas regras de olhar um numero por vez. Chute as cegas. */
        REGRA_LOCAL,
        /** As duas regras mais o total de minas anunciado. Chute as cegas. */
        REGRA_COM_TOTAL,
        /** A enumeracao completa, mas ainda chutando as cegas. */
        ENUMERACAO,
        /** A enumeracao completa, chutando na casa de menor chance. */
        ENUMERACAO_E_CHUTE_BOM
    }

    /** Como a partida terminou. */
    public record Resultado(boolean ganhou, int chutes, int chutesQueExplodiram, int abertas,
            int casasSeguras) {

        public boolean semChute() {
            return chutes == 0;
        }
    }

    private Jogador() {
    }

    public static Resultado joga(Campo campo, int primeira, Jeito jeito, Random sorte) {
        Jogo jogo = new Jogo(campo);
        jogo.abre(primeira);
        int chutes = 0;
        int explodiram = 0;
        while (!jogo.ganhou() && !jogo.explodiu()) {
            Visao visao = jogo.visao();
            Deducao deducao = deduz(visao, jeito);
            for (int casa : deducao.minas()) {
                jogo.marca(casa);
            }
            int[] livres = deducao.livres();
            if (livres.length > 0) {
                for (int casa : livres) {
                    jogo.abre(casa);
                }
                continue;
            }
            int chute = chuta(visao, jeito, sorte);
            if (chute < 0) {
                break;
            }
            chutes++;
            if (!jogo.abre(chute)) {
                explodiram++;
            }
        }
        return new Resultado(jogo.ganhou(), chutes, explodiram, jogo.abertas(), campo.seguras());
    }

    /** O que o jeito escolhido consegue provar nesta visao. */
    public static Deducao deduz(Visao visao, Jeito jeito) {
        return switch (jeito) {
            case REGRA_LOCAL -> RegraLocal.aplica(visao);
            case REGRA_COM_TOTAL -> RegraLocal.comTotal(visao);
            case ENUMERACAO, ENUMERACAO_E_CHUTE_BOM -> new Enumeracao(visao).deducao();
        };
    }

    private static int chuta(Visao visao, Jeito jeito, Random sorte) {
        if (jeito == Jeito.ENUMERACAO_E_CHUTE_BOM) {
            int melhor = new Enumeracao(visao).melhorChute();
            if (melhor >= 0) {
                return melhor;
            }
        }
        List<Integer> abertas = new ArrayList<>();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (visao.emAberto(casa)) {
                abertas.add(casa);
            }
        }
        if (abertas.isEmpty()) {
            return -1;
        }
        return abertas.get(sorte.nextInt(abertas.size()));
    }
}
