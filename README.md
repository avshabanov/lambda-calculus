## Overview

Illustrations on how lambda calculus primitives can be implemented in various lisp dialects.
At the moment it is implemented for the following lisp dialects:
* Clojure (tested on version 1.6.0)
* Common Lisp (tested on SBCL 1.2.2)

## Performance

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
;; - (declaim (optimize (debug 0) (safety 0) (speed 3) (space 0)))
;; - (load "lc.lisp") (compile-file "lc.lisp") (load "lc.fasl")
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
;; Clojure Performance:
;; (time (numcall pow (nth N 9) (nth N 9)))
;;
;;    "Elapsed time: 19578.087145 msecs"
;;    387420489
;; ---
;; 19.6 seconds
;;

```

