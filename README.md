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
;; Clojure Performance (with statistics):
;; (time (numcall pow (nth N 9) (nth N 9)))
;;
;;    "Elapsed time: 19578.087 msecs"
;;    387420489
;; ---
;; 19.6 seconds
;;
;; -----------------------------------------------------------------------------
;; Scheme Performance:
;; ((((pow n9) n9) inc) 0)
;; ---
;; >1 minute
;;

```

