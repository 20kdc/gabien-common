/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

// gcc -s theory.c
// gcc -m32 -s theory.c
// riscv64-unknown-elf-gcc -S theory.c
// riscv64-unknown-elf-gcc -S theory.c
// zig cc -target aarch64-linux-android -S theory.c
// zig cc -target riscv32-linux-gnu -S theory.c
// zig cc -target x86_64-windows-gnu -S theory.c

typedef long long int64_t;

void cManyInts(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m);

void exampleOfManyInts() {
    cManyInts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
}

void cManyFloats(float a, float b, float c, float d, float e, float f, float g, float h, float i, float j, float k, float l, float m);

void exampleOfManyFloats() {
    cManyFloats(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
}

void cManyLongs(int64_t a, int64_t b, int64_t c, int64_t d, int64_t e, int64_t f, int64_t g, int64_t h, int64_t i, int64_t j, int64_t k, int64_t l, int64_t m);

void exampleOfManyLongs() {
    cManyLongs(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
}

void cIntManyLongs(int a, int64_t b, int64_t c, int64_t d, int64_t e, int64_t f, int64_t g, int64_t h, int64_t i, int64_t j, int64_t k, int64_t l, int64_t m);

void exampleOfIntManyLongs() {
    cIntManyLongs(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
}

// This attempts to catch a particularly dodgy case.
void rv32TrySplit(int a, int b, int c, int d, int e, int f, int g, float h, float i, float j, float k, float m, float n, float o, float p, double q);

void exampleRV32TrySplit() {
    rv32TrySplit(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16.789);
}

void cWeirdVA(int a, ...);

void exampleVA() {
    cWeirdVA(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
}

