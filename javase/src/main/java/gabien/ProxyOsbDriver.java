/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
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
    public IWindowGrBackend recreate(int wantedRW, int wantedRH) {
        return new ProxyOsbDriver(target.recreate(wantedRW, wantedRH));
    }
}
