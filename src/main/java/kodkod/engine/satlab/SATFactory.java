/* 
 * Kodkod -- Copyright (c) 2005-present, Emina Torlak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package kodkod.engine.satlab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sat4j.minisat.SolverFactory;

/**
 * A factory for generating SATSolver instances of a given type.
 * Built-in support is provided for many solvers, including 
 * <a href="http://www.sat4j.org/">SAT4J</a> 
 * and the <a href="http://www.cs.chalmers.se/Cs/Research/FormalMethods/MiniSat/">MiniSat</a> 
 * solver by Niklas E&eacute;n and Niklas S&ouml;rensson.
 * @author Emina Torlak
 */
public abstract class SATFactory {
	
	/**
	 * Constructs a new instance of SATFactory.
	 */
	protected SATFactory() {}
	
	/**
	 * Returns true iff the given factory generates solvers that 
	 * are available for use on this system.
	 * @return true iff the given factory generates solvers that 
	 * are available for use on this system.
	 */
	public static final boolean available(SATFactory factory) {
		SATSolver solver = null;
		try {
			solver = factory.instance();
			solver.addVariables(1);
			solver.addClause(new int[]{1});
			return solver.solve();
		} catch (RuntimeException|UnsatisfiedLinkError t) {	
			return false;
		} finally {
			if (solver!=null) {
				solver.free();
			}
		}
	}

//	/**
//	 * The factory that produces instances of the default sat4j solver.
//	 * @see org.sat4j.core.ASolverFactory#defaultSolver()
//	 */
//	public static final SATFactory DefaultSAT4J = new SATFactory() {
//		public SATSolver instance() {
//			return new SAT4J(SolverFactory.instance().defaultSolver());
//		}
//		public String toString() { return "DefaultSAT4J"; }
//	};
//
//	/**
//	 * The factory that produces instances of the "light" sat4j solver.  The
//	 * light solver is suitable for solving many small instances of SAT problems.
//	 * @see org.sat4j.core.ASolverFactory#lightSolver()
//	 */
//	public static final SATFactory LightSAT4J = new SATFactory() {
//		public SATSolver instance() {
//			return new SAT4J(SolverFactory.instance().lightSolver());
//		}
//		public String toString() { return "LightSAT4J"; }
//	};

	public static final SATFactory Cadical4J = new SATFactory() {
		public SATSolver instance() {
			return new Cadical4J();
		}
		public String toString() { return "Cadical4J"; }
	};
	
	/**
	 * Searches the {@code java.library.path} for an executable with the given name. Returns a fully 
	 * qualified path to the first found executable.  Otherwise returns null.
	 * @return a fully qualified path to an executable with the given name, or null if no executable 
	 * is found.
	 */
	private static String findStaticLibrary(String name) { 
		final String[] dirs = System.getProperty("java.library.path").split(System.getProperty("path.separator"));
		
		for(int i = dirs.length-1; i >= 0; i--) {
			final File file = new File(dirs[i]+File.separator+name);
			if (file.canExecute())
				return file.getAbsolutePath();
		}
		
		return null;
	}
	
	/**
	 * Returns a SATFactory that produces instances of the specified
	 * SAT4J solver.  For the list of available SAT4J solvers see
	 * {@link org.sat4j.core.ASolverFactory#solverNames() org.sat4j.core.ASolverFactory#solverNames()}.
	 * @requires solverName is a valid solver name
	 * @return a SATFactory that returns the instances of the specified
	 * SAT4J solver
	 * @see org.sat4j.core.ASolverFactory#solverNames()
	 */
	public static final SATFactory sat4jFactory(final String solverName) {
		return new SATFactory() {
			@Override
			public SATSolver instance() {
				return new SAT4J(SolverFactory.instance().createSolverByName(solverName));
			}
			public String toString() { return solverName; }
		};
	}
	
	/**
	 * Returns a SATFactory that produces SATSolver wrappers for the external
	 * SAT solver specified by the executable parameter.  The solver's input
	 * and output formats must conform to the 
	 * <a href="http://www.satcompetition.org/2011/rules.pdf">SAT competition standards</a>.  The solver
	 * will be called with the specified options, and it is expected to write properly formatted
	 * output to standard out.  If the {@code cnf} string is non-null,  it will be 
	 * used as the file name for generated CNF files by all solver instances that the factory generates.  
	 * If {@code cnf} null, each solver instance will use an automatically generated temporary file, which 
	 * will be deleted when the solver instance is garbage-collected. The {@code cnf} file, if provided, is not 
	 * automatically deleted; it is the caller's responsibility to  delete it when no longer needed.  
	 * External solvers are never incremental.
	 * @return  SATFactory that produces SATSolver wrappers for the specified external
	 * SAT solver
	 */
	public static final SATFactory externalFactory(final String executable, final String cnf, final String... options) {
		return new SATFactory() {

			@Override
			public SATSolver instance() {
				if (cnf != null) {
					return new ExternalSolver(executable, cnf, false, options);
				} else {
					try {
						return new ExternalSolver(executable, 
								File.createTempFile("kodkod", String.valueOf(executable.hashCode())).getAbsolutePath(), 
								true, options);
					} catch (IOException e) {
						throw new SATAbortedException("Could not create a temporary file.", e);
					}
				}
			}
			
			@Override
			public boolean incremental() {
				return false;
			}
			
			public String toString() {
				return (new File(executable)).getName();
			}
		};
	}
	
	
	/**
	 * Returns an instance of a SATSolver produced by this factory.
	 * @return a SATSolver instance
	 */
	public abstract SATSolver instance();
	
	/**
	 * Returns true if the solvers returned by this.instance() are
	 * {@link SATProver SATProvers}.  Otherwise returns false.
	 * @return true if the solvers returned by this.instance() are
	 * {@link SATProver SATProvers}.  Otherwise returns false.
	 */
	public boolean prover() {
		return false;
	}
	
	/**
	 * Returns true if the solvers returned by this.instance() are incremental;
	 * i.e. if clauses/variables can be added to the solver between multiple
	 * calls to solve().
	 * @return true if the solvers returned by this.instance() are incremental
	 */
	public boolean incremental() {
		return true;
	}

}
