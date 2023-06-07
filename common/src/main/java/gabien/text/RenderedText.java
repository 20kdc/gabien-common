/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.IWSIImage;

/**
 * Font rendering is the big trouble-causer.
 * This encapsulates the output of font rendering.
 * Created 7th June 2023.
 */
public abstract class RenderedText {
    /**
     * These are the offset to apply to the image, not specifying the rectangle within the image.
     */
    public final int offsetX, offsetY;
    public final int measureX;

    public RenderedText(int offsetX, int offsetY, int measureX) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.measureX = measureX;
    }

    public abstract IImage getIImage();
    public abstract IWSIImage getIWSIImage();

    public void renderTo(IGrDriver igd, int x, int y) {
        IImage res = getIImage();
        igd.blitImage(0, 0, res.getWidth(), res.getHeight(), x + offsetX, y + offsetY, res);
    }

    public static class CPU extends RenderedText {
        public final IWSIImage render;
        public IImage renderUpload;

        public CPU(int offsetX, int offsetY, int measureX, IWSIImage r) {
            super(offsetX, offsetY, measureX);
            render = r;
        }

        @Override
        public IImage getIImage() {
            synchronized (this) {
                if (renderUpload == null)
                    renderUpload = render.upload();
                return renderUpload;
            }
        }

        @Override
        public IWSIImage getIWSIImage() {
            return render;
        }
    }

    public static class GPU extends RenderedText {
        public final IImage render;
        public IWSIImage renderDownload;

        public GPU(int offsetX, int offsetY, int measureX, IImage r) {
            super(offsetX, offsetY, measureX);
            render = r;
        }

        @Override
        public IImage getIImage() {
            return render;
        }

        @Override
        public IWSIImage getIWSIImage() {
            synchronized (this) {
                if (renderDownload == null)
                    renderDownload = render.download();
                return renderDownload;
            }
        }
    }
}
