package t34;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static t34.Main.Atom;

public class GeneratorTest {
  @Test
  public void shouldGenerateFn() throws Exception {
    final AtomGenerator gen = new AtomGenerator(Main.class.getClassLoader());
    Atom val;

    final Atom ga0 = (Atom) gen.generate(0).newInstance();
    val = ga0.fn(Int.valueOf(0));
    assertEquals("0", val.toString());

    final Atom ga1 = (Atom) gen.generate(1).getConstructor(Atom.class).newInstance(new Inc());
    val = ga1.fn(Int.valueOf(0));
    assertEquals("1", val.toString());

    final Atom ga2 = (Atom) gen.generate(2)
        .getConstructor(Atom.class, Atom.class)
        .newInstance(new Inc(), new Inc());
    val = ga2.fn(Int.valueOf(0));
    assertEquals("2", val.toString());
  }
}
