
## Valgrind Mem Usage Report

Demo mode, verification takes less than a second:

```
$ g++ -Wall -Werror -Wimplicit -pedantic -std=c++11 -O0 -g LambdaCalc.cpp -o /tmp/lcpp
$ valgrind --tool=memcheck /tmp/lcpp
==2987== Memcheck, a memory error detector
==2987== Copyright (C) 2002-2013, and GNU GPL'd, by Julian Seward et al.
==2987== Using Valgrind-3.10.1 and LibVEX; rerun with -h for copyright info
==2987== Command: /tmp/lcpp
==2987== 
Lambda Calc C++ Demo
Num Folding Demo
N0 value = 0
N1 value = 1
N2 value = 2
N3 value = 3
N4 value = 4
N5 value = 5
N6 value = 6
N7 value = 7
N8 value = 8
N9 value = 9
N2^N3 = 8
N3^N2 = 9
DONE.
==2987== 
==2987== HEAP SUMMARY:
==2987==     in use at exit: 29,482 bytes in 377 blocks
==2987==   total heap usage: 644 allocs, 267 frees, 47,514 bytes allocated
==2987== 
==2987== LEAK SUMMARY:
==2987==    definitely lost: 0 bytes in 0 blocks
==2987==    indirectly lost: 0 bytes in 0 blocks
==2987==      possibly lost: 0 bytes in 0 blocks
==2987==    still reachable: 4,096 bytes in 1 blocks
==2987==         suppressed: 25,386 bytes in 376 blocks
==2987== Rerun with --leak-check=full to see details of leaked memory
==2987== 
==2987== For counts of detected and suppressed errors, rerun with: -v
==2987== ERROR SUMMARY: 0 errors from 0 contexts (suppressed: 0 from 0)
```

Verifying N9^N9 in debug mode:

```
==3026== 
==3026== HEAP SUMMARY:
==3026==     in use at exit: 29,482 bytes in 377 blocks
==3026==   total heap usage: 823,269,046 allocs, 823,268,669 frees, 55,013,748,178 bytes allocated
==3026== 
==3026== LEAK SUMMARY:
==3026==    definitely lost: 0 bytes in 0 blocks
==3026==    indirectly lost: 0 bytes in 0 blocks
==3026==      possibly lost: 0 bytes in 0 blocks
==3026==    still reachable: 4,096 bytes in 1 blocks
==3026==         suppressed: 25,386 bytes in 376 blocks
==3026== Rerun with --leak-check=full to see details of leaked memory
==3026== 
==3026== For counts of detected and suppressed errors, rerun with: -v
==3026== ERROR SUMMARY: 0 errors from 0 contexts (suppressed: 0 from 0)
```

