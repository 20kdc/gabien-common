/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

import static gabien.una.UNAC.*;

/**
 * Peek/poke functions.
 * Created 25th May, 2023.
 */
public class UNAPoke {
    public static String peekString(long address) {
        return peekString(address, StandardCharsets.UTF_8);
    }

    public static String peekString(long address, Charset cs) {
        return new String(peekAB(address, (int) strlen(address)), cs);
    }

    /* Bulk Allocating Peek */

    public static boolean[] peekAZ(long addr, int length) {
        boolean[] data2 = new boolean[length];
        peekAZ(addr, length, data2, 0);
        return data2;
    }

    public static byte[] peekAB(long addr, int length) {
        byte[] data2 = new byte[length];
        peekAB(addr, length, data2, 0);
        return data2;
    }

    public static char[] peekAC(long addr, int length) {
        char[] data2 = new char[length];
        peekAC(addr, length, data2, 0);
        return data2;
    }

    public static short[] peekAS(long addr, int length) {
        short[] data2 = new short[length];
        peekAS(addr, length, data2, 0);
        return data2;
    }

    public static int[] peekAI(long addr, int length) {
        int[] data2 = new int[length];
        peekAI(addr, length, data2, 0);
        return data2;
    }

    public static long[] peekAJ(long addr, int length) {
        long[] data2 = new long[length];
        peekAJ(addr, length, data2, 0);
        return data2;
    }

    public static float[] peekAF(long addr, int length) {
        float[] data2 = new float[length];
        peekAF(addr, length, data2, 0);
        return data2;
    }

    public static double[] peekAD(long addr, int length) {
        double[] data2 = new double[length];
        peekAD(addr, length, data2, 0);
        return data2;
    }

    /* Bulk Allocating Poke */

    public static long allocAZ(boolean[] data2) { return allocAZ(data2, 0, data2.length, false); }
    public static long allocAZ(boolean[] data2, int base, int len) { return allocAZ(data2, base, len, false); }
    public static long allocAZ(boolean[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 1);
        pokeAZ(addr, len, data2, base);
        if (nt)
            pokeZ(addr + (len * 1), false);
        return addr;
    }

    public static long allocAB(byte[] data2) { return allocAB(data2, 0, data2.length, false); }
    public static long allocAB(byte[] data2, int base, int len) { return allocAB(data2, base, len, false); }
    public static long allocAB(byte[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 1);
        pokeAB(addr, len, data2, base);
        if (nt)
            pokeB(addr + (len * 1), (byte) 0);
        return addr;
    }

    public static long allocAC(char[] data2) { return allocAC(data2, 0, data2.length, false); }
    public static long allocAC(char[] data2, int base, int len) { return allocAC(data2, base, len, false); }
    public static long allocAC(char[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 2);
        pokeAC(addr, len, data2, base);
        if (nt)
            pokeC(addr + (len * 2), (char) 0);
        return addr;
    }

    public static long allocAS(short[] data2) { return allocAS(data2, 0, data2.length, false); }
    public static long allocAS(short[] data2, int base, int len) { return allocAS(data2, base, len, false); }
    public static long allocAS(short[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 2);
        pokeAS(addr, len, data2, base);
        if (nt)
            pokeS(addr + (len * 2), (short) 0);
        return addr;
    }

    public static long allocAI(int[] data2) { return allocAI(data2, 0, data2.length, false); }
    public static long allocAI(int[] data2, int base, int len) { return allocAI(data2, base, len, false); }
    public static long allocAI(int[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 4);
        pokeAI(addr, len, data2, base);
        if (nt)
            pokeI(addr + (len * 4), 0);
        return addr;
    }

    public static long allocAJ(long[] data2) { return allocAJ(data2, 0, data2.length, false); }
    public static long allocAJ(long[] data2, int base, int len) { return allocAJ(data2, base, len, false); }
    public static long allocAJ(long[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 8);
        pokeAJ(addr, len, data2, base);
        if (nt)
            pokeJ(addr + (len * 8), 0);
        return addr;
    }

    public static long allocAF(float[] data2) { return allocAF(data2, 0, data2.length, false); }
    public static long allocAF(float[] data2, int base, int len) { return allocAF(data2, base, len, false); }
    public static long allocAF(float[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 4);
        pokeAF(addr, len, data2, base);
        if (nt)
            pokeI(addr + (len * 4), 0);
        return addr;
    }

    public static long allocAD(double[] data2) { return allocAD(data2, 0, data2.length, false); }
    public static long allocAD(double[] data2, int base, int len) { return allocAD(data2, base, len, false); }
    public static long allocAD(double[] data2, int base, int len, boolean nt) {
        long addr = mallocChk((len + (nt ? 1 : 0)) * 8);
        pokeAD(addr, len, data2, base);
        if (nt)
            pokeJ(addr + (len * 8), 0);
        return addr;
    }

    /* Natives - Peek/Poke */
    public static native boolean peekZ(long addr);
    public static native byte peekB(long addr);
    public static native short peekS(long addr);
    public static native int peekI(long addr);
    public static native long peekJ(long addr);
    public static native float peekF(long addr);
    public static native double peekD(long addr);
    public static native long peekPtr(long addr);

    public static native void pokeZ(long addr, boolean data);
    public static native void pokeB(long addr, byte data);
    public static native void pokeC(long addr, char data);
    public static native void pokeS(long addr, short data);
    public static native void pokeI(long addr, int data);
    public static native void pokeJ(long addr, long data);
    public static native void pokeF(long addr, float data);
    public static native void pokeD(long addr, double data);
    public static native void pokePtr(long addr, long data);

    /* Natives - JNIEnv - peek/poke (Opposite polarity to JNIEnv functions) */
    public static native void peekAZ(long addr, long length, boolean[] array, long index);
    public static native void peekAB(long addr, long length, byte[] array, long index);
    public static native void peekAC(long addr, long length, char[] array, long index);
    public static native void peekAS(long addr, long length, short[] array, long index);
    public static native void peekAI(long addr, long length, int[] array, long index);
    public static native void peekAJ(long addr, long length, long[] array, long index);
    public static native void peekAF(long addr, long length, float[] array, long index);
    public static native void peekAD(long addr, long length, double[] array, long index);

    public static native void pokeAZ(long addr, long length, boolean[] array, long index);
    public static native void pokeAB(long addr, long length, byte[] array, long index);
    public static native void pokeAC(long addr, long length, char[] array, long index);
    public static native void pokeAS(long addr, long length, short[] array, long index);
    public static native void pokeAI(long addr, long length, int[] array, long index);
    public static native void pokeAJ(long addr, long length, long[] array, long index);
    public static native void pokeAF(long addr, long length, float[] array, long index);
    public static native void pokeAD(long addr, long length, double[] array, long index);
}
