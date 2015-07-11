package t34;

import net.sf.cglib.asm.ClassWriter;
import net.sf.cglib.asm.MethodVisitor;
import net.sf.cglib.asm.Opcodes;
import net.sf.cglib.core.ReflectUtils;

import java.util.HashMap;
import java.util.Map;

public final class Main {

  public static abstract class Atom {
    public Atom fn(Atom arg) { throw new UnsupportedOperationException(); }
    public int toInt() { throw new UnsupportedOperationException(); }
  }

  public static void main(String[] args) throws Exception {
    final AtomGenerator gen = new AtomGenerator(Main.class.getClassLoader());
    Atom val;

    final Atom ga0 = (Atom) gen.generate(0).newInstance();
    val = ga0.fn(Int.valueOf(0));
    System.out.println("[0] val=" + val);

    final Atom ga1 = (Atom) gen.generate(1).getConstructor(Atom.class).newInstance(new Inc());
    val = ga1.fn(Int.valueOf(0));
    System.out.println("[1] val=" + val);

    final Atom ga2 = (Atom) gen.generate(2)
        .getConstructor(Atom.class, Atom.class)
        .newInstance(new Inc(), new Inc());
    val = ga2.fn(Int.valueOf(0));
    System.out.println("[2] val=" + val);
  }
}

//
// Builtins
//

final class Int extends Main.Atom {
  private final static int CACHE_SIZE = 256;
  private final static Int[] CACHE = new Int[CACHE_SIZE];
  static {
    for (int i = 0; i < CACHE.length; ++i) {
      CACHE[i] = new Int(i);
    }
  }

  private final int value;

  private Int(int value) { this.value = value; }

  public static Int valueOf(int value) {
    if (value >= 0 && value < CACHE_SIZE) {
      return CACHE[value];
    }
    return new Int(value);
  }

  @Override
  public int toInt() { return value; }

  @Override
  public String toString() { return Integer.toString(value); }
}

final class Inc extends Main.Atom {
  @Override
  public Main.Atom fn(Main.Atom arg) {
    return Int.valueOf(arg.toInt() + 1);
  }

  @Override
  public String toString() { return "inc"; }
}

//
// Code Generator
//

final class AtomGenerator implements Opcodes {
  private final ClassLoader loader;
  private int index = 0;

  public AtomGenerator(ClassLoader loader) {
    this.loader = loader;
  }

  public Class<?> generate(int numberOfFields) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    MethodVisitor mv;
    final String className = "GenAtom" + (++index);

    cw.visit(49,
        ACC_PUBLIC + ACC_SUPER,
        className,
        null,
        "t34/Main$Atom",
        null);

    cw.visitSource(className + ".java", null);

    // fields
    for (int i = 0; i < numberOfFields; ++i) {
      cw.visitField(ACC_PRIVATE, "c" + i, "Lt34/Main$Atom;", null, null).visitEnd();
    }

