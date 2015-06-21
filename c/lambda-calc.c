/* gcc -Wall -Werror -Wimplicit -pedantic -std=c99 -fsyntax-only lambda-calc.c */

/* gcc -Wall -Werror -Wimplicit -pedantic -std=c99 -O3 lambda-calc.c -o /tmp/lc */

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

/*
 * forward decls
 */

struct atom_vtbl_t;
struct atom_t;

/* mem management forward decl */
static void* mem_alloc(int size);

/*
 * types
 */

struct atom_vtbl_t {
  int                 (* to_int)(void* self);
  struct atom_t*      (* f1)(void* self, struct atom_t* arg);
};

struct atom_t {
  struct atom_vtbl_t* vtbl;
};



/*
 * virtual methods
 */

static int notimpl_to_int(void* self) {
  fputs(";; [ERROR] not implemented: to_int()\n", stderr);
  abort();
  return -1; /* should not come here */
}

static struct atom_t* notimpl_f1(void* self, struct atom_t* arg) {
  fputs(";; [ERROR] not implemented: f1(atom)\n", stderr);
  abort();
  return arg; /* should not come here */
}

#define DEFINE_INTFN_VTBL(vtbl_name, to_int_fn_name) \
  static struct atom_vtbl_t vtbl_name = { \
    .to_int = to_int_fn_name, \
    .f1 = notimpl_f1 \
  }

#define DEFINE_F1_VTBL(vtbl_name, f1_fn_name) \
  static struct atom_vtbl_t vtbl_name = { \
    .to_int = notimpl_to_int, \
    .f1 = f1_fn_name \
  }

#define F1(lhs, arg) (lhs)->vtbl->f1((lhs), (arg))

/*
 * classes
 */


/* Zero.Z: (% z z) */

static struct atom_t* lambda_z_f1(void* self, struct atom_t* z) {
  return z;
}
DEFINE_F1_VTBL(g_lambda_z_vtbl, lambda_z_f1);
static struct atom_t ZERO_LAMBDA_Z = {
  .vtbl = &g_lambda_z_vtbl
};

/* Zero: (def zero (% s (% z z))) */

static struct atom_t* zero_s_f1(void* self, struct atom_t* ignored) {
  return &ZERO_LAMBDA_Z;
}
DEFINE_F1_VTBL(g_zero_vtbl, zero_s_f1);
static struct atom_t ZERO = {
  .vtbl = &g_zero_vtbl
};

/* Succ: (def succ (% n (% s (% z s ((n s) z))))) */

/* inner function: (% z s ((n s) z)) */
struct succ_fn_z_atom_t {
  struct atom_vtbl_t* vtbl;
  struct atom_t* n;
  struct atom_t* s;
};
static struct atom_t* succ_fn_z_f1(void* self, struct atom_t* z) {
  struct succ_fn_z_atom_t* this = (struct succ_fn_z_atom_t*) self;
  struct atom_t* s = this->s;
  struct atom_t* n = this->n;

  struct atom_t* ns = F1(n, s);     /* (n s) */
  struct atom_t* nsz = F1(ns, z);   /* ((n s) z) */
  return F1(s, nsz);                /* (s ((n s) z)) */
}
DEFINE_F1_VTBL(g_succ_fn_z_atom_vtbl_t, succ_fn_z_f1);
static struct atom_t* new_succ_fn_z(struct atom_t* n, struct atom_t* s) {
  struct succ_fn_z_atom_t* result = mem_alloc(sizeof(struct succ_fn_z_atom_t));
  result->vtbl = &g_succ_fn_z_atom_vtbl_t;
  result->n = n;
  result->s = s;
  return (struct atom_t*) result;
}

