// g++ -Wall -Werror -Wimplicit -pedantic -std=c++1y -fsyntax-only meta-lambda-calc.cpp
// g++ -Wall -Werror -Wimplicit -pedantic -std=c++1y meta-lambda-calc.cpp -Os -o /tmp/mlc

#include <iostream>

using std::cout;
using std::endl;

auto inc = [](auto x) { return x + 1; };
auto zero = [](auto s) { return [](auto z) { return z; }; };
auto succ = [](auto n) {
  return [=](auto s) {
    return [=](auto z) {
      return s(n(s)(z));
    };
  };
};

auto pow = [](auto b) {
  return [=](auto e) { return e(b); };
};

auto plus = [](auto a) {
  return [=](auto b) { return a(succ)(b); };
};

auto mul = [](auto a) {
  return [=](auto b) { return a(plus(b))(zero); };
};

auto pred = [](auto n) {
  return [=](auto f) {
    return [=](auto x) {
      auto gf = [=](auto g) {
        return [=](auto h) { return h(g(f)); };
      };
      auto ux = [=](auto u) { return x; };
      auto uu = [](auto u) { return u; };

      return n(gf)(ux)(uu);
    };
  };
};

// reporting

auto print = [](const char* op, auto f) {
  cout << op << f() << endl;
};

int main() {
  auto n0 = zero;
  auto n1 = succ(n0);
  auto n2 = succ(n1);
  auto n3 = succ(n2);
  auto n4 = succ(n3);
  auto n5 = succ(n4);
  auto n6 = succ(n5);
  auto n7 = succ(n6);
  auto n8 = succ(n7);
  auto n9 = succ(n8);
  auto n10 = succ(n9);
  
  print("succ(zero) = ", [&]{ return succ(zero)(inc)(0); });
  print("n2^n3 = ", [&]{ return pow(n2)(n3)(inc)(0); });
  print("n3^n2 = ", [&]{ return pow(n3)(n2)(inc)(0); });
  //print("n9^n9 = ", [&]{ return pow(n9)(n9)(inc)(0); });
  print("n2^n3(F) = ", [&]{ return pow(n2)(n3)(inc)(0.0); });
  print("n9+n5 = ", [&]{ return plus(n9)(n5)(inc)(0); });
  print("n9*n5 = ", [&]{ return mul(n9)(n5)(inc)(0); });
  print("n10-1 = ", [&]{ return pred(n10)(inc)(0); });

#ifdef I_CAN_WAIT
  auto n10000 = pow(n10)(n4);
  print("n9999 = ", [&]{ return pred(n10000)(inc)(0); });
#endif

  return 0;
}

