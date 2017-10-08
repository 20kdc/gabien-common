/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.GaBIEn;

import java.util.LinkedList;

public class UIFileBrowser extends UIPanel implements IWindowElement {
    private boolean done;
    private IConsumer<String> run;
    private UIScrollLayout basicLayout;
    private int fontSize;
    private LinkedList<String> pathComponents = new LinkedList<String>();
    private String strBack, strAccept, strTP;
    public UIFileBrowser(IConsumer<String> r, String titlePrefix, String back, String accept, int fSize, int scrollerSize) {
        run = r;
        strTP = titlePrefix;
        strBack = back;
        strAccept = accept;
        basicLayout = new UIScrollLayout(true, scrollerSize);
        fontSize = fSize;
        allElements.add(basicLayout);
        rebuild();
        // make a *vague* guess
        setBounds(new Rect(0, 0, scrollerSize + fSize * 16, fSize * 16));
    }

    @Override
    public void setBounds(Rect r) {
        basicLayout.setBounds(new Rect(0, 0, r.width, r.height));
        super.setBounds(r);
    }

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
        basicLayout.panels.clear();
        boolean showManualControl = true;
        final String exact = getPath(null);
        String[] paths = GaBIEn.listEntries(exact);
        if (pathComponents.size() > 0) {
            basicLayout.panels.add(new UITextButton(fontSize, strBack, new Runnable() {
                @Override
                public void run() {
                    pathComponents.removeLast();
                    rebuild();
                }
            }));
        }
        if (paths != null) {
            for (final String s : paths) {
                if (s.contains("\\"))
                    throw new RuntimeException("Backend Error (\\)");
                if (s.contains("/"))
                    throw new RuntimeException("Backend Error (/)");
                String pfx = "F:";
                if (GaBIEn.dirExists(s))
                    pfx = "D:";
                basicLayout.panels.add(new UITextButton(fontSize, pfx + s, new Runnable() {
                    @Override
                    public void run() {
                        pathComponents.add(s);
                        rebuild();
                    }
                }));
            }
        } else {
            if (GaBIEn.fileOrDirExists(exact))
                if (!GaBIEn.dirExists(exact)) {
                    showManualControl = false;
                    // having a separate screen allows user to back out
                    basicLayout.panels.add(new UITextButton(fontSize, strAccept, new Runnable() {
                        @Override
                        public void run() {
                            if (!done) {
                                run.accept(exact);
                                done = true;
                            }
                        }
                    }));
                }
        }
        if (showManualControl) {
            final UITextBox pathText = new UITextBox(fontSize);
            basicLayout.panels.add(new UISplitterLayout(pathText, new UITextButton(fontSize, "Accept", new Runnable() {
                @Override
                public void run() {
                    if (!done) {
                        run.accept(exact + "/" + pathText.text);
                        done = true;
                    }
                }
            }), false, 1.0d));
        }
        basicLayout.setBounds(basicLayout.getBounds());
    }

    @Override
    public boolean wantsSelfClose() {
        return done;
    }

    @Override
    public void windowClosed() {
        if (!done)
            run.accept(null);
    }

    @Override
    public String toString() {
        return strTP + getPath(null);
    }
}
