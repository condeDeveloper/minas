package br.com.conde.minas;

import java.util.Collection;
import java.util.TreeSet;

/** O que um metodo conseguiu provar: casas que sao mina e casas que sao livres. */
public final class Deducao {

    private final TreeSet<Integer> minas;
    private final TreeSet<Integer> livres;

    public Deducao(Collection<Integer> minas, Collection<Integer> livres) {
        this.minas = new TreeSet<>(minas);
        this.livres = new TreeSet<>(livres);
        for (int casa : this.minas) {
            if (this.livres.contains(casa)) {
                throw new IllegalArgumentException("casa " + casa + " provada mina e livre ao mesmo tempo");
            }
        }
    }

    public int[] minas() {
        return paraArranjo(minas);
    }

    public int[] livres() {
        return paraArranjo(livres);
    }

    public boolean provaMina(int casa) {
        return minas.contains(casa);
    }

    public boolean provaLivre(int casa) {
        return livres.contains(casa);
    }

    /** Quantas casas foram decididas, de um jeito ou do outro. */
    public int quantas() {
        return minas.size() + livres.size();
    }

    public boolean vazia() {
        return quantas() == 0;
    }

    private static int[] paraArranjo(TreeSet<Integer> casas) {
        int[] fora = new int[casas.size()];
        int i = 0;
        for (int casa : casas) {
            fora[i++] = casa;
        }
        return fora;
    }

    @Override
    public String toString() {
        return "minas=" + minas + " livres=" + livres;
    }
}
