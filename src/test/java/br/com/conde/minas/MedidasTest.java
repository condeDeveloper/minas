package br.com.conde.minas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Os numeros que estao escritos no README, cada um preso a uma medida. */
class MedidasTest {

    private static final int[][] NIVEIS = {
        {9, 9, 10, 400},
        {16, 16, 40, 200},
        {30, 16, 99, 100},
    };

    private static Campo campoDe(int[] nivel, int partida) {
        return Campo.sorteado(nivel[0], nivel[1], nivel[2], primeira(nivel),
                new Random(1000L + partida));
    }

    private static int primeira(int[] nivel) {
        return nivel[0] * nivel[1] / 2 + nivel[0] / 2;
    }

    @Test
    @DisplayName("README: a tabela das partidas ganhas")
    void partidasGanhas() {
        int[][] esperado = {
            // REGRA_LOCAL, REGRA_COM_TOTAL, ENUMERACAO, ENUMERACAO_E_CHUTE_BOM
            {323, 323, 383, 386},
            {93, 93, 163, 168},
            {3, 3, 35, 51},
        };
        int[][] semChuteEsperado = {
            {250, 254, 360, 360},
            {41, 41, 128, 128},
            {0, 0, 16, 16},
        };
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            Jogador.Jeito[] jeitos = Jogador.Jeito.values();
            for (int j = 0; j < jeitos.length; j++) {
                int ganhas = 0;
                int semChute = 0;
                for (int partida = 0; partida < nivel[3]; partida++) {
                    Jogador.Resultado fim = Jogador.joga(campoDe(nivel, partida), primeira(nivel),
                            jeitos[j], new Random(7000L + partida));
                    if (fim.ganhou()) {
                        ganhas++;
                    }
                    if (fim.semChute()) {
                        semChute++;
                    }
                }
                assertThat(ganhas).as("ganhas, nivel " + n + ", " + jeitos[j])
                        .isEqualTo(esperado[n][j]);
                assertThat(semChute).as("sem chute, nivel " + n + ", " + jeitos[j])
                        .isEqualTo(semChuteEsperado[n][j]);
            }
        }
    }

    @Test
    @DisplayName("README: o total de minas nao muda nada como regra local")
    void oTotalComoRegraLocalNaoMudaNada() {
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            int semOTotal = 0;
            int comOTotal = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                if (Jogador.joga(campoDe(nivel, partida), primeira(nivel),
                        Jogador.Jeito.REGRA_LOCAL, new Random(7000L + partida)).ganhou()) {
                    semOTotal++;
                }
                if (Jogador.joga(campoDe(nivel, partida), primeira(nivel),
                        Jogador.Jeito.REGRA_COM_TOTAL, new Random(7000L + partida)).ganhou()) {
                    comOTotal++;
                }
            }
            assertThat(comOTotal).as("nivel " + n).isEqualTo(semOTotal);
        }
    }

    @Test
    @DisplayName("README: a tabela das posicoes em que a regra local trava")
    void posicoesTravadas() {
        int[][] esperado = {
            // posicoes, trava, destravadas, casas a mais, so pelo total, destravadas so pelo total
            {1334, 142, 79, 3059, 1494, 7},
            {1489, 165, 63, 4142, 657, 7},
            {2069, 414, 175, 6041, 339, 7},
        };
        for (int n = 0; n < NIVEIS.length; n++) {
            assertThat(mede(NIVEIS[n])).as("nivel " + n).isEqualTo(esperado[n]);
        }
    }

    private static int[] mede(int[] nivel) {
        int posicoes = 0;
        int travou = 0;
        int destravadas = 0;
        int casasAMais = 0;
        int soPeloTotal = 0;
        int destravadasSoPeloTotal = 0;
        for (int partida = 0; partida < nivel[3]; partida++) {
            Jogo jogo = new Jogo(campoDe(nivel, partida));
            jogo.abre(primeira(nivel));
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
        return new int[] {posicoes, travou, destravadas, casasAMais, soPeloTotal,
            destravadasSoPeloTotal};
    }

    @Test
    @DisplayName("README: a enumeracao e a forca bruta concordam em 14.235 casas, sem uma discordancia")
    void enumeracaoContraForcaBruta() {
        int[][] pequenos = {{6, 6, 6, 200}, {7, 7, 8, 200}, {8, 8, 10, 200}};
        int[] casasEsperadas = {4544, 5413, 4278};
        int[] comparadasEsperadas = {435, 491, 406};
        for (int n = 0; n < pequenos.length; n++) {
            int[] nivel = pequenos[n];
            int comparadas = 0;
            int casas = 0;
            int discordancias = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                Jogo jogo = new Jogo(campoDe(nivel, partida));
                jogo.abre(primeira(nivel));
                while (!jogo.ganhou() && !jogo.explodiu()) {
                    Visao visao = jogo.visao();
                    Enumeracao enumeracao = new Enumeracao(visao);
                    if (visao.emAberto() <= 22) {
                        comparadas++;
                        ForcaBruta forca = new ForcaBruta(visao);
                        if (!enumeracao.modelos().equals(BigInteger.valueOf(forca.modelos()))) {
                            discordancias++;
                        }
                        for (int casa = 0; casa < visao.casas(); casa++) {
                            if (!visao.emAberto(casa)) {
                                continue;
                            }
                            casas++;
                            BigInteger umLado = enumeracao.modelosComMina(casa)
                                    .multiply(BigInteger.valueOf(forca.modelos()));
                            BigInteger outro = BigInteger.valueOf(forca.modelosComMina(casa))
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
            assertThat(discordancias).as("tabuleiro " + n).isZero();
            assertThat(comparadas).as("posicoes comparadas no tabuleiro " + n)
                    .isEqualTo(comparadasEsperadas[n]);
            assertThat(casas).as("casas comparadas no tabuleiro " + n)
                    .isEqualTo(casasEsperadas[n]);
        }
    }

    @Test
    @DisplayName("README: o maior pedaco teve 81 casas e so 35.478 distribuicoes de pe")
    void oTamanhoDaEnumeracao() {
        int[] maiorPedacoEsperado = {33, 63, 81};
        long[] maiorEnumeracaoEsperada = {3430, 4278, 35_478};
        for (int n = 0; n < NIVEIS.length; n++) {
            int[] nivel = NIVEIS[n];
            int maiorPedaco = 0;
            long maiorEnumeracao = 0;
            for (int partida = 0; partida < nivel[3]; partida++) {
                Jogo jogo = new Jogo(campoDe(nivel, partida));
                jogo.abre(primeira(nivel));
                while (!jogo.ganhou() && !jogo.explodiu()) {
                    Enumeracao enumeracao = new Enumeracao(jogo.visao());
                    maiorPedaco = Math.max(maiorPedaco, enumeracao.maiorComponente());
                    maiorEnumeracao = Math.max(maiorEnumeracao, enumeracao.atribuicoes());
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
            assertThat(maiorPedaco).as("maior pedaco, nivel " + n)
                    .isEqualTo(maiorPedacoEsperado[n]);
            assertThat(maiorEnumeracao).as("maior enumeracao, nivel " + n)
                    .isEqualTo(maiorEnumeracaoEsperada[n]);
        }
    }

    @Test
    @DisplayName("README: as tres posicoes mostradas aparecem mesmo onde eu disse que aparecem")
    void asPosicoesMostradas() {
        Casos.Caso soAEnumeracao = Casos.procura(Casos.Tipo.SO_A_ENUMERACAO, 9, 9, 10, 400);
        assertThat(soAEnumeracao).isNotNull();
        assertThat(soAEnumeracao.partida()).isEqualTo(3);
        assertThat(soAEnumeracao.local().quantas()).isZero();
        assertThat(soAEnumeracao.semTotal().quantas()).isEqualTo(7);
        assertThat(soAEnumeracao.enumeracao().modelos())
                .isEqualTo(BigInteger.valueOf(200_255_440L));

        Casos.Caso soComOTotal = Casos.procura(Casos.Tipo.SO_COM_O_TOTAL, 9, 9, 10, 400);
        assertThat(soComOTotal).isNotNull();
        assertThat(soComOTotal.partida()).isEqualTo(2);
        assertThat(soComOTotal.semTotal().quantas()).isZero();
        assertThat(soComOTotal.completa().quantas()).isEqualTo(22);
        assertThat(soComOTotal.enumeracao().modelos()).isEqualTo(BigInteger.TWO);

        Casos.Caso noEscuro = Casos.procura(Casos.Tipo.MELHOR_NO_ESCURO, 16, 16, 40, 200);
        assertThat(noEscuro).isNotNull();
        assertThat(noEscuro.partida()).isEqualTo(10);
        assertThat(noEscuro.visao().emAberto()).isEqualTo(240);
        int chute = noEscuro.enumeracao().melhorChute();
        assertThat(noEscuro.enumeracao().probabilidade(chute)).isBetween(0.126, 0.128);
        assertThat(noEscuro.campo().temMina(chute)).as("e nao havia mina").isFalse();
    }
}
