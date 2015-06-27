// g++ -Wall -Werror -Wimplicit -pedantic -std=c++11 -fsyntax-only LambdaCalcIntrusivePtr.cpp
// g++ -DENABLE_MEM_STATS -Wall -Werror -Wimplicit -pedantic -std=c++11 -fsyntax-only LambdaCalcIntrusivePtr.cpp
// g++ -DENABLE_BOOST_POOL -Wall -Werror -Wimplicit -pedantic -std=c++11 -fsyntax-only LambdaCalcIntrusivePtr.cpp
//
// Default mode (standard new, no statistics)
// g++ -Wall -Werror -Wimplicit -pedantic -std=c++11 -O3 LambdaCalcIntrusivePtr.cpp -o /tmp/lcpp-intp
//
// Malloc, with statistics
// g++ -DENABLE_MEM_STATS -Wall -Werror -Wimplicit -pedantic -std=c++11 -O3 LambdaCalcIntrusivePtr.cpp -o /tmp/lcpp-intp
//
// With boost pool (and automatic statistics):
// g++ -DENABLE_BOOST_POOL -Wall -Werror -Wimplicit -pedantic -std=c++11 -O3 -lboost_system LambdaCalcIntrusivePtr.cpp -o /tmp/lcpp-intp
//
//
// Then calc N9^N9 - /tmp/lcpp-intp n9

#include <iostream>
#include <vector>
#include <exception>

#include <boost/intrusive_ptr.hpp>

#if defined(ENABLE_BOOST_POOL) || defined(ENABLE_MEM_STATS)
#include <cassert> // for asserts in new overrides
#endif

#if defined(ENABLE_BOOST_POOL)
#include <boost/pool/pool.hpp>
#include <boost/pool/pool_alloc.hpp>
#endif

#if defined(ENABLE_MEM_STATS)
#include <cstdlib> // for malloc/free in new/delete overrides
#endif

/* perf measurement */
#include <sys/time.h>


using std::endl;
using std::cout;
using std::cerr;
using std::logic_error;

using boost::intrusive_ptr;

//
// Memory helpers
//

// define memory statistics if boost pool or mem stats macros are defined
#if defined(ENABLE_BOOST_POOL) || defined(ENABLE_MEM_STATS)

struct AllocFree {
  int alloc = 0;
  int free = 0;
};

struct AtomAllocStats {
  AllocFree af_Zero_Z;
  AllocFree af_Zero;
  AllocFree af_Succ_Z;
  AllocFree af_Succ_S;
  AllocFree af_Succ;
  AllocFree af_Int;
  AllocFree af_Inc;
};

static AtomAllocStats gAllocStats;

static void printMemUsageStats() {
  cout << "MEMORY USAGE:" << endl
      << "Zero_Z alloc=" << gAllocStats.af_Zero_Z.alloc << ", free=" << gAllocStats.af_Zero_Z.free  << endl
      << "Zero   alloc=" << gAllocStats.af_Zero.alloc   << ", free=" << gAllocStats.af_Zero.free    << endl
      << "Succ_Z alloc=" << gAllocStats.af_Succ_Z.alloc << ", free=" << gAllocStats.af_Succ_Z.free  << endl
      << "Succ_S alloc=" << gAllocStats.af_Succ_S.alloc << ", free=" << gAllocStats.af_Succ_S.free  << endl
      << "Succ   alloc=" << gAllocStats.af_Succ.alloc   << ", free=" << gAllocStats.af_Succ.free    << endl
      << "Int    alloc=" << gAllocStats.af_Int.alloc    << ", free=" << gAllocStats.af_Int.free     << endl
      << "Inc    alloc=" << gAllocStats.af_Inc.alloc    << ", free=" << gAllocStats.af_Inc.free     << endl
      << endl;
}

#else

// no mem stats - do not report about mem use
static void printMemUsageStats() {
}

#endif // defined(ENABLE_BOOST_POOL) || defined(ENABLE_MEM_STATS)

// define memory allocators
#if defined(ENABLE_BOOST_POOL)

// boost pool-based new/delete overrides
// TODO: release functionality
//#define OVERRIDE_NEW_AND_DELETE_FOR_CLASS(AtomClass) \
//  static boost::fast_pool_allocator<AtomClass> g_static_pool; \
//  static void* operator new (size_t size) { \
//    assert(sizeof(AtomClass) == size); \
//    return g_static_pool.allocate(size); \
//  } \
//  \
//  static void operator delete (void *p) { \
//    g_static_pool.deallocate(p, sizeof(AtomClass)); \
//    ++gAllocStats.af_ ## AtomClass.free; \
//  }

