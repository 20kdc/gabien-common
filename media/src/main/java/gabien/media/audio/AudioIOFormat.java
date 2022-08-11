/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.media.audio;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstract WAV configuration.
 * Created on 6th June 2022 as part of project VE2Bun
 */
public abstract class AudioIOFormat {
    public static final int FC_PCM = 1;
    public static final int FC_FLOAT = 3;
    public static final int FC_ALAW = 6;
    public static final int FC_ULAW = 7;

    // Requirements (used to control writing, to prevent writing stuff that might explode naive readers)
    // Requirements are modified in the writer depending on further configuration
    // "fact" chunk
    public static final int REQ_FACT = 1;
    // extension field
    public static final int REQ_EXT_SIZE = 2;
    // extension mode
    public static final int REQ_EXT_MODE = 4;

    // unsigned PCM
    // NOTE: The writer can't actually handle non-integer byte/sample ratios.
    public static final AudioIOFormat F_U8 = new PCM(false, 0, 8, 1);

    // non-linear PCM
    public static final AudioIOFormat F_AL8 = new NoConv(FC_ALAW, REQ_FACT, 8, 1);
    public static final AudioIOFormat F_UL8 = new NoConv(FC_ULAW, REQ_FACT, 8, 1);

    // signed PCM
    public static final AudioIOFormat F_S16 = new PCM(true, 0, 16, 2);
    public static final AudioIOFormat F_S24 = new PCM(true, REQ_FACT | REQ_EXT_SIZE | REQ_EXT_MODE, 24, 3);
    public static final AudioIOFormat F_S32 = new PCM(true, 0, 32, 4);

    // floating-point
    public static final AudioIOFormat F_F32 = new FP(false);
    public static final AudioIOFormat F_F64 = new FP(true);

    // actual format code (specifically never 0xFFFE)
    public final int formatCode;
    public final int requirements;
    // bits per sample (what we tell the WAV reader)
    public final int bitsPerSample;
    // bytes per sample (actual - nBlockAlign, etc.)
    public final int bytesPerSample;

    private AudioIOFormat(int fmt, int req, int bitsPS, int bytesPS) {
        formatCode = fmt;
        requirements = req;
        bitsPerSample = bitsPS;
        bytesPerSample = bytesPS;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " FC=" + formatCode + " bitsPerSample=" + bitsPerSample;
    }

    public static AudioIOFormat detect(int fmt, int sampleBits) {
        if (fmt == FC_PCM) {
            if (sampleBits == 8)
                return F_U8;
            if (sampleBits == 16)
                return F_S16;
            if (sampleBits == 24)
                return F_S24;
            if (sampleBits == 32)
                return F_S32;
        } else if (fmt == FC_FLOAT) {
            return sampleBits == 64 ? F_F64 : F_F32;
        } else if (fmt == FC_ULAW) {
            return F_UL8;
        } else if (fmt == FC_ALAW) {
            return F_AL8;
        }
        throw new UnsupportedOperationException("Unable to comprehend WAV format code " + fmt + ".");
    }

    public abstract int asS32(@NonNull ByteBuffer from, int at);
    public abstract double asF64(@NonNull ByteBuffer from, int at);
    public abstract void ofS32(@NonNull ByteBuffer from, int at, int val);
    public abstract void ofF64(@NonNull ByteBuffer from, int at, double val);

    public static class NoConv extends AudioIOFormat {
        public NoConv(int fmt, int req, int bitsPS, int bytesPS) {
            super(fmt, req, bitsPS, bytesPS);
        }
        public int asS32(@NonNull ByteBuffer from, int at) {
            throw new UnsupportedOperationException("Can't convert this format");
        }
        public double asF64(@NonNull ByteBuffer from, int at) {
            throw new UnsupportedOperationException("Can't convert this format");
        }
        public void ofS32(@NonNull ByteBuffer from, int at, int val) {
            throw new UnsupportedOperationException("Can't convert this format");
        }
        public void ofF64(@NonNull ByteBuffer from, int at, double val) {
            throw new UnsupportedOperationException("Can't convert this format");
        }
    }

    public static double cS32toF64(int valI) {
        double val = valI;
        if (val < 0) {
            return val / 2147483648d;
        } else {
            return val / 2147483647d;
        }
    }
    public static int cF64toS32(double val) {
        if (val < 0) {
            if (val < -1d)
                val = -1d;
            val *= 2147483648d;
        } else {
            if (val > 1d)
                val = 1d;
            val *= 2147483647d;
        }
        return (int) val;
    }

    public static class PCM extends AudioIOFormat {
        public final boolean signed;
        public PCM(boolean s, int req, int bitsPS, int bytesPS) {
            super(FC_PCM, req, bitsPS, bytesPS);
            signed = s;
        }
        public int asS32(@NonNull ByteBuffer from, int at) {
            int val;
            if (bytesPerSample == 1) {
                val = from.get(at) & 0xFF;
                val |= val << 8;
                val |= val << 16;
                val ^= signed ? 0x00808080 : 0x80000000;
            } else if (bytesPerSample == 2) {
                val = from.getShort(at) & 0xFFFF;
                val |= val << 16;
                val ^= signed ? 0x00008000 : 0x80000000;
            } else if (bytesPerSample == 3) {
                val = (from.getShort(at) & 0xFFFF) << 8;
                int hb = from.get(at + 2) & 0xFF;
                val |= hb << 24;
                val |= hb;
                val ^= signed ? 0x00000080 : 0x80000000;
            } else if (bytesPerSample == 4) {
                val = from.getInt(at);
                val ^= signed ? 0x00000000 : 0x80000000;
            } else {
                throw new UnsupportedOperationException("Can't convert this width");
            }
            return val;
        }
        public double asF64(@NonNull ByteBuffer from, int at) {
            return cS32toF64(asS32(from, at));
        }
        public void ofS32(@NonNull ByteBuffer from, int at, int val) {
            if (!signed)
                val ^= 0x80000000;
            if (bytesPerSample == 1) {
                from.put(at, (byte) (val >> 24));
            } else if (bytesPerSample == 2) {
                from.putShort(at, (short) (val >> 16));
            } else if (bytesPerSample == 3) {
                from.putShort(at, (short) (val >> 8));
                from.put(at + 2, (byte) (val >> 24));
            } else if (bytesPerSample == 4) {
                from.putInt(at, val);
            } else {
                throw new UnsupportedOperationException("Can't convert this width");
            }
        }
        public void ofF64(@NonNull ByteBuffer from, int at, double val) {
            ofS32(from, at, cF64toS32(val));
        }
    }

    public static class FP extends AudioIOFormat {
        public final boolean dbl;
        public FP(boolean d) {
            super(FC_FLOAT, REQ_FACT, d ? 64 : 32, d ? 8 : 4);
            dbl = d;
        }
        public int asS32(@NonNull ByteBuffer from, int at) {
            return cF64toS32(asF64(from, at));
        }
        public double asF64(@NonNull ByteBuffer from, int at) {
            return dbl ? from.getDouble(at) : from.getFloat(at);
        }
        public void ofS32(@NonNull ByteBuffer from, int at, int val) {
            ofF64(from, at, cS32toF64(val));
        }
        public void ofF64(@NonNull ByteBuffer from, int at, double val) {
            if (dbl)
                from.putDouble(at, val);
            else
                from.putFloat(at, (float) val);
        }
    }
}
