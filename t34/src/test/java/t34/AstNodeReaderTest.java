package t34;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link AstNodeReader}.
 */
public class AstNodeReaderTest {
  private final GlobalLexicalScope globalScope = new GlobalLexicalScope();

  @Test
  public void shouldReadGlobalSymbol() {
    // Given:
    final AstNodeReader reader = createReader("a");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    assertTrue(node instanceof Symbol);
    final Symbol sym = (Symbol) node;
    assertEquals(SimpleLocation.GLOBAL, sym.location);
    assertEquals("a", sym.toString());
  }

  @Test
  public void shouldReadNumber() {
    // Given:
    final AstNodeReader reader = createReader("0");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    assertTrue(node instanceof Int);
    assertEquals(0, node.toInt());
  }

  @Test
  public void shouldReadLambdas() {
    // Given:
    final AstNodeReader reader = createReader("(lambda (a) a)");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(lambda (a:CLOSURE[0]) a:VAR)", nodeStr);
  }

  @Test
  public void shouldReadNestedLambdas() {
    // Given:
    final AstNodeReader reader = createReader("(lambda (a0) (lambda (b) (b a0)))");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(lambda (a0:CLOSURE[0]) (lambda (b:CLOSURE[1]) (b:VAR a0:CLOSURE[0])))", nodeStr);
  }

  @Test
  public void shouldReadNestedLambdas2() {
    // Given:
    final AstNodeReader reader = createReader("(lambda (a) (lambda (b) (b (lambda (c) (c (b a))))))");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(lambda (a:CLOSURE[0]) " +
            "(lambda (b:CLOSURE[1]) (b:VAR (lambda (c:CLOSURE[2]) (c:VAR (b:CLOSURE[1] a:CLOSURE[0]))))))",
        nodeStr);
  }

  @Test
  public void shouldReadCall() {
    // Given:
    final AstNodeReader reader = createReader("(a b)");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(a:GLOBAL b:GLOBAL)", nodeStr);
  }

  @Test
  public void shouldReadNestedCall() {
    // Given:
    final AstNodeReader reader = createReader("(((a b) c) d)");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(((a:GLOBAL b:GLOBAL) c:GLOBAL) d:GLOBAL)", nodeStr);
  }

  @Test
  public void shouldReadSimpleLambda() {
    // Given:
    final AstNodeReader reader = createReader("(define one 1)");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    assertTrue(node instanceof Define);
  }

  @Test
  public void shouldReadLambdaDefine() {
    // Given:
    final AstNodeReader reader = createReader("(define id (lambda (x) x))");

    // When:
    final PrimitiveAtom node = reader.read(globalScope);

    // Then:
    assertTrue(node instanceof Define);
  }

  //
  // Private
  //

  public static AstNodeReader createReader(String input) {
    return new AstNodeReader(new Parser().init(input.toCharArray(), 0, input.length()));
  }

  private static String toString(PrimitiveAtom node) {
    final StringBuilder builder = new StringBuilder(100);
    append(builder, node);
    return builder.toString();
  }

  private static void append(StringBuilder builder, Lambda lambda) {
    builder.append('(').append("lambda").append(' ')
        .append('(').append(lambda.scope.getLocalVarName());
    append(builder, lambda.scope.getLocalClosureLocation());
    builder.append(')').append(' ');

    append(builder, lambda.body);

    builder.append(')');
  }

  private static void append(StringBuilder builder, PrimitiveAtom node) {
    if (node instanceof Symbol) {
      final Symbol sym = (Symbol) node;
      builder.append(sym.toString());
      append(builder, sym.location);
      return;
    }

    if (node instanceof Lambda) {
      append(builder, (Lambda) node);
      return;
    }

    if (node instanceof Call) {
      final Call call = (Call) node;
      builder.append('(');
      append(builder, call.lhs);
      builder.append(' ');
      append(builder, call.rhs);
      builder.append(')');
      return;
    }

    throw new UnsupportedOperationException("Unsupported node: " + node);
  }

  private static void append(StringBuilder builder, Location location) {
    builder.append(':');

    if (location == SimpleLocation.VAR) {
      builder.append("VAR");
      return;
    }

    if (location == SimpleLocation.GLOBAL) {
      builder.append("GLOBAL");
      return;
    }

    final ClosureLocation closureLocation = (ClosureLocation) location;
    builder.append("CLOSURE").append('[').append(closureLocation.getParameterIndex()).append(']');
  }
}
