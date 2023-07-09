/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.IReplicatedTexRegion;
import gabien.render.WSIImage;

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

    public abstract IReplicatedTexRegion getIImage();
    public abstract WSIImage getIWSIImage();

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
        IReplicatedTexRegion res = getIImage();
        igd.blitImage(x + cX + offsetX, y + cY + offsetY, res);
    }

    public static void background(IGrDriver igd, int x, int y, int w, int h, int margin, int r, int g, int b, int a) {
        int margin2 = margin * 2;
        igd.fillRect(r, g, b, a, x - margin, y - margin, w + margin2, h + margin2);
    }

    @Override
    public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
        background(igd, x + cursorXIn, y + cursorYIn, measureX, highestLineHeight, 1, r, g, b, a);
    }

    public static class WSI extends ImageRenderedTextChunk {
        public final WSIImage render;
        private IImage renderUpload;

        public WSI(int offsetX, int offsetY, int measureX, int lineHeight, WSIImage r) {
            super(offsetX, offsetY, measureX, lineHeight);
            render = r;
        }

        @Override
        public IImage getIImage() {
            synchronized (this) {
                if (renderUpload == null)
                    renderUpload = render.upload("Text");
                return renderUpload;
            }
        }

        @Override
        public WSIImage getIWSIImage() {
            return render;
        }
    }

    public static class GPU extends ImageRenderedTextChunk {
        public final IImage render;
        private WSIImage renderDownload;

        public GPU(int offsetX, int offsetY, int measureX, int lineHeight, IImage r) {
            super(offsetX, offsetY, measureX, lineHeight);
            render = r;
        }

        @Override
        public IImage getIImage() {
            return render;
        }

        @Override
        public WSIImage getIWSIImage() {
            synchronized (this) {
                if (renderDownload == null)
                    renderDownload = render.download();
                return renderDownload;
            }
        }
    }
}
