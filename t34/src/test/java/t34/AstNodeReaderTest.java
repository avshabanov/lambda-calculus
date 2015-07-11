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
    final AstNode node = reader.read(globalScope);

    // Then:
    assertTrue(node instanceof AstNode.Sym);
    final AstNode.Sym sym = (AstNode.Sym) node;
    assertEquals(SimpleLocation.GLOBAL, sym.location);
    assertEquals("a", sym.text);
  }

  @Test
  public void shouldReadLambdas() {
    // Given:
    final AstNodeReader reader = createReader("(lambda (a) a)");

    // When:
    final AstNode node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(lambda (a:CLOSURE[0]) a:VAR)", nodeStr);
  }

  @Test
  public void shouldReadNestedLambdas() {
    // Given:
    final AstNodeReader reader = createReader("(lambda (a) (lambda (b) (b a)))");

    // When:
    final AstNode node = reader.read(globalScope);

    // Then:
    final String nodeStr = toString(node);
    assertEquals("(lambda (a:CLOSURE[0]) (lambda (b:CLOSURE[1]) (b:VAR a:CLOSURE[0])))", nodeStr);
  }

  //
  // Private
  //

  private AstNodeReader createReader(String input) {
    return new AstNodeReader(new Parser(input.toCharArray(), 0, input.length()));
  }

  private static String toString(AstNode node) {
    final StringBuilder builder = new StringBuilder(100);
    append(builder, node);
    return builder.toString();
  }

  private static void append(StringBuilder builder, AstNode.Lambda lambda) {
    builder.append('(').append("lambda").append(' ')
        .append('(').append(lambda.scope.getLocalVarName());
    append(builder, lambda.scope.getLocalClosureLocation());
    builder.append(')').append(' ');

    append(builder, lambda.body);

    builder.append(')');
  }

  private static void append(StringBuilder builder, AstNode node) {
    if (node instanceof AstNode.Sym) {
      final AstNode.Sym sym = (AstNode.Sym) node;
      builder.append(sym.text);
      append(builder, sym.location);
      return;
    }

    if (node instanceof AstNode.Lambda) {
      append(builder, (AstNode.Lambda) node);
      return;
    }

    if (node instanceof AstNode.Call) {
      final AstNode.Call call = (AstNode.Call) node;
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
