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
public abstract class ImageRenderedTextChunk extends RenderedTextChunk {
    /**
     * These are the offset to apply to the image, not specifying the rectangle within the image.
     */
    public final int offsetX, offsetY;
    public final int measureX;

    public ImageRenderedTextChunk(int offsetX, int offsetY, int measureX, int lineHeight) {
        super(lineHeight);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.measureX = measureX;
    }

    public abstract IImage getIImage();
    public abstract IWSIImage getIWSIImage();

    @Override
    public int cursorX(int cursorXIn) {
        return cursorXIn + measureX;
    }

    @Override
    public int cursorY(int cursorYIn, int lineHeight) {
        return cursorYIn;
    }

    @Override
    public void renderTo(IGrDriver igd, int x, int y, int cX, int cY, int hlh) {
        IImage res = getIImage();
        igd.blitImage(0, 0, res.getWidth(), res.getHeight(), x + cX + offsetX, y + cY + offsetY, res);
    }

    @Override
    public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
        int margin = 1;
        int margin2 = margin * 2;
        igd.clearRectAlpha(r, g, b, a, x + cursorXIn - margin, y + cursorYIn - margin, measureX + margin2, highestLineHeight + margin2);
    }

    public static class CPU extends ImageRenderedTextChunk {
        public final IWSIImage render;
        public IImage renderUpload;

        public CPU(int offsetX, int offsetY, int measureX, int lineHeight, IWSIImage r) {
            super(offsetX, offsetY, measureX, lineHeight);
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

    public static class GPU extends ImageRenderedTextChunk {
        public final IImage render;
        public IWSIImage renderDownload;

        public GPU(int offsetX, int offsetY, int measureX, int lineHeight, IImage r) {
            super(offsetX, offsetY, measureX, lineHeight);
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
