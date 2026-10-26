package br.com.conde.minas;

import java.util.Locale;
import java.util.Random;

/**
 * Procura nas partidas medidas a primeira posicao de cada tipo interessante e
 * mostra o tabuleiro inteiro, para quem le conferir na mao.
 *
 * <p>Nada aqui e inventado: sao posicoes que apareceram jogando, com o numero da
 * partida anotado do lado, e cada uma e conferida contra o campo de verdade antes
 * de ser impressa.
 */
public final class Casos {

    /** Os tres tipos de posicao que valem a pena mostrar. */
    public enum Tipo {
        /** A regra local nao prova nada e a enumeracao prova, sem precisar do total. */
        SO_A_ENUMERACAO,
        /** Sem o total de minas nao se prova nada; com ele, prova-se. */
        SO_COM_O_TOTAL,
        /** Nao ha o que provar, e o chute mais seguro esta longe de todo numero. */
        MELHOR_NO_ESCURO
    }

    /** Uma posicao achada, com o que cada metodo provou nela. */
    public record Caso(int partida, Visao visao, Deducao local, Deducao semTotal,
            Deducao completa, Enumeracao enumeracao, Campo campo) {
    }

    private Casos() {
    }

    public static void mostra() {
        imprime("a regra local trava e a enumeracao resolve",
                procura(Tipo.SO_A_ENUMERACAO, 9, 9, 10, 400));
        imprime("sem o total de minas nao se prova nada; com ele, prova-se",
                procura(Tipo.SO_COM_O_TOTAL, 9, 9, 10, 400));
        imprime("o chute mais seguro esta no escuro, longe de todo numero",
                procura(Tipo.MELHOR_NO_ESCURO, 16, 16, 40, 200));
    }

    /** Joga as partidas em ordem e devolve a primeira posicao do tipo pedido. */
    public static Caso procura(Tipo tipo, int largura, int altura, int minas, int partidas) {
        int primeira = largura * altura / 2 + largura / 2;
        for (int partida = 0; partida < partidas; partida++) {
            Campo campo = Campo.sorteado(largura, altura, minas, primeira,
                    new Random(1000L + partida));
            Jogo jogo = new Jogo(campo);
            jogo.abre(primeira);
            while (!jogo.ganhou() && !jogo.explodiu()) {
                Visao visao = jogo.visao();
                Enumeracao enumeracao = new Enumeracao(visao);
                Deducao completa = enumeracao.deducao();
                Deducao local = RegraLocal.aplica(visao);
                Deducao semTotal = new Enumeracao(visao, false).deducao();
                if (serve(tipo, visao, enumeracao, local, semTotal, completa)) {
                    return new Caso(partida, visao, local, semTotal, completa, enumeracao, campo);
                }
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
        return null;
    }

    private static boolean serve(Tipo tipo, Visao visao, Enumeracao enumeracao, Deducao local,
            Deducao semTotal, Deducao completa) {
        return switch (tipo) {
            case SO_A_ENUMERACAO -> local.vazia() && !semTotal.vazia();
            case SO_COM_O_TOTAL -> semTotal.vazia() && !completa.vazia();
            case MELHOR_NO_ESCURO -> completa.vazia() && noEscuroEMelhor(visao, enumeracao);
        };
    }

    /** O melhor chute esta no escuro e e estritamente melhor que toda a fronteira. */
    public static boolean noEscuroEMelhor(Visao visao, Enumeracao enumeracao) {
        if (visao.longe().length == 0) {
            return false;
        }
        double noEscuro = enumeracao.probabilidade(visao.longe()[0]);
        for (int casa : visao.fronteira()) {
            if (enumeracao.probabilidade(casa) <= noEscuro + 1e-12) {
                return false;
            }
        }
        return true;
    }

    private static void imprime(String titulo, Caso caso) {
        System.out.println();
        System.out.println("-- " + titulo);
        if (caso == null) {
            System.out.println("   nao apareceu nas partidas medidas");
            return;
        }
        Visao visao = caso.visao();
        System.out.printf(Locale.ROOT, "   partida %d, %d casas em aberto, %d minas soltas%n",
                caso.partida(), visao.emAberto(), visao.minasRestantes());
        System.out.printf(Locale.ROOT,
                "   regra local: %d casas | enumeracao sem o total: %d | com o total: %d"
                        + " | %s distribuicoes de pe%n",
                caso.local().quantas(), caso.semTotal().quantas(), caso.completa().quantas(),
                caso.enumeracao().modelos());
        System.out.println(desenho(visao, caso.completa(), caso.enumeracao()));
        confere(caso);
    }

    /**
     * O tabuleiro com o que a enumeracao provou por cima: {@code X} mina provada,
     * {@code o} casa provada livre, {@code ?} o melhor chute.
     */
    public static String desenho(Visao visao, Deducao deducao, Enumeracao enumeracao) {
        char[] marca = new char[visao.casas()];
        for (int casa : deducao.minas()) {
            marca[casa] = 'X';
        }
        for (int casa : deducao.livres()) {
            marca[casa] = 'o';
        }
        if (deducao.vazia()) {
            int chute = enumeracao.melhorChute();
            if (chute >= 0) {
                marca[chute] = '?';
            }
        }
        StringBuilder texto = new StringBuilder();
        for (int linha = 0; linha < visao.altura(); linha++) {
            texto.append("   ");
            for (int coluna = 0; coluna < visao.largura(); coluna++) {
                int casa = linha * visao.largura() + coluna;
                if (marca[casa] != 0) {
                    texto.append(marca[casa]);
                } else if (visao.bandeira(casa)) {
                    texto.append('F');
                } else if (!visao.aberta(casa)) {
                    texto.append('.');
                } else if (visao.numero(casa) == 0) {
                    texto.append(' ');
                } else {
                    texto.append((char) ('0' + visao.numero(casa)));
                }
            }
            texto.append('\n');
        }
        return texto.toString();
    }

    /** Confere contra o campo de verdade: provou mina onde tinha mina? */
    private static void confere(Caso caso) {
        for (int casa : caso.completa().minas()) {
            if (!caso.campo().temMina(casa)) {
                throw new IllegalStateException("provou mina onde nao tinha: casa " + casa);
            }
        }
        for (int casa : caso.completa().livres()) {
            if (caso.campo().temMina(casa)) {
                throw new IllegalStateException("provou livre onde tinha mina: casa " + casa);
            }
        }
        if (caso.completa().vazia()) {
            int chute = caso.enumeracao().melhorChute();
            double melhorDaFronteira = 1;
            for (int casa : caso.visao().fronteira()) {
                melhorDaFronteira = Math.min(melhorDaFronteira,
                        caso.enumeracao().probabilidade(casa));
            }
            System.out.printf(Locale.ROOT,
                    "   o ? tem %.1f%% de chance de mina; o melhor da fronteira tem %.1f%%"
                            + ", e debaixo do ? %s%n",
                    100 * caso.enumeracao().probabilidade(chute), 100 * melhorDaFronteira,
                    caso.campo().temMina(chute) ? "havia mina" : "nao havia mina");
        } else {
            System.out.println("   conferido contra o campo: nenhuma casa provada errada");
        }
    }
}
