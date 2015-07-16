package t34;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {
  private final GlobalLexicalScope scope = new GlobalLexicalScope();

  @Test
  public void shouldParseSpecials() {
    assertEquals(Special.LAMBDA, createParser("lambda").next(scope));
    assertEquals(Special.DEFINE, createParser("define").next(scope));
    assertEquals(Special.OPEN_BRACE, createParser("(").next(scope));
    assertEquals(Special.CLOSE_BRACE, createParser(")").next(scope));
  }

  @Test
  public void shouldTrimHeadingWhitespace() {
    assertEquals(Special.LAMBDA, createParser(" \n  lambda ").next(scope));
    assertEquals(Special.DEFINE, createParser(" \ndefine  ").next(scope));
    assertEquals(Special.OPEN_BRACE, createParser(" (").next(scope));
    assertEquals(Special.CLOSE_BRACE, createParser("  ) ").next(scope));
  }

  @Test
  public void shouldParseSequence() {
    final Parser parser = createParser("(lambda  (a ) bbb)");
    assertEquals(Special.OPEN_BRACE, parser.next(scope));
    assertEquals(Special.LAMBDA, parser.next(scope));
    assertEquals(Special.OPEN_BRACE, parser.next(scope));
    PrimitiveAtom token = parser.next(scope);
    assertTrue(token instanceof Symbol);
    assertEquals("a", token.toString());
    assertEquals(Special.CLOSE_BRACE, parser.next(scope));
    token = parser.next(scope);
    assertTrue(token instanceof Symbol);
    assertEquals("bbb", token.toString());
    assertEquals(Special.CLOSE_BRACE, parser.next(scope));
  }

  //
  // Private
  //

  private Parser createParser(String input) {
    return new Parser().init(input.toCharArray(), 0, input.length());
  }
}
