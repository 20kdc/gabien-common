/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Consumer;

import gabien.GaBIEn;
import gabien.render.IGrDriver;
import gabien.render.ITexRegion;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;

/**
 * 
 * Created 18th July, 2023.
 */
public final class SimpleAtlasBuilder {
    private LinkedList<Entry> entries = new LinkedList<>();
    private final Size pageSize;
    private final IAtlasStrategy pageStrategy;

    public SimpleAtlasBuilder(int atlasW, int atlasH, IAtlasStrategy pageStrategy) {
        pageSize = new Size(atlasW, atlasH);
        this.pageStrategy = pageStrategy;
    }

    public void add(Consumer<ITexRegion> result, AtlasDrawable src) {
        entries.add(new Entry(result, src));
    }

    public AtlasSet compile() {
        // Entries are nulled as completed
        Entry[] entriesArray = entries.toArray(new Entry[0]);
        final Comparator<Size> sizeSorter = pageStrategy.getSortingAlgorithm();
        Arrays.sort(entriesArray, (a, b) -> sizeSorter.compare(a.sz, b.sz));
        int amountInArray = entriesArray.length;
        AtlasSet res = new AtlasSet();
        while (amountInArray > 0) {
            int change = compilePage(res, entriesArray);
            if (change == 0) {
                // couldn't put anything further into an atlas, give up...
                for (int i = 0; i < entriesArray.length; i++) {
                    Entry e = entriesArray[i];
                    if (e == null)
                        continue;
                    IGrDriver ap = GaBIEn.makeAtlasPage(e.sz.width, e.sz.height);
                    e.tex.drawTo(ap, 0, 0);
                    e.key.accept(ap);
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

    private int compilePage(AtlasSet res, Entry[] entriesArray) {
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
                IAtlasStrategy.Instance instance = pageStrategy.instance(pageSize);
                Rect rcI = instance.add(eI.sz);
                if (rcI == null)
                    continue;
                Rect rcJ = instance.add(eJ.sz);
                if (rcJ == null)
                    continue;
                // after this point, it's confirmed!
                entriesArray[i] = null;
                entriesArray[j] = null;
                Rect[] seedTest = {rcI, rcJ};
                return compilePageWithSeed(res, entriesArray, seedTest, eI, eJ, instance);
            }
        }
        return 0;
    }

    private int compilePageWithSeed(AtlasSet res, Entry[] entriesArray, Rect[] currentBest, Entry eI, Entry eJ, IAtlasStrategy.Instance instance) {
        Entry[] currentBestContents = {eI, eJ};
        while (true) {
            Entry[] nextContents = new Entry[currentBest.length + 1];
            Rect[] nextRects = new Rect[currentBest.length + 1];
            System.arraycopy(currentBestContents, 0, nextContents, 0, currentBest.length);
            System.arraycopy(currentBest, 0, nextRects, 0, currentBest.length);
            int foundSomething = -1;
            for (int i = 0; i < entriesArray.length; i++) {
                Entry e = entriesArray[i];
                if (e == null)
                    continue;
                // update the last entry
                nextContents[nextContents.length - 1] = e;
                // and recalculate
                Rect rct = instance.add(e.sz);
                if (rct != null) {
                    // Successfully added another element.
                    foundSomething = i;
                    nextRects[nextRects.length - 1] = rct;
                    break;
                }
            }
            if (foundSomething == -1)
                break;
            entriesArray[foundSomething] = null;
            currentBestContents = nextContents;
            currentBest = nextRects;
        }
        return finishCompilePage(res, currentBest, currentBestContents);
    }

    private int finishCompilePage(AtlasSet res, Rect[] currentBest, Entry[] currentBestContents) {
        IGrDriver ap = GaBIEn.makeAtlasPage(pageSize.width, pageSize.height);
        for (int i = 0; i < currentBest.length; i++) {
            Rect r = currentBest[i];
            Entry e = currentBestContents[i];
            e.tex.drawTo(ap, r.x, r.y);
            e.key.accept(ap.subRegion(r.x, r.y, e.sz.width, e.sz.height));
        }
        res.pages.add(ap);
        return currentBest.length;
    }

    private static class Entry {
        final Consumer<ITexRegion> key;
        final Size sz;
        final AtlasDrawable tex;
        Entry(Consumer<ITexRegion> key, AtlasDrawable tex) {
            this.key = key;
            this.tex = tex;
            this.sz = new Size(tex.width, tex.height);
        }
    }
}
