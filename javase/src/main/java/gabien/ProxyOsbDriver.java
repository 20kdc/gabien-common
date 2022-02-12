/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.backendhelp.ProxyGrDriver;

/**
 * Finalization helper.
 * Created on 08/06/17.
 */
public class ProxyOsbDriver extends ProxyGrDriver<IWindowGrBackend> implements IWindowGrBackend {
    public ProxyOsbDriver(IWindowGrBackend targ) {
        super(targ);
    }

    @Override
    public int[] getPixels() {
        return target.getPixels();
    }

    @Override
    public byte[] createPNG() {
        return target.createPNG();
    }

    @Override
    public void resize(int wantedRW, int wantedRH) {
        target.resize(wantedRW, wantedRH);
    }
}
