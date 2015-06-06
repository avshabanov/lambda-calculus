
;; no optimization
;; (declaim (optimize (debug 3) (safety 3) (speed 0) (space 0)))

;; maximum optimization
;;(declaim (optimize (debug 0) (safety 0) (speed 3) (space 0)))

(defun unfold-expr (expr)
  (if (consp expr)
    (case (length expr)
      (1 (unfold-expr (car expr)))
      (otherwise
        (if (or (eq '% (car expr)) (eq 'function (car expr))) expr
          (loop for x in (loop for y in expr collect (unfold-expr y))
            with a = nil
            do (setf a (if (null a) x `(funcall ,a ,x)))
            finally (return a)))))
    expr))

(defmacro lc (&rest body)
  (unfold-expr body))

(defmacro % (varname &rest fbody)
  `(lambda (,varname) ,(unfold-expr fbody)))

;;
;; Lambda Calculus Entities
;;

;;
;; Church numerals and arithmetic operations
;;

(defparameter inc (lambda (x)
                    (declare (type integer x))
                    (+ x 1)))

(defparameter zero (% s (% z z)))
(defparameter succ (% n (% s (% z s ((n s) z)))))

(defparameter n0 zero)
(defparameter n1 (lc succ n0))
(defparameter n2 (lc succ n1))
(defparameter n3 (lc succ n2))
(defparameter n4 (lc succ n3))
(defparameter n5 (lc succ n4))
(defparameter n6 (lc succ n5))
(defparameter n7 (lc succ n6))
(defparameter n8 (lc succ n7))
(defparameter n9 (lc succ n8))

;;
;; Arithmetic functions
;;

;; power (exponent)
;;    POW := λb.λe.e b
(defparameter pow (% b (% e e b)))

;; plus
;;    PLUS := λm.λn.m SUCC n
(defparameter plus (% a (% b (a succ) b)))

;; multiplication
;;    MULT := λm.λn.λf.m (n f)
;;      - Alternatively
;;    MULT := λm.λn.m (PLUS n) 0
(defparameter mul (% a (% b a (plus b) zero)))

;; Test:
;; (lc (succ (succ zero)) inc 0)
