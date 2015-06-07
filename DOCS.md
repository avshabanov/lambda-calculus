
## On Lambda Calculus

* http://en.wikipedia.org/wiki/Lambda_calculus
* http://www.jetcafe.org/~jim/lambda.html
* http://plato.stanford.edu/entries/lambda-calculus/

## On Lisp

## Scheme vs Common Lisp Overview

Original can be found at http://community.schemewiki.org/?scheme-vs-common-lisp

```
What Common Lisp has got:         What Scheme has got:

Much better developed standard    SLIB + SRFI's + a hundred little
libraries                         libs that each do things differently
                                  and aren't very standardized.

   (Arguably Scheme is the place where new ideas fight for mindshare
    and prove themselves - but the fights and the multiplicity
    of contenders commits most code to one idea or another and
    limits the code's interoperability, longevity, and/or
    portability.)


A well-defined comprehensive      A well-defined minimal spec plus
spec and several implementations  dozens of variously comprehensive
which provide some extensions.    implementations.

Escaping continuations only.      Fully reentrant continuations.
                                  Scheme just wins on this point.

   (I have heard the arguments about whether fully reentrant
    continuations are worth the cost of stack copying, or the
    cost of heap-allocating and garbage collecting invocation
    frames.  I don't care.  I'm just noting here that you can
    do a *LOT* of things with them that are hard to do without
    them.)

Lots of iterative constructs      Memory-safe tail recursion avoids
                                  the need for iteration syntax.
                                  There's a looping construct, but
                                  it's more complicated than tail
                                  recursion so hardly anyone uses it.
                                  If you care for them, you can
                                  roll your own using continuations.

Both Lexically and Dynamically    Lexical scope only, per the standard.
scoped special vars.  Common      Dynamically scoped vars are provided
Lisp just wins on this point.     by some implementations as an extension
                                  but code using them is not portable.

     (I have heard the arguments about whether Dynamic scoping
      is or is not a Bad Idea in the first place.  I don't care.
      I'm just noting that you can do things with it that you
      can't easily do without it.)


C numeric types plus bignums      Implementation-defined numeric types,
and complex nums, but no exact/   in some implementations failing to
inexact distinction.              include bignums or complex nums.  An
                                  exact/inexact distinction is required
                                  by the standard but properly implemented
                                  in only about 3/4 of scheme systems.
                                  In a good implementation, numerics
                                  (capabilities and correctness) are
                                  better than most CLs; on average,
                                  they are worse.

Optional type declarations        Optional type declarations provided
allow blazing fast numeric        by a few implementations as extensions.
code to skip typechecking.        Code using them is nonportable.  Some
Common Lisp just wins on numeric  implementations provide blazing speed
calculation speed.                but generally at the expense of numeric
                                  type richness and/or standard
                                  conformance.

Signals and conditions, catch     Roll your own using fully reentrant
and throw.                        continuations, or use any of several
                                  libraries.

CLOS                              Roll your own objects using closures
                                  and macros, or any of several OO
                                  libraries. TinyCLOS and Meroon are
                                  the most popular.

Well-defined standard module      At least three competing well-defined
system.  Common Lisp just wins    module systems which it's a pain in
on this point.                    the butt to move modules between.
                                  (or roll your own using scope, macros,
                                  and/or preprocessing code).

Readtables for low-level          Implementation-defined means of doing
macrology. Common Lisp wins       low-level macrology - none of it
here.                             portable.

gensym tricks to avoid implicit   hygienic macros with define-syntax and
variable captures in high-level   syntax-case.  You *can't* capture a
macros.                           variable in a macro except explicitly.

    (Different people claim this as a "win" for both languages.
     I don't care.  There is little difference in what I can do
     with it, nor in how hard it is to do it, so I'm not the guy
     to judge a winner here.)

One-argument eval assumes         environment specifier is second arg to
environment                       eval, allowing access to multiple
                                  environments.  Scheme just wins here.

Lambda syntax supports keyword    Available as add-on library developed
arguments & default vals for      using macros, but widely ignored.
optional arguments.

Symbols have properties,          Variables have values and also names.
including but not limited to      The names are lexically indistinguishable
function value and data value.    from symbols but the value of a variable
                                  is not a property of its name symbol.
                                  Property lists are an extension
                                  provided by relatively few schemes.

Native hash tables.               Library hash tables.

Well-defined means of doing       A fragile hack that depends on common
binary I/O.  Common Lisp just     character encodings and/or assumption
wins here.                        that character ports act as byte ports.


Assertions.  Common Lisp just     In scheme you have to do this as two
wins here.                        macros; one for development, that signals
                                  an error if the condition isn't true, and
                                  one for production code which "expands"
                                  into nothing and gets out of the way.
                                  The compiler will not use your assertions
                                  to produce better code.


Large runtime environment         Small runtime environment, easily
                                  embeddable.  Scheme wins here.
```
