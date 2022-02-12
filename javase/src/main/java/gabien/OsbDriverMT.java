/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.backendhelp.ThreadForwardingGrDriver;

/**
 * Created on 08/06/17.
 */
public class OsbDriverMT extends ThreadForwardingGrDriver<OsbDriverCore> implements IWindowGrBackend {
    public OsbDriverMT(int w, int h, boolean alpha) {
        super(new OsbDriverCore(w, h, alpha));
        clientWidth = w;
        clientHeight = h;
    }

    @Override
    public void resize(int wantedRW, int wantedRH) {
        Runnable r = flushCmdBufAndLock();
        target.resize(wantedRW, wantedRH);
        r.run();
    }
}
