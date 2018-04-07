/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.GaBIEn;

import java.util.Collections;
import java.util.LinkedList;

/**
 * Forgot when this was made initially, but now, February 17th, 2018:
 * With this working, with text being wrapped in complex layouts, I saw everything working exactly as I had planned.
 * I'm going ahead with changing things to this.
 */
public class UIFileBrowser extends UIElement.UIProxy {
    private boolean done;
    private IConsumer<String> run;
    private UIScrollLayout basicLayout;
    private UISplitterLayout outerLayout;
    private UIPublicPanel lowerSection;
    private UIElement lowerSectionContents;
    private int fontSize;
    private LinkedList<String> pathComponents = new LinkedList<String>();
    private String strBack, strAccept, strTP;

    public UIFileBrowser(IConsumer<String> r, String titlePrefix, String back, String accept, int fSize, int scrollerSize) {
        // Vague guess.
        run = r;
        strTP = titlePrefix;
        strBack = back;
        strAccept = accept;
        basicLayout = new UIScrollLayout(true, scrollerSize);
        lowerSection = new UIPublicPanel(1, 1) {
            @Override
            public void runLayout() {
                if (lowerSectionContents != null) {
                    lowerSectionContents.forceToRecommended(this);
                    setWantedSize(lowerSectionContents.getWantedSize());
                    lowerSectionContents.setForcedBounds(this, new Rect(getSize()));
                }
            }
        };
        fontSize = fSize;
        outerLayout = new UISplitterLayout(basicLayout, lowerSection, true, 1);
        rebuild();
        outerLayout.setForcedBounds(null, new Rect(outerLayout.getWantedSize()));
        outerLayout.runLayout();
        proxySetElement(outerLayout, true);
    }

    // Should we show a given item, by postfix? (Point for future expansion)
    public boolean shouldShow(String possiblePostfix) {
        return true;
    }

    // Translates the current path components (and possibly the 'last' component, which might be null) into a system path.
    public String getPath(String possiblePostfix) {
        String text = "";
        boolean first = true;
        for (String s : pathComponents) {
            if (!first)
                text += "/";
            text += s;
            first = false;
        }
        if (text.equals(""))
            text = ".";
        if (possiblePostfix != null)
            return text + "/" + possiblePostfix;
        return text;
    }

    private void rebuild() {
        basicLayout.panelsClear();

        if (lowerSectionContents != null) {
            lowerSection.layoutRemoveElement(lowerSectionContents);
            lowerSectionContents = null;
        }

        boolean showManualControl = true;
        final String exact = getPath(null);
        String[] paths = GaBIEn.listEntries(exact);
        basicLayout.panelsAdd(new UITextButton(strBack, fontSize, new Runnable() {
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
                if (GaBIEn.dirExists(s)) {
                    dirs.add(s);
                } else {
                    fils.add(s);
                }
            }
            Collections.sort(dirs);
            Collections.sort(fils);
            for (final String s : dirs) {
                if (shouldShow(s)) {
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
                if (shouldShow(s)) {
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
            if (GaBIEn.fileOrDirExists(exact))
                if (!GaBIEn.dirExists(exact)) {
                    showManualControl = false;
                    // having a separate screen allows user to back out
                    basicLayout.panelsAdd(new UITextButton(strAccept, fontSize, new Runnable() {
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
            lowerSection.layoutAddElement(lowerSectionContents);
        }
        basicLayout.runLayout();
        lowerSection.runLayout();
        outerLayout.runLayout();
    }

    @Override
    public boolean requestsUnparenting() {
        return done;
    }

    @Override
    public void handleRootDisconnect() {
        super.handleRootDisconnect();
        if (!done) {
            done = true;
            run.accept(null);
        }
    }

    @Override
    public String toString() {
        return strTP + getPath(null);
    }
}
