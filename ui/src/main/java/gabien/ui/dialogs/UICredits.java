/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.dialogs;

import gabien.GaBIEn;
import gabien.uslx.append.Rect;
import gabien.uslx.licensing.LicenseComponent;
import gabien.uslx.licensing.LicenseManager;

import java.util.LinkedList;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.UIElement;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;

/**
 * Credits.
 * Created 18th October, 2023.
 */
public class UICredits extends UIProxy {
    private final UILabel labelHeader;
    private final UITextButton urlButton;
    private final UILabel labelLicense;
    private final UILabel labelCredits;
    private @Nullable String lastURL;

    public UICredits(int sc, int textHeight) {
        labelHeader = new UILabel("", textHeight);
        urlButton = new UITextButton("", textHeight, () -> {
            if (lastURL != null)
                GaBIEn.tryStartBrowser(lastURL);
        });
        UIScrollLayout buttons = new UIScrollLayout(true, sc);
        LinkedList<LicenseComponent> lcl = LicenseManager.I.getSortedLicenseComponents();
        LinkedList<UIElement> elms = new LinkedList<>();
        for (final LicenseComponent lc : lcl) {
            elms.add(new UITextButton(lc.name, textHeight, () -> {
                loadLicenseComponent(lc);
            }));
        }
        buttons.panelsSet(elms);
        labelLicense = new UILabel("", textHeight);
        labelCredits = new UILabel("", textHeight);
        UIScrollLayout mainUSL = new UIScrollLayout(true, sc, labelHeader, urlButton, labelLicense, labelCredits);
        proxySetElement(new UISplitterLayout(new UILabel("Presence in this list does not imply endorsement by the involved parties.", textHeight), new UISplitterLayout(buttons, mainUSL, false, 0), true, 0), true);
        setForcedBounds(null, new Rect(0, 0, textHeight * 32, textHeight * 16));
        if (lcl.size() > 0)
            loadLicenseComponent(lcl.getFirst());
    }

    private void loadLicenseComponent(LicenseComponent lc) {
        labelHeader.setText(lc.name);
        urlButton.setText(lc.url);
        lastURL = lc.url;
        String lf = GaBIEn.getTextResourceAsString(lc.licenseFile);
        String cf = null;
        if (lc.creditsFile == null) {
            cf = "";
        } else {
            cf = GaBIEn.getTextResourceAsString(lc.creditsFile);
        }
        if (lf == null)
            lf = "<license file missing>";
        if (cf == null)
            cf = "<credits file missing>";
        labelLicense.setText(lf);
        labelCredits.setText(cf);
    }
}
