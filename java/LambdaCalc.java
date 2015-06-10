

public class LambdaCalc {
  abstract static class Atom {
    int intValue() {
      throw new UnsupportedOperationException("function can't be casted to int");
    }

    abstract Atom fn(Atom atom);
  }


  //  (def zero (% s (% z z)))
  static final class Zero extends Atom {
    static final class Z extends Atom {
      @Override
      Atom fn(Atom z) { return z; }
    }

    static final Z lambdaZ = new Z(); /* optimization */

    @Override
    Atom fn(Atom s) { return lambdaZ; }
  }

  static final Zero ZERO = new Zero();

  //  (def succ (% n (% s (% z s ((n s) z)))))
  static final class Succ extends Atom {

    @Override
    Atom fn(final Atom n) {
      return new Atom() {
        @Override
        Atom fn(final Atom s) {
          return new Atom() {
            @Override
            Atom fn(Atom z) {
              return s.fn(n.fn(s).fn(z));
            }
          };
        }
      };
    }
  }

  static final Succ SUCC = new Succ();

  static final class Int extends Atom {
    final int value;

    public Int(int value) {
      this.value = value;
    }

    @Override
    int intValue() {
      return value;
    }

    @Override
    Atom fn(Atom atom) {
      throw new UnsupportedOperationException("int value is a primitive");
    }

    @Override
    public String toString() {
      return "Int_value=" + value;
    }
  }

  static final class Increment extends Atom {

    @Override
    Atom fn(Atom atom) {
      return new Int(atom.intValue() + 1);
    }
  }

  static final Increment INC = new Increment();

  static final Atom N[] = new Atom[10];
  static {
    Atom next = ZERO;
    for (int i = 0; i < N.length; ++i) {
      N[i] = next;
      next = SUCC.fn(next);
    }
  }

  static Atom numcall(Atom a) {
    return a.fn(INC).fn(new Int(0));
  }

  public static void main(String[] args) {
    System.out.println("LambdaCalc - java");

    for (int i = 0; i < N.length; ++i) {
      System.out.println("N" + i + "(inc, 0) = " + numcall(N[i]));
    }

    System.out.println("N2^N3 = " + numcall(N[3].fn(N[2])));
    System.out.println("N3^N2 = " + numcall(N[2].fn(N[3])));
    System.out.println("N5^N2 = " + numcall(N[2].fn(N[5])));
    System.out.println("N2^N5 = " + numcall(N[5].fn(N[2])));

    long executionTime = System.currentTimeMillis();
    final Atom result = numcall(N[9].fn(N[9]));
    executionTime = System.currentTimeMillis() - executionTime;
    System.out.println("N9^N9 = " + result + ", executionTime=" + executionTime);
  }
}
