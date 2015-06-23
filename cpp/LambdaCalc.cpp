// g++ -Wall -Werror -Wimplicit -pedantic -std=c++11 -fsyntax-only LambdaCalc.cpp
// g++ -Wall -Werror -Wimplicit -pedantic -std=c++11 -O3 LambdaCalc.cpp -o /tmp/lcpp

// Then calc N9^N9 - /tmp/lcpp n9

#include <iostream>
#include <cstdlib>
#include <memory>
#include <vector>

  /* perf measurement */
#include <sys/time.h>

using std::endl;
using std::cout;
using std::cerr;

using std::make_shared;

// types

class Atom;

typedef std::shared_ptr<Atom> AtomPtr;

class Atom : public std::enable_shared_from_this<Atom> {
public:
  virtual int toInt() = 0;
  virtual AtomPtr f1(const AtomPtr& arg) = 0;
  virtual ~Atom() {};
};

class AbstractAtom : public Atom {
public:
  virtual int toInt() {
    cerr << ";; FATAL: toInt should not be called" << endl;
    abort();
    return 0;
  }

  virtual AtomPtr f1(const AtomPtr& arg) {
    cerr << ";; FATAL: f1 should not be called" << endl;
    abort();
    return AtomPtr(0);
  }
};

// Zero lambda

class Zero_Z : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& arg) {
    return arg;
  }
};

class Zero : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& arg) {
    return make_shared<Zero_Z>();
  }
};

// Succ lambda

class Succ_Z : public AbstractAtom { // (% z s ((n s) z))
private:
  AtomPtr n;
  AtomPtr s;
public:
  Succ_Z(const AtomPtr& nArg, const AtomPtr& sArg) : n(nArg), s(sArg) {}

  virtual AtomPtr f1(const AtomPtr& z) {
    return s->f1(n->f1(s)->f1(z));
  }
};

class Succ_S : public AbstractAtom { // (% s (% z s ((n s) z)))
private:
  AtomPtr n;
public:
  Succ_S(const AtomPtr& nArg) : n(nArg) {}

  virtual AtomPtr f1(const AtomPtr& s) {
    return make_shared<Succ_Z>(n, s);
  }
};

class Succ : public AbstractAtom { // (def succ (% n (% s (% z s ((n s) z)))))
public:
  virtual AtomPtr f1(const AtomPtr& n) {
    return make_shared<Succ_S>(n);
  }
};

// Int

class Int : public AbstractAtom {
private:
  int val;
public:
  Int(int v) : val(v) {}

  virtual int toInt() { return val; }
};

// Inc

class Inc : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& intAtom) {
    return make_shared<Int>(intAtom->toInt() + 1);
  }
};

// helpers

#define NANO_UNIT       (1000000000LL)

static void print_formatted_nano_time(long long nano_time) {
  long long sec = nano_time / NANO_UNIT;
  long long nanos = (nano_time - (sec * NANO_UNIT));
  cout << "nano_time = " << sec << " sec " << nanos / 1000 << " " << nanos % 1000 << " msec" << endl;
}


// entry point

int main(int argc, const char** argv) {
  cout << "Lambda Calc C++ Demo" << endl;

  const int count = 10;
  std::vector<AtomPtr> n;

  auto succ = make_shared<Succ>();
  auto zeroVal = make_shared<Int>(0);
  auto inc = make_shared<Inc>();

  // init numerals
  for (int i = 0; i < count; ++i) {
    if (i == 0) {
      n.push_back(make_shared<Zero>());
    } else {
      n.push_back(succ->f1(n.at(i - 1)));
    }
  }

  bool calcN9 = false;
  if (argc > 1 && 0 == strcmp(argv[1], "n9")) {
    calcN9 = true;
  } 

  if (calcN9) {
    cout << "CalcN9..." << endl;

    struct timeval start;
    struct timeval stop;

    gettimeofday(&start, NULL);
    int result = n.at(9)->f1(n.at(9))->f1(inc)->f1(zeroVal)->toInt();
    gettimeofday(&stop, NULL);

    long long nano_time = (stop.tv_sec - start.tv_sec) * NANO_UNIT + (stop.tv_usec - start.tv_usec) * 1000L;
    cout << "N9^N9 = " << result << endl;
    print_formatted_nano_time(nano_time);
  } else {
    cout << "Num Folding Demo" << endl;

    // demo num folding
    for (int i = 0; i < count; ++i) {
      int foldValue = n.at(i)->f1(inc)->f1(zeroVal)->toInt();
      cout << "N" << i << " value = " << foldValue << endl;
    }

    // demo power function
    cout << "N2^N3 = " << n.at(3)->f1(n.at(2))->f1(inc)->f1(zeroVal)->toInt() << endl;
    cout << "N3^N2 = " << n.at(2)->f1(n.at(3))->f1(inc)->f1(zeroVal)->toInt() << endl;
  }

  cout << "DONE." << endl;
  return 0;
}

