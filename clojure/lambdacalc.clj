(ns lambdacalc)

;; Debug variables for tracing lambda calc execution
(def ^:dynamic *stat-call* (atom 0))
(def stat-enabled false) ; debug will be turned off if false (and thus *won't affect exection*)

(defn print-stats []
  (println ";; Execution Statistics")
  (println ";;  Number of calls:   " @*stat-call*)
  (println))

;;
;; Helpers
;;

(defmacro %
  "Helper macro for defining lambda functions (each function always takes exactly one argument).
  (% x {body}) is a rough equivalent of (fn x {callable-form}).
  This macro also transforms lambda function body into the callable form, so that the sequence of terms is
  transformed into the list of function calls so that each function is called with exactly one argument, e.g.:
  a b c d -> (((a b) c) d)
  See also lc macro for seeing how body tranformation works."
  [var-name & body]
  (let [unfold-body (condp = (count body)
                      0 (assert false "Unable to unfold body of no elements")
                      1 (first body)
                      2 body
                      (reduce
                        (fn [c e] (list c e))
                        (take 2 body)
                        (nthrest body 2)))]
    `(fn [~var-name]
       ~(if stat-enabled `(do (swap! *stat-call* inc) ~unfold-body) unfold-body))))

;; (macroexpand-1 '(% x x d))

(defmacro lc
  "Helper Macro for calling lambda calculus functions.
  Transforms a given list to the list of nested calls,
  so that each function in this list is called only once.
  Does not work recursively.
  E.g.:
    (lc a b c d) -> (((a b) c) d)
    (lc a (b c) d) -> ((a (b c)) d)
  "
  [& body]
  (reduce
    (fn [c e] (list c e))
    (take 2 body)
    (nthrest body 2)))

;;
;; Lambda Calculus Entities
;;

;;
;; Church numerals and arithmetic operations
;;

;; base church numerals: zero and successor function
(def zero (% s (% z z))) ; <=> (def zero (fn [s] (fn [z] z)))
(def succ (% n (% s (% z s ((n s) z)))))

;; convenience definition of the church number one
(def one (succ zero))

;; infinite sequence of Church numerals
(def N (iterate succ zero))
;; Illustration: (map (fn [x] (lc x inc 0)) (take 5 N))

;;
;; Arithmetic functions
;;

;; power (exponent)
;;    POW := λb.λe.e b
(def pow (% b (% e e b)))

;; plus
;;    PLUS := λm.λn.m SUCC n
(def plus (% a (% b (a succ) b)))

;; multiplication
;;    MULT := λm.λn.λf.m (n f)
;;      - Alternatively
;;    MULT := λm.λn.m (PLUS n) 0
(def mul (% a (% b a (plus b) zero)))

;; predecessor (decrement)
;;    PRED := λn.λf.λx.n (λg.λh.h (g f)) (λu.x) (λu.u)
(def pred (% n (% f (% x n (% g (% h h (g f))) (% u x) (% u u)))))

;; subtraction
;;    SUB := λm.λn.n PRED m
(def sub (% m (% n n pred m)))


;;
;; Boolean logic
;;

(def l-true (% a (% b a)))                ; logical 'true'
(def l-false (% a (% b b)))               ; logical 'false'

(def l-and (% p (% q p q p)))             ; logical 'and'
(def l-or (% p (% q p p q)))              ; logical 'or'
(def l-not (% p p l-false l-true))        ; logical 'not'
(def l-xor (% p (% q p (l-not q) q)))     ; logical 'xor'

(def l-if (% p (% a (% b p a b))))        ; if (p) then (a) else (b)

(def l-zero? (% n n (% x l-false) l-true)) ; 'is zero?' test for numerals

(def l-leq (% m (% n l-zero? (lc sub m n)))) ; less-or-equal test for numerals
(def l-eq (% m (% n l-and (lc l-leq m n) (lc l-leq n m)))) ; equality test for numerals

;;
;; Inline tests for church numerals and arithmetic operations
;;

;; calls lambda expression and uses 'inc' (+1) as a sequence function
;; and 0 (zero) as zero function
(defmacro numcall [& body]
  (let [extbody (concat body '(inc 0))]
    `(lc ~@extbody)))

;; check numerals form
(dotimes [i 100]
  (assert (= i (numcall (nth N i)))))

;; check addition
(dotimes [pos1 10]
  (dotimes [pos2 10]
    (let [r (numcall plus (nth N pos1) (nth N pos2))]
      (assert (= (+ pos1 pos2) r) (str pos1 " + " pos2 " != " r)))))

;; check power
(dotimes [pos1 5]
  (dotimes [pos2 5]
    (let [r (numcall pow (nth N pos1) (nth N pos2))]
      (assert (= (reduce * (repeat pos2 pos1)) r) (str pos1 " ^ " pos2 " != " r)))))

;; check multiplication
(dotimes [i 10]
  (dotimes [j 10]
    (let [pos1 (inc i) pos2 (inc j) r (numcall mul (nth N pos1) (nth N pos2))]
      (assert (= (* pos1 pos2) r) (str pos1 " * " pos2 " != " r)))))

;; check pred
(assert (= 0 (lc (pred zero) inc 0)) "Predecessor of zero should be zero")
(dotimes [i 10]
  (let [num (inc i) r (numcall pred (nth N num))]
    (assert (= i r) (str num " - 1 != " r))))

;; check subtraction
(dotimes [i 10]
  (dotimes [j i]
    (let [r (numcall sub (nth N i) (nth N j))]
      (assert (= r (- i j)) (str i " - " j " != " r)))))

;;
;; Inline tests for church booleans
;;

(defmacro l-call [& body]
  (let [extbody (concat body '(true false))]
    `(lc ~@extbody)))

(assert (l-call l-true))
(assert (not (l-call l-false)))

(assert (l-call l-and l-true l-true))
(assert (not (l-call l-and l-true l-false)))
(assert (not (l-call l-and l-false l-true)))
(assert (not (l-call l-and l-false l-false)))

(assert (l-call l-or l-true l-true))
(assert (l-call l-or l-true l-false))
(assert (l-call l-or l-false l-true))
(assert (not (l-call l-or l-false l-false)))

(assert (l-call l-not l-false))
(assert (not (l-call l-not l-true)))

(assert (not (l-call l-xor l-true l-true)))
(assert (l-call l-xor l-true l-false))
(assert (l-call l-xor l-false l-true))
(assert (not (l-call l-xor l-false l-false)))

(assert (= "then" (lc l-if l-true "then" "else")))

(assert (l-call l-zero? zero))
(dotimes [i 9] (assert (not (l-call l-zero? (nth N (inc i))))))

;; test leq
(dotimes [i 9]
  (dotimes [j i]
    (assert (l-call l-leq (nth N j) (nth N i)) (str j "<=" i))
    (assert (not (l-call l-leq (nth N (inc i)) (nth N j))) (str (inc i) ">" j))))

;; test eq
(dotimes [i 5]
  (dotimes [j 5]
    (let [eq-expected (= i j)]
      (assert (= eq-expected (l-call l-eq (nth N i) (nth N j))) (str i (if eq-expected "=" "!=") j)))))

;; (succ n0) should be equivalent for (inc ((n0 inc) 0))
;; e.g.: ((n1 inc) 0) == 1
;; e.g.: ((n2 inc) 0) == 2
;; e.g.: ((n3 inc) 0) == 3

;; pow:
;; ((((pow n2) n3) inc) 0)      == 2^3 == 8
;; ((((pow n3) n2) inc) 0)      == 3^2 == 9
;; ((((pow n5) n4) inc) 0)      == 5^4 == 625
;; ((((pow n4) n5) inc) 0)      == 4^5 == 1024
;; ((((pow n8) n9) inc) 0)      == 8^9 == 134217728
;; - alternatively -
;; (lc pow n2 n3 inc 0)

;;
;; Power calls take a lot of resources, e.g.
;; (numcall pow (nth N 9) (nth N 8))
;; (numcall pow (nth N 9) (nth N 8)) ==> 43046721
;; @*stat-call* ==> 102235988, i.e. 102,235,988 (>102 million function calls to calculate that power)
