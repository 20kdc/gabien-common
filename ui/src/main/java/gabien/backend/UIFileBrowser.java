/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.backend;

import gabien.GaBIEn;
import gabien.ui.*;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextBox;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;
import gabien.uslx.append.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Forgot when this was made initially, but now, February 17th, 2018:
 * With this working, with text being wrapped in complex layouts, I saw everything working exactly as I had planned.
 * I'm going ahead with changing things to this.
 * -- Note: This later (at first appearance in gabien-android) became essentially a backend support class for when native file browsers aren't available.
 * Imported into gabien-common on 04/03/2020.
 */
public class UIFileBrowser extends UIElement.UIProxy {
    private boolean done;
    private Consumer<String> run;
    private UILabel upperSection;
    private UIScrollLayout basicLayout;
    private UISplitterLayout outerLayout;
    private UIDynamicProxy lowerSection;
    private int fontSize;
    private LinkedList<String> pathComponents;
    private boolean saving;
    private String strTP;
    private String defaultFileName;

    public UIFileBrowser(String actPath, Consumer<String> r, String titlePrefix, boolean saving, int fSize, int scrollerSize, String dfn) {
        this.saving = saving;
        defaultFileName = dfn;
        run = r;
        strTP = titlePrefix;
        pathComponents = UIFileBrowser.createComponents(actPath);
        basicLayout = new UIScrollLayout(true, scrollerSize);
        upperSection = new UILabel("!", fSize);
        lowerSection = new UIDynamicProxy();
        fontSize = fSize;
        outerLayout = new UISplitterLayout(upperSection, new UISplitterLayout(basicLayout, lowerSection, true, 1), true, 0);
        rebuild();
        outerLayout.setForcedBounds(null, new Rect(outerLayout.getWantedSize()));
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

        lowerSection.dynProxySet(null);

        boolean showManualControl = true;
        final String verb = saving ? GaBIEn.wordSave : GaBIEn.wordLoad;
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
                basicLayout.panelsAdd(new UITextButton(verb + " " + exact, fontSize, new Runnable() {
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
            final UITextBox pathText = new UITextBox(defaultFileName, fontSize);
            final UILabel statusLine = new UILabel("", fontSize);
            UISplitterLayout mainLine = new UISplitterLayout(pathText, new UITextButton(verb, fontSize, new Runnable() {
                @Override
                public void run() {
                    statusLine.text = "";
                    String txt = pathText.text;
                    // catch anything too obvious for better UX
                    if (txt.equals(".") || txt.equals("..") || txt.contains("/") || txt.contains("\\") || txt.equals("")) {
                        statusLine.text = GaBIEn.wordInvalidFileName;
                        return;
                    }
                    if (!done) {
                        done = true;
                        run.accept(exact + "/" + txt);
                    }
                }
            }), false, 1.0d);
            lowerSection.dynProxySet(new UISplitterLayout(mainLine, statusLine, true, 0.5d));
        }
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
