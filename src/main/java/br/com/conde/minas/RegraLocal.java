package br.com.conde.minas;

import java.util.ArrayList;
import java.util.List;

/**
 * As duas regras que todo mundo usa jogando: olhar um numero de cada vez.
 *
 * <ul>
 *   <li>o numero ja esta todo marcado, entao o resto da vizinhanca e livre;</li>
 *   <li>o que falta marcar e exatamente o que sobrou fechado, entao e tudo mina.</li>
 * </ul>
 *
 * <p>As duas se realimentam: uma mina provada muda a conta das casas vizinhas.
 * Entao a regra roda ate parar de descobrir coisa nova.
 *
 * <p>E so isso. Nenhuma das duas olha duas casas abertas ao mesmo tempo, e e
 * justamente aqui que a enumeracao vai passar na frente.
 */
public final class RegraLocal {

    private RegraLocal() {
    }

    /** So as duas regras, sem usar o total de minas que o jogo anuncia. */
    public static Deducao aplica(Visao visao) {
        return aplica(visao, false);
    }

    /**
     * As duas regras mais as duas que o total de minas permite: se o que falta
     * marcar e zero, todo o resto e livre; se bate com o que sobrou, e tudo mina.
     */
    public static Deducao comTotal(Visao visao) {
        return aplica(visao, true);
    }

    private static Deducao aplica(Visao visao, boolean usaTotal) {
        Boolean[] sabido = new Boolean[visao.casas()];
        List<Visao.Restricao> contas = visao.restricoes();
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Visao.Restricao conta : contas) {
                int soma = conta.soma();
                List<Integer> abertas = new ArrayList<>();
                for (int casa : conta.casas()) {
                    if (sabido[casa] == null) {
                        abertas.add(casa);
                    } else if (sabido[casa]) {
                        soma--;
                    }
                }
                if (abertas.isEmpty()) {
                    continue;
                }
                if (soma == 0) {
                    for (int casa : abertas) {
                        sabido[casa] = false;
                    }
                    mudou = true;
                } else if (soma == abertas.size()) {
                    for (int casa : abertas) {
                        sabido[casa] = true;
                    }
                    mudou = true;
                }
            }
            if (usaTotal && contaOTotal(visao, sabido)) {
                mudou = true;
            }
        }
        List<Integer> minas = new ArrayList<>();
        List<Integer> livres = new ArrayList<>();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (sabido[casa] != null) {
                (sabido[casa] ? minas : livres).add(casa);
            }
        }
        return new Deducao(minas, livres);
    }

    private static boolean contaOTotal(Visao visao, Boolean[] sabido) {
        int faltam = visao.minasRestantes();
        List<Integer> abertas = new ArrayList<>();
        for (int casa = 0; casa < visao.casas(); casa++) {
            if (!visao.emAberto(casa)) {
                continue;
            }
            if (sabido[casa] == null) {
                abertas.add(casa);
            } else if (sabido[casa]) {
                faltam--;
            }
        }
        if (abertas.isEmpty()) {
            return false;
        }
        if (faltam == 0) {
            for (int casa : abertas) {
                sabido[casa] = false;
            }
            return true;
        }
        if (faltam == abertas.size()) {
            for (int casa : abertas) {
                sabido[casa] = true;
            }
            return true;
        }
        return false;
    }
}
