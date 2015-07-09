package t34;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

  @Test
  public void shouldParseSimpleTokens() {
    assertEquals(SimpleToken.LAMBDA, createParser("lambda").next());
    assertEquals(SimpleToken.DEFINE, createParser("define").next());
    assertEquals(SimpleToken.OPEN_BRACE, createParser("(").next());
    assertEquals(SimpleToken.CLOSE_BRACE, createParser(")").next());
  }

  @Test
  public void shouldTrimHeadingWhitespace() {
    assertEquals(SimpleToken.LAMBDA, createParser(" \n  lambda ").next());
    assertEquals(SimpleToken.DEFINE, createParser(" \ndefine  ").next());
    assertEquals(SimpleToken.OPEN_BRACE, createParser(" (").next());
    assertEquals(SimpleToken.CLOSE_BRACE, createParser("  ) ").next());
  }

  @Test
  public void shouldParseSequence() {
    final Parser parser = createParser("(lambda  (a ) bbb)");
    assertEquals(SimpleToken.OPEN_BRACE, parser.next());
    assertEquals(SimpleToken.LAMBDA, parser.next());
    assertEquals(SimpleToken.OPEN_BRACE, parser.next());
    Token token = parser.next();
    assertTrue(token.isSymbol());
    assertEquals("a", token.getText());
    assertEquals(SimpleToken.CLOSE_BRACE, parser.next());
    token = parser.next();
    assertTrue(token.isSymbol());
    assertEquals("bbb", token.getText());
    assertEquals(SimpleToken.CLOSE_BRACE, parser.next());
  }

  @Test
  public void shouldReadGlobalSymbol() {
    final Parser parser = createParser("a");
    final GlobalLexicalScope globalScope = new GlobalLexicalScope();
    final AstNode node = parser.read(globalScope);
    assertTrue(node instanceof AstNode.Sym);
    final AstNode.Sym sym = (AstNode.Sym) node;
    assertEquals(SimpleLocation.GLOBAL, sym.location);
    assertEquals("a", sym.text);
  }

  //
  // Private
  //

  private Parser createParser(String input) {
    return new Parser(input.toCharArray(), 0, input.length());
  }
}
