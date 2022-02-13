/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

import gabien.GaBIEn;
import gabien.ui.*;
import gabien.uslx.append.*;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Forgot when this was made initially, but now, February 17th, 2018:
 * With this working, with text being wrapped in complex layouts, I saw everything working exactly as I had planned.
 * I'm going ahead with changing things to this.
 * -- Note: This later (at first appearance in gabien-android) became essentially a backend support class for when native file browsers aren't available.
 * Imported into gabien-common on 04/03/2020.
 */
public class UIFileBrowser extends UIElement.UIProxy {
    private boolean done;
    private IConsumer<String> run;
    private UILabel upperSection;
    private UIScrollLayout basicLayout;
    private UISplitterLayout outerLayout;
    private UIPublicPanel lowerSection;
    private UIElement lowerSectionContents;
    private int fontSize;
    private LinkedList<String> pathComponents;
    private String strAccept, strTP;

    public UIFileBrowser(String actPath, IConsumer<String> r, String titlePrefix, String accept, int fSize, int scrollerSize) {
        run = r;
        strTP = titlePrefix;
        strAccept = accept;
        pathComponents = UIFileBrowser.createComponents(actPath);
        basicLayout = new UIScrollLayout(true, scrollerSize);
        upperSection = new UILabel("!", fSize);
        lowerSection = new UIPublicPanel(1, 1) {
            @Override
            public void runLayout() {
                for (UIElement uie : layoutGetElements())
                    layoutRemoveElement(uie);
                if (lowerSectionContents != null) {
                    layoutAddElement(lowerSectionContents);
                    lowerSectionContents.setForcedBounds(this, new Rect(getSize()));
                    setWantedSize(lowerSectionContents.getWantedSize());
                } else {
                    setWantedSize(new Size(0, 0));
                }
            }
        };
        fontSize = fSize;
        outerLayout = new UISplitterLayout(upperSection, new UISplitterLayout(basicLayout, lowerSection, true, 1), true, 0);
        rebuild();
        outerLayout.setForcedBounds(null, new Rect(outerLayout.getWantedSize()));
        outerLayout.runLayout();
        proxySetElement(outerLayout, true);
    }

    public static LinkedList<String> createComponents(String browserDirectory) {
        LinkedList<String> lls = new LinkedList<String>();
        try {
            browserDirectory = browserDirectory.replace('\\', '/');
            String[] old = browserDirectory.split("/");
            for (String s : old) {
                if (s.equals(""))
                    continue;
                if (s.equals("."))
                    continue;
                lls.add(s);
            }
        } catch (Exception e) {
            // Keep working despite resolve errors
            e.printStackTrace();
        }
        return lls;
    }

    // Should we show a given item, by postfix? (Point for future expansion)
    protected boolean shouldShow(boolean dir, String name) {
        return true;
    }

    // Translates the current path components (and possibly the 'last' component, which might be null) into a system path.
    private String getPath() {
        StringBuilder text = new StringBuilder("/");
        boolean first = true;
        for (String s : pathComponents) {
            if (!first)
                text.append('/');
            first = false;
            text.append(s);
        }
        return text.toString();
    }

    private void rebuild() {
        basicLayout.panelsClear();

        if (lowerSectionContents != null)
            lowerSectionContents = null;

        boolean showManualControl = true;
        final String exact = getPath();
        upperSection.text = exact;
        String[] paths = GaBIEn.listEntries(exact);
        basicLayout.panelsAdd(new UITextButton("<-", fontSize, new Runnable() {
                @Override
                public void run() {
                    if (!done) {
                        if (pathComponents.size() == 0) {
                            done = true;
                            run.accept(null);
                            return;
                        }
                        pathComponents.removeLast();
                        rebuild();
                    }
                }
        }));
        if (paths != null) {
            LinkedList<String> dirs = new LinkedList<String>();
            LinkedList<String> fils = new LinkedList<String>();
            for (final String s : paths) {
                if (s.contains("\\"))
                    throw new RuntimeException("Backend Error (\\)");
                if (s.contains("/"))
                    throw new RuntimeException("Backend Error (/)");
                if (GaBIEn.dirExists(exact + "/" + s)) {
                    dirs.add(s);
                } else {
                    fils.add(s);
                }
            }
            Collections.sort(dirs);
            Collections.sort(fils);
            for (final String s : dirs) {
                if (shouldShow(true, s)) {
                    basicLayout.panelsAdd(new UITextButton("D: " + s, fontSize, new Runnable() {
                        @Override
                        public void run() {
                            pathComponents.add(s);
                            rebuild();
                        }
                    }));
                }
            }
            for (final String s : fils) {
                if (shouldShow(false, s)) {
                    basicLayout.panelsAdd(new UITextButton("F: " + s, fontSize, new Runnable() {
                        @Override
                        public void run() {
                            pathComponents.add(s);
                            rebuild();
                        }
                    }));
                }
            }
        } else {
            if (!GaBIEn.dirExists(exact)) {
                showManualControl = false;
                // having a separate screen allows user to back out
                basicLayout.panelsAdd(new UITextButton(strAccept + " " + exact, fontSize, new Runnable() {
                        @Override
                        public void run() {
                            if (!done) {
                                done = true;
                                run.accept(exact);
                            }
                        }
                }));
            }
        }

        if (showManualControl) {
            final UITextBox pathText = new UITextBox("", fontSize);
            lowerSectionContents = new UISplitterLayout(pathText, new UITextButton(strAccept, fontSize, new Runnable() {
                @Override
                public void run() {
                    if (!done) {
                        done = true;
                        run.accept(exact + "/" + pathText.text);
                    }
                }
            }), false, 1.0d);
            lowerSectionContents.forceToRecommended();
        }
        basicLayout.runLayoutLoop();
        lowerSection.runLayoutLoop();
        outerLayout.runLayoutLoop();
    }

    @Override
    public boolean requestsUnparenting() {
        return done;
    }

    @Override
    public void onWindowClose() {
        if (!done) {
            done = true;
            run.accept(null);
        }
    }

    @Override
    public String toString() {
        return strTP + getPath();
    }
}
