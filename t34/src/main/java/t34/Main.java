package t34;

import net.sf.cglib.asm.ClassWriter;
import net.sf.cglib.asm.MethodVisitor;
import net.sf.cglib.asm.Opcodes;
import net.sf.cglib.core.ReflectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Main {

  public static abstract class Atom {
    public abstract Atom fn(Atom arg);
    public abstract int toInt();
  }

  public static abstract class Fn extends Atom {
    public int toInt() { throw new UnsupportedOperationException("Treating Fn as Int"); }
  }

  // initial environment
  public static class Env {
    private static final Inc INC = new Inc();
    @SuppressWarnings("unused") public Atom get_inc() { return INC; }
  }

  public static void main(String[] args) throws IOException {
    System.out.println(";; Simple Lambda Calc Interpreter");
    try (final BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
      startRepl(r);
    }
    System.out.println(";; Goodbye!");
  }

  private static void startRepl(BufferedReader r) throws IOException {
    final Parser parser = new Parser();
    final AstNodeReader reader = new AstNodeReader(parser);
    final EnvHolder envHolder = new EnvHolder();
    final Evaluator evaluator = new Evaluator(envHolder);

    for (;;) {
      System.out.print("> ");
      final String line = r.readLine();
      if ("(quit)".equals(line)) { return; }
      parser.init(line.toCharArray(), 0, line.length());
      try {
        System.out.println(evaluator.eval(reader.read(envHolder.scope)));
      } catch (RuntimeException e) {
        System.err.println(";; Error");
        e.printStackTrace(System.err);
      }
    }
  }
}

//
// Builtins
//

abstract class PrimitiveAtom extends Main.Atom {
  public Main.Atom fn(Main.Atom arg) { throw new UnsupportedOperationException("Treating " + getClass() + " as Fn"); }
  public int toInt() { throw new UnsupportedOperationException("Treating " + getClass() + " as Int"); }
}

final class Int extends PrimitiveAtom {
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

  public int toInt() { return value; }
  public String toString() { return Integer.toString(value); }
}

final class Inc extends Main.Fn {
  public Main.Atom fn(Main.Atom arg) { return Int.valueOf(arg.toInt() + 1); }
  public String toString() { return "<lambda#inc>"; }
}

abstract class TextAtom extends PrimitiveAtom {
  private final String text;
  public TextAtom(String text) { this.text = text; }
  public String toString() { return text; }
}

final class Special extends TextAtom {
  public static final Special LAMBDA = new Special("lambda");
  public static final Special DEFINE = new Special("define");
  public static final Special OPEN_BRACE = new Special("(");
  public static final Special CLOSE_BRACE = new Special(")");

  public static final Map<String, Special> TOKEN_MAP;

  static {
    final Map<String, Special> map = new HashMap<>(10);
    map.put("lambda", LAMBDA);
    map.put("define", DEFINE);
    map.put("(", OPEN_BRACE);
    map.put(")", CLOSE_BRACE);
    TOKEN_MAP = Collections.unmodifiableMap(map);
  }

  public Special(String text) { super(text); }
}

final class Symbol extends TextAtom {
  public final Location location;

  public Symbol(String text, Location location) {
    super(text);
    this.location = location;
  }
}

final class Call extends PrimitiveAtom {
  public final PrimitiveAtom lhs;
  public final PrimitiveAtom rhs;

  public Call(PrimitiveAtom lhs, PrimitiveAtom rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public String toString() { return "<call>"; }
}

final class Lambda extends PrimitiveAtom {
  final LexicalScope scope;
  final PrimitiveAtom body;
  final Main.Atom fnAtom;

  public Lambda(LexicalScope scope, PrimitiveAtom body, Main.Atom fnAtom) {
    this.scope = scope;
    this.body = body;
    this.fnAtom = fnAtom;
  }

  public String toString() { return "<lambda>"; }
}

final class Define extends PrimitiveAtom {
  public final Symbol sym;
  public final Lambda value;

  public Define(Symbol sym, Lambda value) {
    this.sym = sym;
    this.value = value;
  }
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
    final String className = "GenFn" + (++index);

