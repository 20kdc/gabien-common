/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

import java.util.LinkedList;

import gabien.GaBIEn;
import gabien.render.AtlasPage;
import gabien.ui.Rect;
import gabien.ui.Size;

/**
 * 
 * Created 18th July, 2023.
 */
public final class SimpleAtlasBuilder<K> {
    private LinkedList<Entry> entries = new LinkedList<>();
    private final Size pageSize;
    private final IAtlasStrategy pageStrategy;

    public SimpleAtlasBuilder(int atlasW, int atlasH, IAtlasStrategy pageStrategy) {
        pageSize = new Size(atlasW, atlasH);
        this.pageStrategy = pageStrategy;
    }

    public void add(K key, AtlasDrawable src) {
        entries.add(new Entry(key, src));
    }

    @SuppressWarnings("unchecked")
    public AtlasSet<K> compile() {
        // Entries are nulled as completed
        Entry[] entriesArray = entries.toArray(new Entry[0]);
        int amountInArray = entriesArray.length;
        AtlasSet<K> res = new AtlasSet<>();
        while (amountInArray > 0) {
            int change = compilePage(res, entriesArray);
            if (change == 0) {
                // couldn't put anything further into an atlas, give up...
                for (int i = 0; i < entriesArray.length; i++) {
                    Entry e = entriesArray[i];
                    AtlasPage ap = GaBIEn.makeAtlasPage(e.sz.width, e.sz.height);
                    e.tex.drawTo(ap, 0, 0);
                    res.contents.put((K) e.key, ap);
                    res.pages.add(ap);
                    entriesArray[i] = null;
                    amountInArray--;
                }
            } else {
                amountInArray -= change;
            }
        }
        return res;
    }

    private int compilePage(AtlasSet<K> res, Entry[] entriesArray) {
        // figure out "seed" of the page
        for (int i = 0; i < entriesArray.length; i++) {
            Entry eI = entriesArray[i];
            if (eI == null)
                continue;
            for (int j = i + 1; j < entriesArray.length; j++) {
                Entry eJ = entriesArray[j];
                if (eJ == null)
                    continue;
                // "page seed" : eI + eJ
                Rect[] seedTest = pageStrategy.calculate(pageSize, new Size[] {
                    eI.sz,
                    eJ.sz
                });
                if (seedTest[0] == null || seedTest[1] == null)
                    continue;
                // after this point, it's confirmed!
                entriesArray[i] = null;
                entriesArray[j] = null;
                return compilePageWithSeed(res, entriesArray, seedTest, eI, eJ);
            }
        }
        return 0;
    }

    private int compilePageWithSeed(AtlasSet<K> res, Entry[] entriesArray, Rect[] currentBest, Entry eI, Entry eJ) {
        Entry[] currentBestContents = {eI, eJ};
        Size[] currentSizes = {eI.sz, eJ.sz};
        while (true) {
            Entry[] nextContents = new Entry[currentBest.length + 1];
            Size[] nextSizes = new Size[currentBest.length + 1];
            System.arraycopy(currentBestContents, 0, nextContents, 0, currentBest.length);
            System.arraycopy(currentSizes, 0, nextSizes, 0, currentBest.length);
            int foundSomething = -1;
            Rect[] nextRects = null;
            for (int i = 0; i < entriesArray.length; i++) {
                Entry e = entriesArray[i];
                if (e == null)
                    continue;
                // update the last entry
                nextContents[nextContents.length - 1] = e;
                nextSizes[nextContents.length - 1] = e.sz;
                // and recalculate
                nextRects = pageStrategy.calculate(pageSize, nextSizes);
                // results?
                boolean ok = true;
                for (int j = 0; j < nextRects.length; j++) {
                    if (nextRects[j] == null) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    // Successfully added another element.
                    foundSomething = i;
                    break;
                }
            }
            if (foundSomething == -1)
                break;
            entriesArray[foundSomething] = null;
            currentBestContents = nextContents;
            currentBest = nextRects;
            currentSizes = nextSizes;
        }
        return finishCompilePage(res, currentBest, currentBestContents);
    }

    @SuppressWarnings("unchecked")
    private int finishCompilePage(AtlasSet<K> res, Rect[] currentBest, Entry[] currentBestContents) {
        AtlasPage ap = GaBIEn.makeAtlasPage(pageSize.width, pageSize.height);
        for (int i = 0; i < currentBest.length; i++) {
            Rect r = currentBest[i];
            Entry e = currentBestContents[i];
            e.tex.drawTo(ap, r.x, r.y);
            res.contents.put((K) e.key, ap.subRegion(r.x, r.y, e.sz.width, e.sz.height));
        }
        res.pages.add(ap);
        return currentBest.length;
    }

    private static class Entry {
        final Object key;
        final Size sz;
        final AtlasDrawable tex;
        Entry(Object key, AtlasDrawable tex) {
            this.key = key;
            this.tex = tex;
            this.sz = new Size(tex.width, tex.height);
        }
    }
}
