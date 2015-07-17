package t34;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests evaluator
 */
public class EvaluatorTest {
  private Evaluator evaluator;

  @Before
  public void init() {
    evaluator = new Evaluator();
    Main.ENV = new Main.Env(); // reset environment
  }

  @Test
  public void shouldEvalInt() throws Exception {
    // Given:
    final int value = 10;

    // When:
    final Main.Atom a = evaluator.eval(Int.valueOf(value));

    // Then:
    assertTrue(a instanceof Int);
    assertEquals(value, a.toInt());
  }

  @Test
  public void shouldEvalInc() throws Exception {
    // When:
    final Main.Atom a = evaluator.eval(lookupGlobal("inc"));

    // Then:
    assertTrue(a instanceof Inc);
  }

  @Test
  public void shouldEvalCall() throws Exception {
    // Given:
    final PrimitiveAtom call = parse("(inc 0)");

    // When:
    final Main.Atom a = evaluator.eval(call);

    // Then:
    assertEquals(1, a.toInt());
  }

  @Test
  public void shouldEvalNestedCall() throws Exception {
    // Given:
    final PrimitiveAtom call = parse("(inc (inc (inc 0)))");

    // When:
    final Main.Atom a = evaluator.eval(call);

    // Then:
    assertEquals(3, a.toInt());
  }

  @Test
  public void shouldEvalMultipleFns() throws Exception {
    // Given:
    final PrimitiveAtom call = parse("(dec (inc 0))");

    // When:
    final Main.Atom a = evaluator.eval(call);

    // Then:
    assertEquals(0, a.toInt());
  }

  @Test
  public void shouldEvalSimpleLambdaExpr() throws Exception {
    // Given:
    final PrimitiveAtom lambda = parse("(lambda (a) a)");

    // When:
    final Main.Atom a = evaluator.eval(lambda);

    // Then:
    assertEquals(Int.valueOf(1), a.fn(Int.valueOf(1)));
  }

  @Test
  public void shouldEvalPrimitiveDefine() throws Exception {
    // Given:
    evaluator.eval(parse("(define zero 0)"));
    evaluator.eval(parse("(define one (inc zero))"));

    // When:
    final Main.Atom a0 = evaluator.eval(parse("zero"));
    final Main.Atom a1 = evaluator.eval(parse("one"));

    // Then:
    assertEquals(Int.valueOf(0), a0);
    assertEquals(Int.valueOf(1), a1);
  }

  @Test
  public void shouldEvalDefine() throws Exception {
    // Given:
    evaluator.eval(parse("(define id (lambda (a) a))"));

    // When:
    final Main.Atom a = evaluator.eval(parse("(id 1)"));

    // Then:
    assertEquals(Int.valueOf(1), a);
  }

  @Test
  public void shouldEvalNestedLambda() throws Exception {
    // Given/When:
    final Main.Atom a = evaluator.eval(parse("((lambda (a) (inc a)) 0)"));

    // Then:
    assertEquals(Int.valueOf(1), a);
  }

  @Test
  public void shouldEvalLambdaCalc() throws Exception {
    // Given:
    evaluator.eval(parse("(define zero (lambda (s) (lambda (z) z)))"));
    evaluator.eval(parse("(define succ (lambda (n) (lambda (s) (lambda (z) (s ((n s) z))))))"));

    // When:
    final Main.Atom a = evaluator.eval(parse("(((succ zero) inc) 0)"));

    // Then:
    assertEquals(Int.valueOf(1), a);
  }

  private Symbol lookupGlobal(String val) {
    return new Symbol(val, evaluator.scope.lookup(val));
  }

  private PrimitiveAtom parse(String input) {
    return AstNodeReaderTest.createReader(input).read(evaluator.scope);
  }
}