    cw.visit(49,
        ACC_PUBLIC + ACC_SUPER,
        className,
        null,
        "t34/Main$Fn",
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
      mv.visitMethodInsn(INVOKESPECIAL, "t34/Main$Fn", "<init>", "()V");
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

final class ParserException extends RuntimeException {
  public ParserException(String message) { super(message); }
}

final class Parser {
  char[] buffer;
  int start;
  int end;

  public Parser init(char[] buffer, int start, int end) {
    this.buffer = buffer;
    this.start = start;
    this.end = end;
    return this;
  }

  public Parser() {
    init(null, 0, 0);
  }

  public PrimitiveAtom next(LexicalScope scope) {
    // skip whitespace
    for (; start < end; ++start) {
      char ch = buffer[start];
      if (ch > ' ') {
        break;
      }
    }

    boolean isInt = false;
    int tokenStart = start;
    for (; start < end; ++start) {
      char ch = buffer[start];
      if (tokenStart == start) {
        switch (ch) { // special character?
          case '(':
            ++start;
            return Special.OPEN_BRACE;
          case ')':
            ++start;
            return Special.CLOSE_BRACE;
        }

        if (ch >= '0' && ch <= '9') {
          isInt = true;
          continue;
        }
      }

      if (isInt) {
        if (ch >= '0' && ch <= '9') { continue; }
        break;
      }

      if (ch < 'a' || ch > 'z') { break; }
    }

    if (tokenStart == start) { throw new ParserException("unexpected end of input or illegal character"); }
    return toToken(isInt, new String(buffer, tokenStart, start - tokenStart), scope);
  }

  public void expect(Special token, LexicalScope scope) {
    if (next(scope) != token) { throw new ParserException("token expected: " + token.toString()); }
  }

  private PrimitiveAtom toToken(boolean isInt, String val, LexicalScope scope) {
    if (isInt) { return Int.valueOf(Integer.parseInt(val)); }
    final Special special = Special.TOKEN_MAP.get(val);
    if (special != null) { return special; }

    final Location location = scope.lookup(val);
    location.mark();
    return new Symbol(val, location);
  }
}

final class AstNodeReader {
  private final Parser parser;
  public AstNodeReader(Parser parser) { this.parser = parser; }

  public PrimitiveAtom read(LexicalScope scope) {
    return readToken(scope, parser.next(scope));
  }

  private PrimitiveAtom readToken(LexicalScope scope, PrimitiveAtom token) {
    // is it a symbol or int?
    if (token instanceof Symbol || token instanceof Int) {
      return token;
    }

    // not an open brace? - error
    if (token != Special.OPEN_BRACE) {
      throw new RuntimeException("open brace expected");
    }

    token = parser.next(scope);
    if (token == Special.LAMBDA) { return readLambdaDefinition(scope); }
    if (token == Special.DEFINE) { return readDefine(scope); }

    final PrimitiveAtom lhs = readToken(scope, token);
    final PrimitiveAtom rhs = readToken(scope, parser.next(scope));
    parser.expect(Special.CLOSE_BRACE, scope);
    return new Call(lhs, rhs);
  }

  private Define readDefine(LexicalScope scope) {
    final Symbol sym = (Symbol) parser.next(scope); // TODO: check and report if not a symbol
    parser.expect(Special.OPEN_BRACE, scope);
    parser.expect(Special.LAMBDA, scope);
    return new Define(sym, readLambdaDefinition(scope));
  }

  private Lambda readLambdaDefinition(LexicalScope parentScope) {
    parser.expect(Special.OPEN_BRACE, parentScope); // start arg list
    PrimitiveAtom token = parser.next(parentScope);
    if (!(token instanceof Symbol)) { throw new ParserException("arg is not a symbol"); }
    final String varName = token.toString();
    parser.expect(Special.CLOSE_BRACE, parentScope); // end arg list
    final LexicalScope scope = new LambdaLexicalScope(parentScope, varName);
    final PrimitiveAtom body = readToken(scope, parser.next(scope));
    parser.expect(Special.CLOSE_BRACE, parentScope); // end lambda
    return new Lambda(scope, body, Int.valueOf(0));
  }
}

final class EnvHolder {
  public Main.Env env = new Main.Env();
  public final GlobalLexicalScope scope = new GlobalLexicalScope();
}

final class Evaluator implements Opcodes {
  private final EnvHolder envHolder;

  public Evaluator(EnvHolder envHolder) {
    this.envHolder = envHolder;
  }

  Main.Atom eval(Main.Atom node) {
    if (node instanceof Symbol) {
      // get from env
      try {
        final Method m = envHolder.env.getClass().getMethod("get_" + node.toString());
        return (Main.Atom) m.invoke(envHolder.env);
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    if (node instanceof Call) {
      final Call call = ((Call) node);
      return eval(call.lhs).fn(eval(call.rhs));
    }

    if (node instanceof PrimitiveAtom || node instanceof Main.Fn) { return node; }

    throw new UnsupportedOperationException("Can't eval " + node);
  }
}
