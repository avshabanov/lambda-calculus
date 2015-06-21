## Overview

Illustrations on how lambda calculus primitives can be implemented in various lisp dialects.
At the moment it is implemented for the following lisp dialects:
* Clojure (tested on version 1.6.0)
* Common Lisp (tested on SBCL 1.2.2)
* Scheme (tested on tinyscheme and scheme48, latest versions taken from brew repos at 2015-06-06)

The most advanced implementation at the moment is a clojure one, it includes Church numerals, boolean logic and inline tests.
The others include just Church numerals and power function.

## Performance

Tested on mid-2013 Mac Book Pro 13''.

```
;; -----------------------------------------------------------------------------
;; SBCL Performance:
;;    * (time (lc (pow n9 n9) inc 0))
;;
;;    Evaluation took:
;;    10.202 seconds of real time
;;    10.201781 seconds of total run time (10.156016 user, 0.045765 system)
;;    [ Run times consist of 0.606 seconds GC time, and 9.596 seconds non-GC time. ]
;;    100.00% CPU
;;    26,525,671,562 processor cycles
;;    1 page fault
;;    12,397,474,192 bytes consed
;;
;;    387420489
;; ---
;; 10.2 seconds
;;
;; -----------------------------------------------------------------------------
;; SBCL Performance (max optimizations):
;; - Preparations: (declaim (optimize (debug 0) (safety 0) (speed 3) (space 0)))
;; - Loaded as follows: (progn (load "lambda-calculus.lisp") (compile-file "lambda-calculus.lisp") (load "lambda-calculus.fasl") "DONE")
;;
;;    (time (lc (pow n9 n9) inc 0))
;;
;;    Evaluation took:
;;    9.488 seconds of real time
;;    9.477487 seconds of total run time (9.403645 user, 0.073842 system)
;;    [ Run times consist of 0.275 seconds GC time, and 9.203 seconds non-GC time. ]
;;    99.88% CPU
;;    24,666,184,055 processor cycles
;;    23 page faults
;;    12,397,474,192 bytes consed
;;
;;    387420489
;; ---
;; 9.5 seconds
;;
;; -----------------------------------------------------------------------------
;; CLISP Performance 
;;  (time (lc (pow n9 n9) inc 0))
;; Real time: 242.38795 sec.
;; Run time: 241.44707 sec.
;; Space: 27894276992 Bytes
;; GC: 34414, GC time: 112.93121 sec.
;; 387420489
;; ---
;; 242.4 seconds
;; -----------------------------------------------------------------------------
;; Clojure Performance (with statistics):
;; (time (numcall pow (nth N 9) (nth N 9)))
;;
;;    "Elapsed time: 19578.087 msecs"
;;    387420489
;; ---
;; 19.6 seconds
;;
;; -----------------------------------------------------------------------------
;; Clojure Performance (no statistics):
;; (time (numcall pow (nth N 9) (nth N 9)))
;;
;;    "Elapsed time: 7625.926 msecs"
;;    387420489
;; ---
;; 7.6 seconds
;;
;; -----------------------------------------------------------------------------
;; Scheme Performance:
;; ((((pow n9) n9) inc) 0)
;; ---
;; >1 minute
;;
;; -----------------------------------------------------------------------------
;; Java Performance:
;; N9^N9 = Int_value=387420489, executionTime=4217
;; ---
;; 4.2 seconds
;; -----------------------------------------------------------------------------
;; NodeJS Performance:
;; lc.pow(lc.succ(lc.N[8]))(lc.succ(lc.N[8]))(lc.inc)(0)
;; --> 387420489
;;
;; var t=(new Date().getTime()); var ret = lc.pow(lc.succ(lc.N[8]))(lc.succ(lc.N[8]))(lc.inc)(0); t = (new Date().getTime()) - t;
;;
;; t=17830 msec
;; ---
;; 17.8 seconds
;; -----------------------------------------------------------------------------
;; C Performance:
;; $ gcc -Wall -Werror -Wimplicit -pedantic -std=c99 -O3 lambda-calc.c -o /tmp/lc
;; $ /tmp/lc n9
;; N9^N9 = 387420489
;; nano_time = 12 sec 333215 000 msec
;; ---
;; 12 seconds (6 times more than in Java)
```

## NodeJS Implementation How To

```
var lc = require("./lambdacalc.js");

// inc check:

lc.N[0](lc.inc)(0); // -> 0
lc.N[1](lc.inc)(0); // -> 1
lc.N[8](lc.inc)(0); // -> 8

// pow check:
lc.pow(lc.N[2])(lc.N[3])(lc.inc)(0); // -> 8
lc.pow(lc.N[3])(lc.N[2])(lc.inc)(0); // -> 9
```

