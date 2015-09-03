;; Alexander Shabanov - Church-alike numerals for Z-set.

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

(defn undefined [& args] (throw (IllegalStateException. "Undefined call attempted")))

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
;; Definitions
;;



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

(def l-negative? (% n n (% x l-false) (% y l-true) l-false))

(def l-positive? (% n n (% x l-true) (% y l-false) l-false))


;;
;; Numerals
;;

(def zero (% s (% p (% z z))))

;;Naive - (def succ (% n (% s (% p (% z s (((n s) p) z))))))

(def succ (% n
            (((l-if (l-negative? n))
              n)
              (% s (% p (% z (s (((n s) p) z))))))))

(def pred (% n (% s (% p (% z p (((n s) p) z))))))



;;
;; Boolean Testing
;;

(defmacro l-call [& body]
  (let [extbody (concat body '(true false))]
    `(lc ~@extbody)))

(assert (l-call (l-negative? (% s (% p (% z p z))))))
(assert (not (l-call (l-negative? (% s (% p (% z z)))))))
(assert (not (l-call (l-negative? (% s (% p (% z s z)))))))

(assert (l-call (l-positive? (% s (% p (% z s z))))))
(assert (not (l-call (l-positive? (% s (% p (% z z)))))))
(assert (not (l-call (l-positive? (% s (% p (% z p z)))))))


;;
;; Numerals Testing
;;


;; calls lambda expression and uses 'inc' (+1) as a sequence function
;; and 0 (zero) as zero function
(defmacro numcall [& body]
  (let [extbody (concat body '(inc dec 0))]
    `(lc ~@extbody)))

(assert (= 0 (numcall zero)))
(assert (= 1 (numcall (succ zero))))
(assert (= -1 (numcall (pred zero))))
