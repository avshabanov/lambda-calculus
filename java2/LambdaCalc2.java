/**
 * Incarnation of LambdaCalc "done right" - using interfaces to show the difference between invokevirtual and
 * invokeinterface.
 */
public class LambdaCalc2 {
  interface Atom {
    int intValue();

    Atom fn(Atom atom);
  }

  abstract static class AbstractAtom implements Atom {
    @Override
    public int intValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Atom fn(Atom atom) {
      throw new UnsupportedOperationException();
    }
  }


  //  (def zero (% s (% z z)))
  static final class Zero extends AbstractAtom {
    static final class Z extends AbstractAtom {
      @Override
      public Atom fn(Atom z) { return z; }
    }

    static final Z lambdaZ = new Z(); /* optimization */

    @Override
    public Atom fn(Atom s) { return lambdaZ; }
  }

  static final Zero ZERO = new Zero();

  //  (def succ (% n (% s (% z s ((n s) z)))))
  static final class Succ extends AbstractAtom {

    @Override
    public Atom fn(final Atom n) {
      return new AbstractAtom() {
        @Override
        public Atom fn(final Atom s) {
          return new AbstractAtom() {
            @Override
            public Atom fn(Atom z) {
              return s.fn(n.fn(s).fn(z));
            }
          };
        }
      };
    }
  }

  static final Succ SUCC = new Succ();

  static final class Int extends AbstractAtom {
    final int value;

    public Int(int value) {
      this.value = value;
    }

    @Override
    public int intValue() {
      return value;
    }

    @Override
    public String toString() {
      return "Int_value=" + value;
    }
  }

  static final class Increment extends AbstractAtom {

    @Override
    public Atom fn(Atom atom) {
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
    System.out.println("LambdaCalc2 - java");

    for (int i = 0; i < N.length; ++i) {
      System.out.println("N" + i + "(inc, 0) = " + numcall(N[i]));
    }

    System.out.println("N2^N3 = " + numcall(N[3].fn(N[2])));
    System.out.println("N3^N2 = " + numcall(N[2].fn(N[3])));
    System.out.println("N5^N2 = " + numcall(N[2].fn(N[5])));
    System.out.println("N2^N5 = " + numcall(N[5].fn(N[2])));

    for (int i = 0; i < 3; ++i) {
      long executionTime = System.currentTimeMillis();
      final Atom result = numcall(N[9].fn(N[9]));
      executionTime = System.currentTimeMillis() - executionTime;
      System.out.println("[invokeinterface] Attempt #" + i + " N9^N9 = " + result + ", executionTime=" + executionTime);
    }
  }
}

/*
Another, more direct approach:

public class LambdaCalc2 {
  interface Atom {
    int intValue();

    Atom fn(Atom atom);
  }

  //  (def zero (% s (% z z)))
  static final class Zero implements Atom {
    static final class Z implements Atom {
      @Override
      public int intValue() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Atom fn(Atom z) { return z; }
    }

    static final Z lambdaZ = new Z();

    @Override
    public int intValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Atom fn(Atom s) { return lambdaZ; }
  }

  static final Zero ZERO = new Zero();

  //  (def succ (% n (% s (% z s ((n s) z)))))
  static final class Succ implements Atom {

    @Override
    public int intValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Atom fn(final Atom n) {
      return new Atom() {
        @Override
        public int intValue() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Atom fn(final Atom s) {
          return new Atom() {
            @Override
            public int intValue() {
              throw new UnsupportedOperationException();
            }

            @Override
            public Atom fn(Atom z) {
              return s.fn(n.fn(s).fn(z));
            }
          };
        }
      };
    }
  }

  static final Succ SUCC = new Succ();

  static final class Int implements Atom {
    final int value;

    public Int(int value) {
      this.value = value;
    }

    @Override
    public int intValue() {
      return value;
    }

    @Override
    public Atom fn(Atom atom) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "Int_value=" + value;
    }
  }

  static final class Increment implements Atom {

    @Override
    public int intValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Atom fn(Atom atom) {
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
    System.out.println("LambdaCalc2 - java");

    for (int i = 0; i < N.length; ++i) {
      System.out.println("N" + i + "(inc, 0) = " + numcall(N[i]));
    }

    System.out.println("N2^N3 = " + numcall(N[3].fn(N[2])));
    System.out.println("N3^N2 = " + numcall(N[2].fn(N[3])));
    System.out.println("N5^N2 = " + numcall(N[2].fn(N[5])));
    System.out.println("N2^N5 = " + numcall(N[5].fn(N[2])));

    for (int i = 0; i < 3; ++i) {
      long executionTime = System.currentTimeMillis();
      final Atom result = numcall(N[9].fn(N[9]));
      executionTime = System.currentTimeMillis() - executionTime;
      System.out.println("[invokeinterface] Attempt #" + i + " N9^N9 = " + result + ", executionTime=" + executionTime);
    }
  }
}

 */
