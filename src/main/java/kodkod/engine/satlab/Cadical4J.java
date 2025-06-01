package kodkod.engine.satlab;

import com.github.liveontologies.ipasir4j.IpasirSolver;
import com.github.liveontologies.ipasir4j.SolverTerminatedException;
import com.github.liveontologies.ipasir4j.cadical.Cadical;

import java.util.HashSet;

public class Cadical4J implements SATSolver {
    private IpasirSolver cadical;
    private int clauses;
    private int numVariables;

    public Cadical4J() {
        this.cadical = Cadical.createSolver();
    }

    public String toString() {
        return this.cadical.getSignature();
    }

    @Override
    public int numberOfVariables() {
        return this.numVariables;
    }

    @Override
    public int numberOfClauses() {
        return this.clauses;
    }

    @Override
    public void addVariables(int numVars) {
        this.numVariables += numVars;
    }

    @Override
    public boolean addClause(int[] lits) {
        for (int l : lits) {
            assert l != 0;
            this.cadical.add(l);
        }
        this.cadical.add(0);
        this.clauses++;
        return true;
    }

    @Override
    public boolean solve() throws SATAbortedException {
        try {
            return cadical.isSatisfiable();
        } catch (SolverTerminatedException e) {
            throw new SATAbortedException(e);
        }
    }

    @Override
    public boolean valueOf(int variable) {
        return cadical.val(variable) == variable;
    }

    @Override
    public void free() {

    }
}
