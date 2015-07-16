package t34;

import net.sf.cglib.asm.ClassWriter;
import net.sf.cglib.asm.MethodVisitor;
import net.sf.cglib.asm.Opcodes;
import net.sf.cglib.core.ReflectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private static final Atom INC = new Inc();
    private static final Atom DEC = new Dec();
    @SuppressWarnings("unused") public Atom inc() { return INC; }
    @SuppressWarnings("unused") public Atom dec() { return DEC; }
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
    final Evaluator evaluator = new Evaluator();

    for (;;) {
      System.out.print("> ");
      final String line = r.readLine();
      if ("(quit)".equals(line)) { return; }
      parser.init(line.toCharArray(), 0, line.length());
      try {
        final long start = System.nanoTime();
        final Atom result = evaluator.eval(reader.read(evaluator.scope));
        final long delta = System.nanoTime() - start;
        System.out.println(result + "\n;; time=" + delta + " nanoseconds (~" +
            TimeUnit.NANOSECONDS.toMillis(delta) + " msec, ~" + TimeUnit.NANOSECONDS.toSeconds(delta) + " sec)");
      } catch (Exception e) {
        System.err.println(";; Error");
        e.printStackTrace(System.err);
      }
    }
  }
}

abstract class PrimitiveAtom extends Main.Atom {
  public Main.Atom fn(Main.Atom arg) { throw new UnsupportedOperationException("Treating " + getClass() + " as Fn"); }
  public int toInt() { throw new UnsupportedOperationException("Treating " + getClass() + " as Int"); }
}

final class Int extends PrimitiveAtom {
  private final static int CACHE_SIZE = 256;
  private final static Int[] CACHE = new Int[CACHE_SIZE];
  static { for (int i = 0; i < CACHE.length; ++i) { CACHE[i] = new Int(i); } }

  private final int value;
  private Int(int value) { this.value = value; }

  public static Int valueOf(int value) {
    return (value >= 0 && value < CACHE_SIZE) ? CACHE[value] : new Int(value);
  }

  public int toInt() { return value; }
  public boolean equals(Object o) { return this == o || o instanceof Int && value == ((Int) o).value; }
  public int hashCode() { return value; }
  public String toString() { return Integer.toString(value); }
}

final class Inc extends Main.Fn {
  public Main.Atom fn(Main.Atom arg) { return Int.valueOf(arg.toInt() + 1); }
  public String toString() { return "<lambda#inc>"; }
}

final class Dec extends Main.Fn {
  public Main.Atom fn(Main.Atom arg) { return Int.valueOf(arg.toInt() - 1); }
  public String toString() { return "<lambda#dec>"; }
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
  public final LexicalScope scope;
  public final PrimitiveAtom body;

  public Lambda(LexicalScope scope, PrimitiveAtom body) {
    this.scope = scope;
    this.body = body;
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
  private char[] buffer;
  private int start;
  private int end;

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
    for (; start < end; ++start) { // skip whitespace
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
    if (isInt) { return Int.valueOf(Integer.parseInt(val)); } // number?

    if (Special.DEFINE.toString().equals(val)) { return Special.DEFINE; } // special?
    if (Special.LAMBDA.toString().equals(val)) { return Special.LAMBDA; }

    final Location location = scope.lookup(val);
    location.mark();
    return new Symbol(val, location); // default - treat as a symbol
  }
}

final class AstNodeReader {
  private final Parser parser;
  public AstNodeReader(Parser parser) { this.parser = parser; }

  public PrimitiveAtom read(LexicalScope scope) {
    return readToken(scope, parser.next(scope));
  }

  private PrimitiveAtom readToken(LexicalScope scope, PrimitiveAtom token) {
    if (token instanceof Symbol || token instanceof Int) { return token; }
    if (token != Special.OPEN_BRACE) { throw new RuntimeException("open brace expected"); }
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
    return new Lambda(scope, body);
  }
}

final class Evaluator implements Opcodes {
  private Main.Env env = new Main.Env();
  private static int FN_INDEX = 0;
  private static int ENV_INDEX = 0;
  private String prevEnvClassName = "t34/Main$Env";
  private final ClassLoader loader = getClass().getClassLoader();

  public final GlobalLexicalScope scope = new GlobalLexicalScope();

  public Main.Atom eval(Main.Atom node) throws Exception {
    if (node instanceof Symbol) { return (Main.Atom) env.getClass().getMethod(node.toString()).invoke(env); }

    if (node instanceof Call) {
      final Call call = ((Call) node);
      return eval(call.lhs).fn(eval(call.rhs));
    }

    if (node instanceof Define) { return evalDefine((Define) node); }
    if (node instanceof Lambda) { return evalLambda((Lambda) node); }
    if (node instanceof PrimitiveAtom || node instanceof Main.Fn) { return node; }
    throw new UnsupportedOperationException("Can't eval " + node);
  }

  private Main.Fn evalLambda(Lambda lambda) throws Exception { return (Main.Fn) genLambdaClass(lambda).newInstance(); }

  private Int evalDefine(Define define) throws Exception {
    final Main.Fn lambdaFn = evalLambda(define.value); // generate function using second define argument
    final Class<?> newEnvClass = genNewEnv(define.sym.toString()); // generate new environment class
    newEnvClass.getDeclaredField("SYM").set(null, lambdaFn); // update associated symbol value
    env = (Main.Env) newEnvClass.newInstance(); // create new environment
    prevEnvClassName = newEnvClass.getName(); // this update should be the last one or error will break the state
    return Int.valueOf(0);
  }

  private Class<?> genNewEnv(String sym) throws Exception {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    MethodVisitor mv;
    final String parentClassSgn = prevEnvClassName;
    final String className = "Env" + (++ENV_INDEX);

    cw.visit(49, ACC_PUBLIC + ACC_SUPER, className, null, parentClassSgn, null);
    cw.visitSource(className + ".java", null);
    cw.visitField(ACC_PUBLIC + ACC_STATIC, "SYM", "Lt34/Main$Atom;", null, null).visitEnd();

    // ctor
    mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, parentClassSgn, "<init>", "()V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // fn - sym
    mv = cw.visitMethod(ACC_PUBLIC, sym, "()Lt34/Main$Atom;", null, null);
    mv.visitFieldInsn(GETSTATIC, className, "SYM", "Lt34/Main$Atom;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    cw.visitEnd(); // end of class
    return (Class<?>) ReflectUtils.defineClass(className, cw.toByteArray(), loader);
  }

  private Class<?> genLambdaClass(Lambda lambda) throws Exception {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    MethodVisitor mv;
    final String className = "GenFn" + (++FN_INDEX);

    cw.visit(49, ACC_PUBLIC + ACC_SUPER, className, null, "t34/Main$Fn", null);
    cw.visitSource(className + ".java", null);

    // fields
    int numberOfFields = lambda.scope.getLocalClosureLocation().getParameterIndex();
    for (int i = 0; i < numberOfFields; ++i) {
      cw.visitField(ACC_PRIVATE, "c" + i, "Lt34/Main$Atom;", null, null).visitEnd();
    }

    // ctor
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
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // @Override fn
    mv = cw.visitMethod(ACC_PUBLIC, "fn", "(Lt34/Main$Atom;)Lt34/Main$Atom;", null, null);
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

    cw.visitEnd(); // end of class
    return (Class<?>) ReflectUtils.defineClass(className, cw.toByteArray(), loader);
  }
}
