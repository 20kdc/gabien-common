/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabienapp.newsynth;

import org.eclipse.jdt.annotation.NonNull;

import gabien.media.midi.newsynth.CurvePlotter;
import gabien.media.midi.newsynth.IEditableCurveWaveform;
import gabien.media.midi.newsynth.CurvePlotter.NormalizationMode;
import gabien.render.IGrDriver;
import gabien.ui.IPointerReceiver;
import gabien.ui.UIElement;
import gabien.ui.UILayer;
import gabien.uslx.append.Size;
import gabien.wsi.IDesktopPeripherals;
import gabien.wsi.IGrInDriver;
import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.IPointer.PointerType;

/**
 * Created 2nd July, 2025
 */
public final class UINSWaveformEditor extends UIElement {
    private IEditableCurveWaveform sw;
    int selectedPoint = 0;
    float[] resolveBuffer = new float[0];
    public @NonNull Runnable onWaveformChange = () -> {};
    public boolean normalized = true;

    public UINSWaveformEditor(int width, int height, IEditableCurveWaveform sw) {
        super(width, height);
        this.sw = sw;
    }

    public void setWaveform(IEditableCurveWaveform waveform) {
        sw = waveform;
        selectedPoint = 0;
    }

    private void nextPoint() {
        selectedPoint += 1;
        selectedPoint %= sw.pointCount();
    }

    private void prevPoint() {
        selectedPoint -= 1;
        if (selectedPoint < 0)
            selectedPoint = sw.pointCount() - 1;
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (!selected)
            return;
        if (peripherals instanceof IDesktopPeripherals) {
            boolean insert = ((IDesktopPeripherals) peripherals).isKeyJustPressed(IGrInDriver.VK_INSERT);
            boolean delete = ((IDesktopPeripherals) peripherals).isKeyJustPressed(IGrInDriver.VK_DELETE);
            if (insert) {
                selectedPoint = sw.addPoint(selectedPoint);
                onWaveformChange.run();
            }
            if (delete) {
                sw.rmPoint(selectedPoint);
                prevPoint();
                onWaveformChange.run();
            }
        }
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        super.renderLayer(igd, layer);
        if (layer == UILayer.Content) {
            Size mySize = getSize();
            igd.clearRect(0, 0, 0, 0, 0, mySize.width, mySize.height);
            igd.clearRect(0, 128, 0, 0, mySize.height / 4, mySize.width, 1);
            igd.clearRect(0, 128, 0, 0, (mySize.height / 2) + (mySize.height / 4), mySize.width, 1);
            igd.clearRect(0, 128, 0, 0, mySize.height / 2, mySize.width, 1);
            igd.clearRect(0, 128, 0, mySize.width / 4, 0, 1, mySize.height);
            igd.clearRect(0, 128, 0, (mySize.width / 2) + (mySize.width / 4), 0, 1, mySize.height);
            igd.clearRect(0, 128, 0, mySize.width / 2, 0, 1, mySize.height);
            if (resolveBuffer.length != mySize.width)
                resolveBuffer = new float[mySize.width];
            /* ok now the code is just bad. but that's okay */
            CurvePlotter.resolve(sw, resolveBuffer, normalized ? NormalizationMode.RecentreMinmax : NormalizationMode.None);
            for (int i = 0; i < mySize.width; i++) {
                float v = normalized ? ((resolveBuffer[i] + 1.0f) / 2.0f) : resolveBuffer[i];
                int y = (int) (mySize.height - (v * mySize.height));
                igd.clearRect(0, 255, 0, i, y, 1, 1);
            }
            int pointCount = sw.pointCount();
            for (int j = 0; j < pointCount; j++) {
                float px = sw.pointX(j);
                float py = sw.pointY(j);
                int nsp = 255;
                if (selectedPoint == j)
                    nsp = 0;
                igd.clearRect(255, nsp, nsp, translateX(px) - 2, translateY(py) - 2, 4, 4);
            }
        }
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        if (north) {
            nextPoint();
        } else {
            prevPoint();
        }
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        if (state.getType() == PointerType.Generic) {
            return new IPointerReceiver() {
                IEditableCurveWaveform targetWaveform = sw;
                int targetPoint = selectedPoint;
                int ox, oy;
                float ofx = sw.pointX(selectedPoint);
                float ofy = sw.pointY(selectedPoint);

                @Override
                public void handlePointerBegin(IPointer state) {
                    ox = state.getX();
                    oy = state.getY();
                }

                @Override
                public void handlePointerUpdate(IPointer state) {
                    if (sw != targetWaveform)
                        return;
                    float intendedX = backTranslateX(translateX(ofx) + (state.getX() - ox));
                    float intendedY = backTranslateY(translateY(ofy) + (state.getY() - oy));
                    intendedX = Math.max(intendedX, 0);
                    intendedX = Math.min(intendedX, 1);
                    intendedY = Math.max(intendedY, 0);
                    intendedY = Math.min(intendedY, 1);
                    sw.movePoint(targetPoint, intendedX, intendedY);
                    onWaveformChange.run();
                }
                
                @Override
                public void handlePointerEnd(IPointer state) {
                    handlePointerUpdate(state);
                }
            };
        }
        return super.handleNewPointer(state);
    }

    private int translateX(float x) {
        return (int) (x * getSize().width);
    }

    private float backTranslateX(int x) {
        return x / (float) getSize().width;
    }

    private int translateY(float y) {
        int h = getSize().height;
        return (int) (h - (y * h));
    }

    private float backTranslateY(int y) {
        int h = getSize().height;
        return (y - h) / (float) -h;
    }
}