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
  }

  @Test
  public void shouldEvalInt() {
    // Given:
    final int value = 10;

    // When:
    final Main.Atom a = evaluator.eval(Int.valueOf(value));

    // Then:
    assertTrue(a instanceof Int);
    assertEquals(value, a.toInt());
  }

  @Test
  public void shouldEvalInc() {
    // When:
    final Main.Atom a = evaluator.eval(lookupGlobal("inc"));

    // Then:
    assertTrue(a instanceof Inc);
  }

  @Test
  public void shouldEvalCall() {
    // Given:
    final PrimitiveAtom call = parse("(inc 0)");

    // When:
    final Main.Atom a = evaluator.eval(call);

    // Then:
    assertEquals(1, a.toInt());
  }

  @Test
  public void shouldEvalNestedCall() {
    // Given:
    final PrimitiveAtom call = parse("(inc (inc (inc 0)))");

    // When:
    final Main.Atom a = evaluator.eval(call);

    // Then:
    assertEquals(3, a.toInt());
  }

  @Test
  public void shouldEvalSimpleLambdaExpr() {
    // Given:
    final PrimitiveAtom lambda = parse("(lambda (a) a)");

    // When:
    final Main.Atom a = evaluator.eval(lambda);

    // Then:
    assertEquals(Int.valueOf(1), a.fn(Int.valueOf(1)));
  }

  private Symbol lookupGlobal(String val) {
    return new Symbol(val, evaluator.scope.lookup(val));
  }

  private PrimitiveAtom parse(String input) {
    return AstNodeReaderTest.createReader(input).read(evaluator.scope);
  }
}