/* inner function: (% s (% z s ((n s) z))) */
struct succ_fn_s_atom_t {
  struct atom_vtbl_t* vtbl;
  struct atom_t* n;
};
static struct atom_t* succ_fn_s_f1(void* self, struct atom_t* s) {
  struct succ_fn_s_atom_t* this = (struct succ_fn_s_atom_t*) self;
  struct atom_t* n = this->n;
  return new_succ_fn_z(n, s);
}
DEFINE_F1_VTBL(g_succ_fn_s_atom_vtbl_t, succ_fn_s_f1);
static struct atom_t* new_succ_fn_s(struct atom_t* n) {
  struct succ_fn_s_atom_t* result = mem_alloc(sizeof(struct succ_fn_s_atom_t));
  result->vtbl = &g_succ_fn_s_atom_vtbl_t;
  result->n = n;
  return (struct atom_t*) result;
}

/* (def succ (% n (% s (% z s ((n s) z))))) */
static struct atom_t* succ_f1(void* self, struct atom_t* n) {
  return new_succ_fn_s(n);
}
DEFINE_F1_VTBL(g_succ_vtbl, succ_f1);
static struct atom_t SUCC = {
  .vtbl = &g_succ_vtbl
};

/* Int */

struct int_atom_t {
  struct atom_vtbl_t* vtbl;
  int value;
};
static int intatom_to_int(void* self) {
  struct int_atom_t* a = (struct int_atom_t*) self;
  return a->value;
}
DEFINE_INTFN_VTBL(g_int_vtbl, intatom_to_int);
static struct atom_t* new_int(int value) {
  struct int_atom_t* a = mem_alloc(sizeof(struct int_atom_t));
  a->vtbl = &g_int_vtbl;
  a->value = value;
  return (struct atom_t*) a;
}

/* Inc */

static struct atom_t* inc_f1(void* self, struct atom_t* num) {
  return new_int(num->vtbl->to_int(num) + 1);
}
DEFINE_F1_VTBL(g_inc_vtbl, inc_f1);
static struct atom_t INC = {
  .vtbl = &g_inc_vtbl
};

/*
 * mem management
 */

struct mem_alloc_block_t {
  char* buf;
  int size;
  int capacity;
  struct mem_alloc_block_t* next;
};

#define MEM_ALLOC_BLOCK_CAPACITY (2048*1000)

struct mem_alloc_block_t* g_heap = NULL;
int g_mem_alloc_blocks_allocated = 0;

void* xmalloc(int size) {
  assert(size > 0);

  void* m = malloc(size);
  if (m == NULL) {
    fputs(";; [FATAL] out of memory\n", stderr);
  }
  return m;
}

struct mem_alloc_block_t* new_block(struct mem_alloc_block_t* next) {
  int capacity = MEM_ALLOC_BLOCK_CAPACITY;
  struct mem_alloc_block_t* result = xmalloc(sizeof(struct mem_alloc_block_t) + capacity);
  result->size = 0;
  result->capacity = capacity;
  result->buf = ((char*) result) + sizeof(struct mem_alloc_block_t);
  result->next = next;

  ++g_mem_alloc_blocks_allocated;
  return result;
}

static void* mem_alloc(int size) {
  assert(size > 0);

  if (g_heap == NULL) {
    g_heap = new_block(NULL);
  }

  for (;;) {
    if (size > g_heap->capacity) {
      fputs(";; [FATAL] can't alloc object: size is too big\n", stderr);
    }

    int new_size = g_heap->size + size;
    if (new_size < g_heap->capacity) {
      void* p = g_heap->buf + g_heap->size;
      g_heap->size = new_size;
      return p;
    }

    g_heap = new_block(g_heap);
  }
}

/*
 * entry point
 */

static int to_int(struct atom_t* a) {
  return a->vtbl->to_int(a);
}

#define N (10)

int main(int argc, const char** argv) {
  struct atom_t* zero = &ZERO;
  struct atom_t* succ = &SUCC;
  struct atom_t* inc = &INC;

  struct atom_t* n[N];
  for (int i = 0; i < N; ++i) {
    if (i == 0) {
      n[0] = zero;
    } else {
      n[i] = F1(succ, n[i - 1]);
    }
  }

  /* convert to int */
  for (int i = 0; i < N; ++i) {
    int num = to_int(F1(F1(n[i], inc), new_int(0)));
    fprintf(stdout, "(num#%d inc 0) = %d\n", i, num);
  }

  return 0;
}
