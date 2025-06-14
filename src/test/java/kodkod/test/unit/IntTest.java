package kodkod.test.unit;
import static kodkod.ast.operator.IntCompOperator.EQ;
import static kodkod.ast.operator.IntCompOperator.GT;
import static kodkod.ast.operator.IntCompOperator.GTE;
import static kodkod.ast.operator.IntCompOperator.LT;
import static kodkod.ast.operator.IntCompOperator.LTE;
import static kodkod.ast.operator.IntOperator.AND;
import static kodkod.ast.operator.IntOperator.DIVIDE;
import static kodkod.ast.operator.IntOperator.MINUS;
import static kodkod.ast.operator.IntOperator.MODULO;
import static kodkod.ast.operator.IntOperator.MULTIPLY;
import static kodkod.ast.operator.IntOperator.OR;
import static kodkod.ast.operator.IntOperator.PLUS;
import static kodkod.ast.operator.IntOperator.SHA;
import static kodkod.ast.operator.IntOperator.SHL;
import static kodkod.ast.operator.IntOperator.SHR;
import static kodkod.ast.operator.IntOperator.XOR;
import static kodkod.engine.config.Options.IntEncoding.TWOSCOMPLEMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.ast.operator.IntCompOperator;
import kodkod.ast.operator.IntOperator;
import kodkod.engine.Evaluator;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.util.ints.IntRange;
import kodkod.util.ints.Ints;

import org.junit.Before;
import org.junit.Test;
/**
 * Tests translation of cardinality expressions/formulas.
 * 
 * @author Emina Torlak
 */
public class IntTest {
	private static final int SIZE = 16;
	private final TupleFactory factory;
	private final Solver solver;
	private final Relation r1;
	private Bounds bounds;
	
