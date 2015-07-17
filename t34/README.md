
## Demo

```lisp
(define zero (lambda (s) (lambda (z) z)))
(define succ (lambda (n) (lambda (s) (lambda (z) (s ((n s) z))))))
(define n0 zero)
(define n1 (succ n0))
(define n2 (succ n1))
(define n3 (succ n2))
(define n4 (succ n3))
(define n5 (succ n4))
(define n6 (succ n5))
(define n7 (succ n6))
(define n8 (succ n7))
(define n9 (succ n8))
(define pow (lambda (b) (lambda (e) (e b))))

((((pow n2) n3) inc) 0)
((((pow n3) n2) inc) 0)
```

Stress test:

```lisp
((((pow n9) n9) inc) 0)
```
