/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.dialogs;

import gabien.GaBIEn;
import gabien.ui.UILabel;
import gabien.ui.UIScrollLayout;
import gabien.ui.UISplitterLayout;
import gabien.ui.UITextButton;
import gabien.uslx.append.Rect;
import gabien.uslx.licensing.LicenseComponent;
import gabien.uslx.licensing.LicenseManager;

import java.util.LinkedList;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.UIElement.UIProxy;

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
        for (final LicenseComponent lc : lcl) {
            buttons.panelsAdd(new UITextButton(lc.name, textHeight, () -> {
                loadLicenseComponent(lc);
            }));
        }
        labelLicense = new UILabel("", textHeight);
        labelCredits = new UILabel("", textHeight);
        UIScrollLayout mainUSL = new UIScrollLayout(true, sc);
        mainUSL.panelsAdd(labelHeader);
        mainUSL.panelsAdd(urlButton);
        mainUSL.panelsAdd(labelLicense);
        mainUSL.panelsAdd(labelCredits);
        proxySetElement(new UISplitterLayout(buttons, mainUSL, false, 0), true);
        setForcedBounds(null, new Rect(0, 0, textHeight * 32, textHeight * 16));
        if (lcl.size() > 0)
            loadLicenseComponent(lcl.getFirst());
    }
    private void loadLicenseComponent(LicenseComponent lc) {
        labelHeader.text = lc.name;
        urlButton.text = lc.url;
        lastURL = lc.url;
        labelLicense.text = GaBIEn.getTextResourceAsString(lc.licenseFile);
        labelCredits.text = GaBIEn.getTextResourceAsString(lc.creditsFile);
    }
}
