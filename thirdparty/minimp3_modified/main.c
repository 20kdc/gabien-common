// gabien-common minimp3_modified test program
// This file was not a derivative of minimp3, but has been licensed via the same licensing as provided in COPYING.txt of this directory.

#include <stdio.h>

#define MINIMP3_IMPLEMENTATION
#include "minimp3.h"

int main(int argc, char ** argv) {
   FILE * fn, * fo;
   if (argc != 3) {
      puts("maxwell FILEIN FILEOUT");
      return 1;
   }
   fn = fopen(argv[1], "rb");
   if (!fn) {
      puts("missing file idk");
      return 1;
   }

   puts("NOT YET IMPLEMENTED");
   fclose(fn);
   return 1;
}

