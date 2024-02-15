/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.util.Random;

import gabien.media.midi.MIDISynthesizer.Channel;

/**
 * "Eclipse Debug Editing and its applications for MIDI Synthesis"
 * Created February 14th, 2024.
 */
public enum DefaultMIDIPalette implements MIDISynthesizer.Palette {
    INSTANCE;

    @Override
    public Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity) {
        // if (program < 0x30 || program > 0x50 || bank != 0) return null;
        // System.out.println(bank + ":" + Integer.toHexString(program) + ":" + note);
        if (bank >= 128) {
            // stick
            if (note == 28 || note == 31 || note == 39 || note == 40 || (note >= 60 && note <= 66) || note == 70)
                return new CrashChannel(new Random(bank + program + note + velocity), 0.05d, 0.3d, false);
            // shake
            if (note == 42 || note == 44 || note == 46 || note == 51 || note == 52 || note == 59 || note == 69)
                return new CrashChannel(new Random(bank + program + note + velocity), 0.05d, 0.2d, true);
            // big crash
            if (note == 49 || note == 55 || note == 57)
                return new CrashChannel(new Random(bank + program + note + velocity), 1.0d, 0.2d, false);
            //                     A      D      S      R      M      V      PMF               PMT   PMD    PL    MST
            return new MLDIChannel(0.01f, 0,     1,     0.10f, 1.00f, 0.30f, 50 + (note * 2),  25,   1,     true, 0);
        }
        // parameters for the synthesizer!
        //           vwPlADSR
        int sCFG = 0x00000000;
        switch (program) {
        //                  vwPlADSR
        case 0x00: sCFG = 0xC1100882; break; // piano ACG
        case 0x01: sCFG = 0xC1100882; break; // piano ACG-B
        case 0x02: sCFG = 0x62100882; break; // piano EGP
        case 0x03: sCFG = 0x62100882; break; // piano HTP
        case 0x04: sCFG = 0x62100882; break; // piano RHO
        case 0x05: sCFG = 0x62100882; break; // piano CHO
        case 0x06: sCFG = 0x62100882; break; // piano HAR
        case 0x07: sCFG = 0x62100882; break; // piano CLA
        //                  vwPlADSR
        case 0x08: sCFG = 0x81000821; break; // chrpk CEL
        case 0x09: sCFG = 0x81000821; break; // chrpk GLO
        case 0x0A: sCFG = 0x81000821; break; // chrpk MBX
        case 0x0B: sCFG = 0x81000821; break; // chrpk VIB
        case 0x0C: sCFG = 0x81000821; break; // chrpk MAR
        case 0x0D: sCFG = 0x81000821; break; // chrpk XYL
        case 0x0E: sCFG = 0x81000821; break; // chrpk TUB
        case 0x0F: sCFG = 0x81000821; break; // chrpk DUL
        //                  vwPlADSR
        // these use w=1 for an organ sound
        case 0x10: sCFG = 0x81001FC2; break; // organ DRW
        case 0x11: sCFG = 0x81001FC2; break; // organ PER
        case 0x12: sCFG = 0x81001FC2; break; // organ ROC
        case 0x13: sCFG = 0x81001FC2; break; // organ CHR
        case 0x14: sCFG = 0x81001FC2; break; // organ REE
        // these three use w=2 for that harmonica sound
        case 0x15: sCFG = 0x42001FC2; break; // organ ACC
        case 0x16: sCFG = 0x42001FC2; break; // organ HAR
        case 0x17: sCFG = 0x42001FC2; break; // organ BAN
        //                  vwPlADSR
        case 0x18: sCFG = 0xC1230844; break; // guitr
        case 0x19: sCFG = 0xC1230844; break; // guitr
        case 0x1A: sCFG = 0xC1230844; break; // guitr
        // note change over to 22 for electric sounds
        case 0x1B: sCFG = 0x42230844; break; // guitr
        case 0x1C: sCFG = 0x42230844; break; // guitr
        case 0x1D: sCFG = 0x42230844; break; // guitr
        case 0x1E: sCFG = 0x42230844; break; // guitr
        case 0x1F: sCFG = 0x42230844; break; // guitr
        //                  vwPlADSR
        case 0x20: sCFG = 0x44100881; break; // bass!
        case 0x21: sCFG = 0x44100881; break; // bass!
        case 0x22: sCFG = 0x44100881; break; // bass!
        case 0x23: sCFG = 0x44100881; break; // bass!
        case 0x24: sCFG = 0x44100881; break; // bass!
        case 0x25: sCFG = 0x44100881; break; // bass!
        case 0x26: sCFG = 0x44100881; break; // bass!
        case 0x27: sCFG = 0x44100881; break; // bass!
        //                  vwPlADSR
        // stringsish use long attack/release times
        case 0x28: sCFG = 0x42002882; break; // orchs
        case 0x29: sCFG = 0x42002882; break; // orchs
        case 0x2A: sCFG = 0x42002882; break; // orchs
        case 0x2B: sCFG = 0x42002882; break; // orchs
        case 0x2C: sCFG = 0x42002882; break; // orchs
        case 0x2D: sCFG = 0x42002882; break; // orchs
        // harpish
        case 0x2E: sCFG = 0x81000C42; break; // orchs
        case 0x2F: sCFG = 0x81000C42; break; // orchs
        //                  vwPlADSR
        case 0x30: sCFG = 0x42002882; break; // orche
        case 0x31: sCFG = 0x42002882; break; // orche
        case 0x32: sCFG = 0x42002882; break; // orche
        case 0x33: sCFG = 0x42002882; break; // orche
        // voice
        case 0x34: sCFG = 0x81003883; break; // orche
        case 0x35: sCFG = 0x81003883; break; // orche
        case 0x36: sCFG = 0x81003883; break; // orche
        // hit
        case 0x37: sCFG = 0x32000F0F; break; // orche
        //                  vwPlADSR
        case 0x38: sCFG = 0x421014C4; break; // brass
        case 0x39: sCFG = 0x421014C4; break; // brass
        case 0x3A: sCFG = 0x421014C4; break; // brass
        case 0x3B: sCFG = 0x421014C4; break; // brass
        case 0x3C: sCFG = 0x421014C4; break; // brass
        case 0x3D: sCFG = 0x421014C4; break; // brass
        case 0x3E: sCFG = 0x421014C4; break; // brass
        case 0x3F: sCFG = 0x421014C4; break; // brass
        //                  vwPlADSR
        case 0x40: sCFG = 0x421014C4; break; // reed!
        case 0x41: sCFG = 0x421014C4; break; // reed!
        case 0x42: sCFG = 0x421014C4; break; // reed!
        case 0x43: sCFG = 0x421014C4; break; // reed!
        case 0x44: sCFG = 0x421014C4; break; // reed!
        case 0x45: sCFG = 0x421014C4; break; // reed!
        case 0x46: sCFG = 0x421014C4; break; // reed!
        case 0x47: sCFG = 0x421014C4; break; // reed!
        //                  vwPlADSR
        case 0x48: sCFG = 0xC1002FC2; break; // wind!
        case 0x49: sCFG = 0xC1002FC2; break; // wind!
        case 0x4A: sCFG = 0xC1002FC2; break; // wind!
        case 0x4B: sCFG = 0xC1002FC2; break; // wind!
        case 0x4C: sCFG = 0xC1002FC2; break; // wind!
        case 0x4D: sCFG = 0xC1002FC2; break; // wind!
        case 0x4E: sCFG = 0xC1002FC2; break; // wind!
        case 0x4F: sCFG = 0xC1002FC2; break; // wind!
        //                  vwPlADSR
        case 0x50: sCFG = 0x53100842; break; // synth
        case 0x51: sCFG = 0x53100842; break; // synth
        case 0x52: sCFG = 0x53100842; break; // synth
        case 0x53: sCFG = 0x53100842; break; // synth
        case 0x54: sCFG = 0x53100842; break; // synth
        case 0x55: sCFG = 0x53100842; break; // synth
        case 0x56: sCFG = 0x53100842; break; // synth
        case 0x57: sCFG = 0x53100842; break; // synth
        //                  vwPlADSR
        case 0x58: sCFG = 0x22100F82; break; // sypad
        case 0x59: sCFG = 0x22100F82; break; // sypad
        case 0x5A: sCFG = 0x22100F82; break; // sypad
        case 0x5B: sCFG = 0x22100F82; break; // sypad
        case 0x5C: sCFG = 0x22100F82; break; // sypad
        case 0x5D: sCFG = 0x22100F82; break; // sypad
        case 0x5E: sCFG = 0x22100F82; break; // sypad
        case 0x5F: sCFG = 0x22100F82; break; // sypad
        //                  vwPlADSR
        case 0x60: sCFG = 0x81100841; break; // sysfx
        case 0x61: sCFG = 0x81100841; break; // sysfx
        case 0x62: sCFG = 0x81100841; break; // sysfx
        case 0x63: sCFG = 0x81100841; break; // sysfx
        case 0x64: sCFG = 0x81100841; break; // sysfx
        case 0x65: sCFG = 0x81100841; break; // sysfx
        case 0x66: sCFG = 0x81100841; break; // sysfx
        case 0x67: sCFG = 0x81100841; break; // sysfx
        //                  vwPlADSR
        case 0x68: sCFG = 0x81230844; break; // appro
        case 0x69: sCFG = 0x81230844; break; // appro
        case 0x6A: sCFG = 0x81230844; break; // appro
        case 0x6B: sCFG = 0x81230844; break; // appro
        case 0x6C: sCFG = 0x81230844; break; // appro
        case 0x6D: sCFG = 0x81230844; break; // appro
        case 0x6E: sCFG = 0x81230844; break; // appro
        case 0x6F: sCFG = 0x81230844; break; // appro
        //                  vwPlADSR
        case 0x70: sCFG = 0x81230844; break; // percs
        case 0x71: sCFG = 0x81230844; break; // percs
        case 0x72: sCFG = 0x81230844; break; // percs
        case 0x73: sCFG = 0x81230844; break; // percs
        case 0x74: sCFG = 0x81230844; break; // percs
        case 0x75: sCFG = 0x81230844; break; // percs
        case 0x76: sCFG = 0x81230844; break; // percs
        case 0x77: sCFG = 0x81230844; break; // percs
        //                  vwPlADSR
        case 0x78: sCFG = 0x81230844; break; // sndfx
        case 0x79: sCFG = 0x81230844; break; // sndfx
        case 0x7A: sCFG = 0x81230844; break; // sndfx
        case 0x7B: sCFG = 0x81230844; break; // sndfx
        case 0x7C: sCFG = 0x81230844; break; // sndfx
        case 0x7D: sCFG = 0x81230844; break; // sndfx
        case 0x7E: sCFG = 0x81230844; break; // sndfx
        case 0x7F: sCFG = 0x81230844; break; // sndfx
        }
        // Waveform
        float pMix;
        switch ((sCFG >> 24) & 0xF) {
        case 0: // Sine
            pMix = 0.00f;
            break;
        case 1: // Sineish (but only slightly otherwise it sounds too close to 2)
            pMix = 0.10f;
            break;
        case 2: // Squarish
            pMix = 0.80f;
            break;
        case 3: // Square
        default:
            pMix = 1.00f;
            break;
        case 4: // Bass
            pMix = 0.70f;
            break;
        }
        float pVol = ((sCFG >> 28) & 0xF) / 15.0f;
        // A/D/S/R
        float pA = ((sCFG >> 12) & 0xF) / 31.0f;
        float pD = ((sCFG >> 8) & 0xF) / 15.0f;
        float pS = ((sCFG >> 4) & 0xF) / 15.0f;
        float pR = (sCFG & 0xF) / 15.0f;
        // PITCH
        float pPMF, pPMT;
        switch ((sCFG >> 20) & 0xF) {
        case 0: // none/default
        default:
            pPMF = 1;
            pPMT = 1;
            break;
        case 1: // Synth drop
            pPMF = 1.1f;
            pPMT = 0.99f;
            break;
        case 2: // Guitar drop
            pPMF = 1.01f;
            pPMT = 0.99f;
            break;
        }
        float pPMD = (((sCFG >> 16) & 0xF) + 1) / 15f;
        // FINALE
        //                     A   D   S   R   M     V     PMF   PMT   PMD
        return new MLDIChannel(pA, pD, pS, pR, pMix, pVol, pPMF, pPMT, pPMD);
    }
}
