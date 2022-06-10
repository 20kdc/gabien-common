/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

/**
 * Samples, but discrete!
 * Created on 10th June 2022 as part of project WTFr7
 */
public abstract class DiscreteSample extends AudioIOCRSet {
    /**
     * Length in frames
     */
    public final int length;
    public DiscreteSample(AudioIOCRSet crs, int l) {
        super(crs);
        length = l;
    }
    public abstract void getS32(int frame, int[] buffer);
    public abstract void getF64(int frame, double[] buffer);
}