// TODO: implement - now uses the same impl as in mem stats
#define OVERRIDE_NEW_AND_DELETE_FOR_CLASS(AtomClass) \
  static void* operator new (size_t size) { \
    assert(sizeof(AtomClass) == size); \
    void* p = malloc(size); \
    if (p == NULL) { \
      throw new std::bad_alloc(); \
    } \
    ++gAllocStats.af_ ## AtomClass.alloc; \
    return p; \
  } \
  \
  static void operator delete (void *p) { \
    free(p); \
    ++gAllocStats.af_ ## AtomClass.free; \
  }

#elif defined(ENABLE_MEM_STATS)

// malloc/free based new/delete overrides
#define OVERRIDE_NEW_AND_DELETE_FOR_CLASS(AtomClass) \
  static void* operator new (size_t size) { \
    assert(sizeof(AtomClass) == size); \
    void* p = malloc(size); \
    if (p == NULL) { \
      throw new std::bad_alloc(); \
    } \
    ++gAllocStats.af_ ## AtomClass.alloc; \
    return p; \
  } \
  \
  static void operator delete (void *p) { \
    free(p); \
    ++gAllocStats.af_ ## AtomClass.free; \
  }

#else

// no new/delete overrides
#define OVERRIDE_NEW_AND_DELETE_FOR_CLASS(AtomClass)

#endif // ENABLE_MEM_STATS


//
// types
//

class Atom;

typedef intrusive_ptr<Atom> AtomPtr;

class Atom {
private:
  int refCount;

public:
  Atom(): refCount(0) {}

  virtual int toInt() = 0;
  virtual AtomPtr f1(const AtomPtr& arg) = 0;
  virtual ~Atom() {};

  friend void intrusive_ptr_add_ref(Atom* p) { ++p->refCount; }
  friend void intrusive_ptr_release(Atom* p) { if (--p->refCount == 0) { delete p; } }
};



class AbstractAtom : public Atom {
public:
  virtual int toInt() {
    throw new logic_error("toInt should not be called");
  }

  virtual AtomPtr f1(const AtomPtr& arg) {
    throw new logic_error("f1 should not be called");
  }
};

// Zero lambda

class Zero_Z : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& arg) {
    return arg;
  }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Zero_Z);
};

class Zero : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& arg) {
    return AtomPtr(new Zero_Z());
  }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Zero);
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

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Succ_Z);
};

class Succ_S : public AbstractAtom { // (% s (% z s ((n s) z)))
private:
  AtomPtr n;
public:
  Succ_S(const AtomPtr& nArg) : n(nArg) {}

  virtual AtomPtr f1(const AtomPtr& s) {
    return AtomPtr(new Succ_Z(n, s));
  }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Succ_S);
};

class Succ : public AbstractAtom { // (def succ (% n (% s (% z s ((n s) z)))))
public:
  virtual AtomPtr f1(const AtomPtr& n) {
    return AtomPtr(new Succ_S(n));
  }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Succ);
};

// Int

class Int : public AbstractAtom {
private:
  int val;
public:
  Int(int v) : val(v) {}

  virtual int toInt() { return val; }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Int);
};

// Inc

class Inc : public AbstractAtom {
public:
  virtual AtomPtr f1(const AtomPtr& intAtom) {
    return AtomPtr(new Int(intAtom->toInt() + 1));
  }

  OVERRIDE_NEW_AND_DELETE_FOR_CLASS(Inc);
};

// helpers

#define NANO_UNIT     (1000000000LL)

static void print_formatted_nano_time(long long nano_time) {
  long long sec = nano_time / NANO_UNIT;
  long long nanos = (nano_time - (sec * NANO_UNIT));
  cout << "nano_time = " << sec << " sec " << nanos / 1000 << " " << nanos % 1000 << " msec" << endl;
}


// entry point

static void runDemo(int argc, const char** argv) {
  cout << "Lambda Calc C++ Demo" << endl;

  const int count = 10;
  std::vector<AtomPtr> n;

  auto succ = AtomPtr(new Succ());
  auto zeroVal = AtomPtr(new Int(0));
  auto inc = AtomPtr(new Inc());

  // init numerals
  for (int i = 0; i < count; ++i) {
    if (i == 0) {
      n.push_back(AtomPtr(new Zero()));
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
}

int main(int argc, const char** argv) {
  runDemo(argc, argv);
  printMemUsageStats();
  return 0;
}
