
var inc = function (x) { return x + 1; };

// ZERO := λs.λz.z
var zero = function (s) { return function (z) { return z; } };
// SUCC := λn.λs.λz.s ((n s) z)
var succ = function (n) {
  return function (s) {
    return function (z) {
      return s(n(s)(z));
    }
  }
};

// POW := λb.λe.e b
var pow = function (b) { return function (e) { return e(b); }; };

var N = [];
var it = zero;
for (var i = 0; i < 9; ++i) {
  N.push(it);
  it = succ(it);
}

//
// exports
//

exports.inc = inc;
exports.zero = zero;
exports.succ = succ;
exports.pow = pow;
exports.N = N;