    // ctor
    {
      final StringBuilder signature = new StringBuilder(18 * numberOfFields + 5);
      signature.append('(');
      for (int i = 0; i < numberOfFields; ++i) { signature.append("Lt34/Main$Atom;"); }
      signature.append(")V");
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", signature.toString(), null, null);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "t34/Main$Atom", "<init>", "()V");
      for (int i = 0; i < numberOfFields; ++i) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, i + 1);
        mv.visitFieldInsn(PUTFIELD, className, "c" + i, "Lt34/Main$Atom;");
      }
      mv.visitInsn(RETURN);

      mv.visitMaxs(0, 0); // 1 + numberOfFields
      mv.visitEnd();
    }

    // @Override fn
    {
      mv = cw.visitMethod(ACC_PUBLIC,
          "fn",
          "(Lt34/Main$Atom;)Lt34/Main$Atom;",
          null,
          null);

      for (int i = 0; i < numberOfFields; ++i) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "c" + i, "Lt34/Main$Atom;");
      }

      mv.visitVarInsn(ALOAD, 1);

      for (int i = 0; i < numberOfFields; ++i) {
        mv.visitMethodInsn(INVOKEVIRTUAL, "t34/Main$Atom", "fn", "(Lt34/Main$Atom;)Lt34/Main$Atom;");
      }

      mv.visitInsn(ARETURN);

      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    cw.visitEnd();

    try {
      //noinspection unchecked
      return (Class<?>) ReflectUtils.defineClass(className, cw.toByteArray(), loader);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

interface Location {
  default void mark() {}
}

final class ClosureLocation implements Location {
  private boolean used;
  private final int index;

  public ClosureLocation(int index) { this.index = index; }
  public int getParameterIndex() { return index; }
  public void mark() { this.used = true; }
  @Override
  public String toString() {
    return "closure(" + index + ":" + (used ? "used" : "unused") + ")";
  }
}

enum SimpleLocation implements Location {
  VAR,
  GLOBAL
}

interface LexicalScope {
  Location lookup(String symbol);
  boolean isGlobal();

  default Map<String, Location> getLocations() { throw new UnsupportedOperationException(); }
  default String getLocalVarName() { throw new UnsupportedOperationException(); }
  default ClosureLocation getLocalClosureLocation() { throw new UnsupportedOperationException(); }
}

final class GlobalLexicalScope implements LexicalScope {
  public boolean isGlobal() { return true; }
  public Location lookup(String symbol) { return SimpleLocation.GLOBAL; }
}

final class LambdaLexicalScope implements LexicalScope {
  private final Map<String, Location> locations;
  private final String localVarName;
  private final ClosureLocation localClosureLocation;

  public LambdaLexicalScope(LexicalScope parent, String varName) {
    localVarName = varName;
    if (parent.isGlobal()) {
      localClosureLocation = new ClosureLocation(0);
      locations = new HashMap<>();
    } else {
      locations = new HashMap<>(parent.getLocations());
      localClosureLocation = new ClosureLocation(parent.getLocalClosureLocation().getParameterIndex() + 1);
      locations.put(parent.getLocalVarName(), parent.getLocalClosureLocation()); // parent's var visible as a closure
    }

    locations.put(varName, SimpleLocation.VAR);
  }

  public boolean isGlobal() { return false; }

  public Map<String, Location> getLocations() { return locations; }

  public String getLocalVarName() { return localVarName; }

  public ClosureLocation getLocalClosureLocation() { return localClosureLocation; }

  public Location lookup(String symbol) {
    Location result = locations.get(symbol);
    if (result != null) { return result; }
    return SimpleLocation.GLOBAL; // no such symbol - assume it was (or it will be) defined in global scope
  }
}

interface Token {
  String getText();
  boolean isSymbol();
}

enum SimpleToken implements Token {
  DEFINE("define"),
  LAMBDA("lambda"),
  OPEN_BRACE("("),
  CLOSE_BRACE(")");

  final String text;
  SimpleToken(String text) { this.text = text; }
  public String getText() { return text; }
  public boolean isSymbol() { return false; }
}

final class Symbol implements Token {
  final String text;

  public Symbol(String text) { this.text = text; }
  public String getText() { return text; }
  public boolean isSymbol() { return true; }
}

abstract class AstNode {
  static final class Sym extends AstNode {
    final String text;
    final Location location;

    Sym(String text, Location location) {
      this.text = text;
      this.location = location;
    }
  }

  static final class Call extends AstNode {
    final AstNode lhs;
    final AstNode rhs;

    Call(AstNode lhs, AstNode rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  static final class Lambda extends AstNode {
    final LexicalScope scope;
    final AstNode body;
    final Main.Atom atom;

    public Lambda(LexicalScope scope, AstNode body, final Main.Atom atom) {
      this.scope = scope;
      this.body = body;
      this.atom = atom;
    }
  }
}

final class ParserException extends RuntimeException {
  public ParserException(String message) { super(message); }
}

final class AstNodeReader {
  final Parser parser;

  public AstNodeReader(Parser parser) {
    this.parser = parser;
  }
  
  AstNode read(LexicalScope scope) {
    Token token = parser.next();
    if (token == SimpleToken.OPEN_BRACE) {
      token = parser.next();
      if (token == SimpleToken.LAMBDA) {
        return readLambdaDefinition(scope);
      }
      if (token == SimpleToken.DEFINE) {
        throw new UnsupportedOperationException(); // TODO: implement
      }
      // this is a function call
    }

    return readLambdaBody(scope, token);
  }

  private AstNode.Lambda readLambdaDefinition(LexicalScope parentScope) {
    parser.expect(SimpleToken.OPEN_BRACE); // start arg list
    Token token = parser.next();
    if (!token.isSymbol()) { throw new ParserException("arg is not a symbol"); }
    final String varName = token.getText();
    parser.expect(SimpleToken.CLOSE_BRACE); // end arg list
    final LexicalScope scope = new LambdaLexicalScope(parentScope, varName);
    final AstNode body = readLambdaBody(scope, parser.next());
    parser.expect(SimpleToken.CLOSE_BRACE); // end lambda
    return new AstNode.Lambda(scope, body, Int.valueOf(0));
  }

  private AstNode readLambdaBody(LexicalScope scope, Token token) {
    // is it a symbol?
    if (token.isSymbol()) {
      final Location location = scope.lookup(token.getText());
      location.mark();
      return new AstNode.Sym(token.getText(), location);
    }

    // not an open brace? - error
    if (token != SimpleToken.OPEN_BRACE) {
      throw new RuntimeException("open brace expected");
    }

    token = parser.next();
    if (token == SimpleToken.LAMBDA) {
      return readLambdaDefinition(scope); // special handling for inner lambdas
    }

    final AstNode lhs = readLambdaBody(scope, token);
    final AstNode rhs = readLambdaBody(scope, parser.next());
    parser.expect(SimpleToken.CLOSE_BRACE);
    return new AstNode.Call(lhs, rhs);
  }
}

final class Parser {
  final char[] buffer;
  int start;
  int end;

  public Parser(char[] buffer, int start, int end) {
    this.buffer = buffer;
    this.start = start;
    this.end = end;
  }

  public Token next() {
    // skip whitespace
    for (; start < end; ++start) {
      char ch = buffer[start];
      if (ch > ' ') {
        break;
      }
    }

    int tokenStart = start;
    for (; start < end; ++start) {
      char ch = buffer[start];
      if (tokenStart == start) {
        switch (ch) { // special character?
          case '(':
            ++start;
            return SimpleToken.OPEN_BRACE;
          case ')':
            ++start;
            return SimpleToken.CLOSE_BRACE;
        }
      }

      if (ch < 'a' || ch > 'z') { break; }
    }

    if (tokenStart == start) {
      throw new ParserException("unexpected end of input or illegal character");
    }

    final String val = new String(buffer, tokenStart, start - tokenStart);
    assert val.matches("[a-z]+") : "String should contain only letters";
    for (final SimpleToken simpleToken : SimpleToken.values()) {
      if (simpleToken.getText().equals(val)) {
        return simpleToken;
      }
    }

    return new Symbol(val);
  }

  public void expect(Token token) {
    if (next() != token) { throw new ParserException("token expected: " + token.getText()); }
  }
}