	public IntTest() {
		this.solver = new Solver();
		List<String> atoms = new ArrayList<String>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			atoms.add(String.valueOf(i));
		}
		final Universe universe = new Universe(atoms);
		this.factory = universe.factory();
		r1 = Relation.unary("r1");
	}

	@Before
	public void setUp() throws Exception {
		bounds = new Bounds(factory.universe());
		solver.options().setSolver(SATFactory.Cadical4J);
	}

	
	
	private Solution solve(Formula formula) {
//		  System.out.println(formula); 
//		  System.out.println(bounds);
			return solver.solve(formula, bounds);
		
	}

		
	private IntExpression[] constants() {
		final Options options = solver.options();
		final IntRange range = options.integers();
		final int min = range.min(), max = range.max();
		final IntExpression[] vals = new IntExpression[max-min+1];
		for(int i = min; i <= max; i++) {
			vals[i-min] = constant(i);
		}
		return vals;
	}
	
	private IntExpression[] nonConstants() {
		final Options options = solver.options();
			
		final IntRange range = options.integers();
		final int min = range.min(), max = range.max();
		final int size = range.size();
		
		final Relation[] r = new Relation[size];
				
		final TupleFactory f = bounds.universe().factory();
		for(int i = 0; i < size; i++) {
			int arity = i%3 + 1;
			r[i] = Relation.nary("r"+i, arity);
			
			TupleSet b = f.noneOf(arity);
			for(int j = (i/3)*((int)Math.pow(SIZE, arity-1)), jmax = j+size; j < jmax; j++ ) {
				b.add(f.tuple(arity, j%b.capacity()));
			}
			
			bounds.bound(r[i], b);
		}
		
		final IntExpression[] vals = new IntExpression[max-min+1];
		for(int i = 0; i < size; i++) {
			vals[i] = i+min < 0 ? r[i].count().negate() : r[i].count();
		}
		
		return vals;
	}
	
	private static IntExpression constant(int i) { return IntConstant.constant(i); }
	
	private final void testBinOp(IntOperator op, IntExpression ei, IntExpression ej, int i, int j, int result, int mask) {
		final IntExpression e = ei.compose(op, ej);
		final Formula f = ei.eq(constant(i)).and(ej.eq(constant(j))).and(e.eq(constant(result)));
		final Solution s = solve(f);
		//if (s.instance()==null)
		//	System.out.println(f + " no solution!");
		assertNotNull(s.instance());
		final Evaluator eval = new Evaluator(s.instance(), solver.options());
		//System.out.println(f + ", expected: " + (result & mask) + ", actual: " + (eval.evaluate(e) & mask));
		assertEquals(result & mask, eval.evaluate(e) & mask);
		
	}
	
	/**
	 * Tests all binary ops for this.solver.options and range of vals.
	 * @requires this.solver.options.intEncoding = binary 
	 * @requires vals contains int expressions that represent all 
	 * integers allowed by this.solver.options, in proper sequence
	 */
	private final void test2sComplementBinOps(IntExpression[] vals) {
		final Options options = solver.options();
		final int bw = options.bitwidth();
		final IntRange range = options.integers();
		final int min = range.min(), max = range.max();
		final int mask = ~(-1 << bw);
		
		for(int i = min; i <= max; i++) {
				IntExpression vi = vals[i-min];
			
			for(int j = min; j <= max; j++) {
				
				IntExpression vj = vals[j-min];
				testBinOp(PLUS, vi, vj, i, j, i+j, mask);
				testBinOp(MINUS, vi, vj, i, j, i-j, mask);
				testBinOp(MULTIPLY, vi, vj, i, j, i*j, mask);
				
				if (j!=0) {
					testBinOp(DIVIDE, vi, vj, i, j, i/j, mask);
					testBinOp(MODULO, vi, vj, i, j, i%j, mask);
				}
				
				testBinOp(AND, vi, vj, i, j, i & j, mask);
				testBinOp(OR, vi, vj, i, j, i | j, mask);
				testBinOp(XOR, vi, vj, i, j, i ^ j, mask);
				
				final int shrmask = ~(-1 << (bw - ((j < 0 || j > bw) ? bw : j)));
				testBinOp(SHL, vi, vj, i, j, i << j, mask);				
				testBinOp(SHR, vi, vj, i, j, shrmask & (i >> j), mask);
				testBinOp(SHA, vi, vj, i, j, i >> j, mask);
			}
		}
		
	}
	
	
	@Test
	public final void testConstant2sComplementBinOps() {
		test2sComplementBinOps(constants());
	}
	
	@Test
	public final void testExtremeShifts() {
		final int bw = 32;
		solver.options().setBitwidth(bw);
		final IntRange range = solver.options().integers();
		final int mask = ~(-1 << bw);
		final IntConstant min = IntConstant.constant(range.min());
		final IntConstant max = IntConstant.constant(range.max());
		testBinOp(SHL, min, min, min.value(), min.value(), 0, mask);
		testBinOp(SHL, min, max, min.value(), max.value(), 0, mask);
		testBinOp(SHL, max, min, max.value(), min.value(), 0, mask);
		testBinOp(SHL, max, max, max.value(), max.value(), 0, mask);
		testBinOp(SHA, min, min, min.value(), min.value(), -1, mask);
		testBinOp(SHA, min, max, min.value(), max.value(), -1, mask);
		testBinOp(SHA, max, min, max.value(), min.value(), 0, mask);
		testBinOp(SHA, max, max, max.value(), max.value(), 0, mask);
		testBinOp(SHR, min, min, min.value(), min.value(), 0, mask);
		testBinOp(SHR, min, max, min.value(), max.value(), 0, mask);
		testBinOp(SHR, max, min, max.value(), min.value(), 0, mask);
		testBinOp(SHR, max, max, max.value(), max.value(), 0, mask);
	}
	
	@Test
	public final void testNonConstant2sComplementBinOps() {
		solver.options().setBitwidth(3);
		test2sComplementBinOps(nonConstants());
	}
	
	private final void testUnOp(IntOperator op, IntExpression ei, int i, int result, int mask) {
		final IntExpression e = ei.apply(op);
		final Formula f = ei.eq(constant(i)).and(e.eq(constant(result)));
		final Solution s = solve(f);

		assertNotNull(s.instance());
		final Evaluator eval = new Evaluator(s.instance(), solver.options());
		assertEquals(result & mask, eval.evaluate(e) & mask);
		
	}
	
	private final void test2sComplementUnOps(IntExpression[] vals) {
		final Options options = solver.options();
		
		final IntRange range = options.integers();
		final int min = range.min(), max = range.max();
		final int mask = ~(-1 << options.bitwidth());
		
		for(int i = min; i <= max; i++) {
			IntExpression vi = vals[i-min];
			testUnOp(IntOperator.NEG, vi, i, -i, mask);
			testUnOp(IntOperator.NOT, vi, i, ~i, mask);
			testUnOp(IntOperator.ABS, vi, i, Math.abs(i), mask);
			testUnOp(IntOperator.SGN, vi, i, i < 0 ? -1 : i > 0 ? 1 : 0, mask);
		}		
	}
	
	@Test
	public final void testConstant2sComplementUnOps() {
		test2sComplementUnOps(constants());
	}
	
	@Test
	public final void testNonConstant2sComplementUnOps() {
		solver.options().setBitwidth(3);
		test2sComplementUnOps(nonConstants());
	}
	
	private final void testCompOp(IntCompOperator op, IntExpression ei, IntExpression ej, int i, int j, boolean result) {
		final Formula e = ei.compare(op, ej);
		final Formula f = ei.eq(constant(i)).and(ej.eq(constant(j))).and(result ? e : e.not());
		final Solution s = solve(f);
		assertNotNull(s.instance());
		final Evaluator eval = new Evaluator(s.instance(), solver.options());
		assertFalse(result ^ eval.evaluate(e));
		
	}
	
	/**
	 * Tests all comparison ops for this.solver.options and range of vals.
	 * @requires this.solver.options.intEncoding = binary 
	 * @requires vals contains int expressions that represent all 
	 * integers allowed by this.solver.options, in proper sequence
	 */
	private final void testComparisonOps(IntExpression[] vals) {
		final Options options = solver.options();
		
		final IntRange range = options.integers();
		final int min = range.min(), max = range.max();
		
		for(int i = min; i <= max; i++) {
				IntExpression vi = vals[i-min];
			
				
			for(int j = min; j <= max; j++) {
				
				IntExpression vj = vals[j-min];
			
				testCompOp(EQ, vi, vj, i, j, i==j);
				testCompOp(LT, vi, vj, i, j, i<j);
				testCompOp(LTE, vi, vj, i, j, i<=j);
				testCompOp(GT, vi, vj, i, j, i>j);
				testCompOp(GTE, vi, vj, i, j, i>=j);
				
			}
		}
		
	}
	
	@Test
	public final void testConstant2sComplementComparisonOps() {
		testComparisonOps(constants());
	}
	
	@Test
	public final void testNonConstant2sComplementComparisonOps() {
		solver.options().setBitwidth(3);
		testComparisonOps(nonConstants());
	}

	
	@Test
	public void testSum() {
		solver.options().setBitwidth(6);
		TupleSet r1b = factory.setOf("1", "5", "9");
		bounds.bound(r1, r1b, r1b);
		
		
		Formula f = r1.sum().eq(IntConstant.constant(0));
		Solution s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(5, factory.setOf(factory.tuple("5")));
		
		f = r1.sum().eq(IntConstant.constant(5));
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(2, factory.setOf(factory.tuple("2")));
		
		f = r1.sum().eq(IntConstant.constant(5));
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(1, factory.setOf(factory.tuple("9")));
		f = r1.sum().eq(IntConstant.constant(6));
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(-8, factory.setOf(factory.tuple("1")));
		f = r1.sum().eq(IntConstant.constant(-2));
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.bound(r1, r1b);
		f = r1.sum().eq(IntConstant.constant(-2));
		s = solve(f);
		assertNotNull(s.instance());
		assertEquals(s.instance().tuples(r1), r1b);
	}
	
	@Test
	public void testIntCast() {
		solver.options().setBitwidth(6);
		TupleSet r1b = factory.setOf("1", "5", "9");
		bounds.bound(r1, r1b, r1b);
		
		Formula f = r1.sum().toExpression().eq(Expression.NONE);
		Solution s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(5, factory.setOf(factory.tuple("5")));
		f = r1.sum().toExpression().eq(IntConstant.constant(5).toExpression());
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(1, factory.setOf(factory.tuple("1")));
		bounds.boundExactly(6, factory.setOf(factory.tuple("6")));
		
		f = r1.sum().toExpression().eq(IntConstant.constant(6).toExpression());
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.bound(r1, r1b);
		f = r1.sum().toExpression().eq(IntConstant.constant(6).toExpression());
		s = solve(f);
		assertNotNull(s.instance());
		
		bounds.boundExactly(6, factory.setOf(factory.tuple("1")));
		f = r1.sum().toExpression().eq(IntConstant.constant(6).toExpression());
		s = solve(f);
		assertNull(s.instance());
		
	}
	
	@Test
	public void testBitsetCast() {
		final int width = 4, msb = width - 1;
		solver.options().setBitwidth(width);
		final List<Integer> atoms = new ArrayList<Integer>(width);
		for(int i = 0; i < msb; i++) { 
			atoms.add(Integer.valueOf(1<<i));
		}
		atoms.add(Integer.valueOf(-1<<msb));
		final Bounds b = new Bounds(new Universe(atoms));
		final TupleFactory f = b.universe().factory();
		for(Integer i : atoms) { b.boundExactly(i, f.setOf(i)); }
		b.bound(r1, f.allOf(1));
		
		for(int i = -1<<msb, max = 1<<msb; i < max; i++) { 
			Formula test = r1.sum().toBitset().eq(IntConstant.constant(i).toBitset());
			Solution sol = solver.solve(test, b);
			Instance inst = sol.instance();
			assertNotNull(inst);
			Evaluator eval = new Evaluator(inst, solver.options());
			assertEquals(i, eval.evaluate(r1.sum()));
		}
		
	}
	
	private void testIfIntExpr(Options.IntEncoding encoding) {
		solver.options().setIntEncoding(encoding);
		bounds.bound(r1, factory.setOf("15"), factory.setOf("15"));
		Formula f = (r1.some().thenElse(r1.count(), IntConstant.constant(5))).eq(IntConstant.constant(1));
		Solution s = solve(f);
		assertNotNull(s.instance());
		assertEquals(Ints.singleton(15), s.instance().tuples(r1).indexView());
		
		f = (r1.some().thenElse(r1.sum(), IntConstant.constant(5))).eq(IntConstant.constant(1));
		s = solve(f);
		assertNull(s.instance());
		
		bounds.bound(r1, factory.setOf("3"), factory.allOf(1));
		bounds.boundExactly(3, factory.setOf("3"));
		bounds.boundExactly(1, factory.setOf("1"));
		f = ((r1.count().eq(IntConstant.constant(2))).thenElse(r1.sum(), IntConstant.constant(5))).eq(IntConstant.constant(4));
		s = solve(f);
		assertNotNull(s.instance());
		assertTrue(s.instance().tuples(r1).indexView().contains(1));
		assertTrue(s.instance().tuples(r1).indexView().contains(3));
		assertEquals(2, s.instance().tuples(r1).size());
		
		f = Formula.TRUE.thenElse(IntConstant.constant(2), IntConstant.constant(3)).eq(IntConstant.constant(4));
		s = solve(f);
		assertEquals(Solution.Outcome.TRIVIALLY_UNSATISFIABLE, s.outcome());
		
		f = Formula.FALSE.thenElse(IntConstant.constant(2), IntConstant.constant(3)).eq(IntConstant.constant(3));
		s = solve(f);
		assertEquals(Solution.Outcome.TRIVIALLY_SATISFIABLE, s.outcome());
	}
	
	@Test
	public void testIfIntExpr() {
		solver.options().setBitwidth(8);
		testIfIntExpr(TWOSCOMPLEMENT);
	}
	
	private void testIntSum(Options.IntEncoding encoding) {
		solver.options().setIntEncoding(encoding);
		final Variable x = Variable.unary("x");
		bounds.bound(r1, factory.setOf("13","14","15"), factory.setOf("13","14","15"));
		Formula f = IntConstant.constant(3).eq(IntConstant.constant(1).sum(x.oneOf(r1)));
		Solution s = solve(f);
		
		assertNotNull(s.instance());
		bounds.bound(r1, factory.noneOf(1), factory.setOf("1","3","5"));
		bounds.boundExactly(1, factory.setOf("1"));
		bounds.boundExactly(3, factory.setOf("3"));
		bounds.boundExactly(5, factory.setOf("5"));
		
		f = IntConstant.constant(9).eq(x.sum().sum(x.oneOf(r1)));
		s = solve(f);
		assertNotNull(s.instance());
		assertEquals(s.instance().tuples(r1), factory.setOf("1","3","5"));
	}
	
	@Test
	public void testIntSum() {
		solver.options().setBitwidth(8);
		testIntSum(TWOSCOMPLEMENT);
	}
	
}
