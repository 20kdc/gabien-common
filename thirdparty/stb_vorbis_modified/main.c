// gabien-common stb_vorbis_modified test program
// This file was not a derivative of stb_vorbis, but has been licensed via the same licensing as provided in COPYING.txt of this directory.

#include <stdio.h>
#include <ogg/ogg.h>
#include "stb_vorbis.c"

// because this is just a test, packets are decoded into memory in advance
#define MAX_PACKET_COUNT 0x10000

ogg_packet packets[MAX_PACKET_COUNT];
int packet_count = 0;

int main(int argc, char ** argv) {
   ogg_sync_state oy;
   ogg_stream_state os;
   int has_inited_stream_yet = 0;
   FILE * fn, * fo;
   if (argc != 3) {
      puts("torres FILEIN FILEOUT");
      return 1;
   }
   fn = fopen(argv[1], "rb");
   if (!fn) {
      puts("missing file idk");
      return 1;
   }

   ogg_sync_init(&oy);
   int eof = 0;
   while (1) {
      ogg_page og = {};
      while (ogg_sync_pageout(&oy, &og) != 1) {
         char * dat = ogg_sync_buffer(&oy, 8192);
         size_t res = fread(dat, 1, 8192, fn);
         // nothing coming?
         if (!res) {
            eof = 1;
            break;
         }
         ogg_sync_wrote(&oy, res);
      }
      if (eof)
         break;
      // valid page, deal with it
      if (!has_inited_stream_yet) {
         ogg_stream_init(&os, ogg_page_serialno(&og));
         has_inited_stream_yet = 1;
      }
      // feed page to stream
      ogg_stream_pagein(&os, &og);
      // packets, please
      while (1) {
         if (packet_count >= MAX_PACKET_COUNT) {
            // accepting any more packets at this time is not allowed
            break;
         }
         int pkterr = ogg_stream_packetout(&os, packets + packet_count);
         if (pkterr == 0) {
            // not enough data or w/e
            break;
         } else if (pkterr == 1) {
            // got packet
            // copy data, because libogg's like that I guess
            unsigned char * m = malloc(packets[packet_count].bytes);
            memcpy(m, packets[packet_count].packet, packets[packet_count].bytes);
            packets[packet_count].packet = m;
            packet_count++;
         }
      }
   }
   fclose(fn);
   printf(" - %i packets available... -\n", packet_count);
   if (packet_count < 3) {
      // which isn't enough
      return 1;
   }
   // printf("1st byte: %i %i %i %i\n", packets[0].packet[0], packets[0].packet[1], packets[0].packet[2], packets[0].packet[3]);
   int errid = 0;
   stb_vorbis_g * vbg = stb_vorbis_g_open(packets[0].packet, packets[0].bytes, packets[2].packet, packets[2].bytes, &errid);
   if (!vbg) {
      printf("stb_vorbis_g error %i!\n", errid);
   }

   unsigned int sample_rate = stb_vorbis_g_get_sample_rate(vbg);
   int channels = stb_vorbis_g_get_channels(vbg);
   printf("%ihz %ich // play -r %i -e float -b 32 -c %i %s\n", sample_rate, channels, sample_rate, channels, argv[2]);

   fo = fopen(argv[2], "wb");
   if (!fo)
      puts("unable to open output file");
   for (int i = 3; i < packet_count; i++) {
      float ** output = NULL;
      int samples = stb_vorbis_g_decode_frame(vbg, packets[i].packet, packets[i].bytes, &output);
      for (int j = 0; j < samples; j++) {
         for (int k = 0; k < channels; k++) {
            fwrite(&output[k][j], 1, 4, fo);
         }
      }
   }
   fclose(fo);
   return 0;
}

