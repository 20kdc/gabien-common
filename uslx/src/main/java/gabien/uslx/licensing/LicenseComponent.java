/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.licensing;

import java.util.HashSet;

/**
 * Collates licensing data.
 * Dependencies are dynamic because of things like "natives may or may not be loaded".
 * gabien-natives is still part of gabien-common, but if natives aren't in the output binary,
 *  then they shouldn't be included.
 * Conversely, if they are, they should be, even though gabien-uslx doesn't depend on them!
 * Created 17th October, 2023.
 */
public final class LicenseComponent {
    public final String name;
    public final String url;
    public final String licenseFile;
    public final String creditsFile;

    /**
     * Controlled from LicenseManager.
     * Every component directly or indirectly dependent on this one.
     */
    final HashSet<LicenseComponent> dependents = new HashSet<>();

    /**
     * Controlled from LicenseManager.
     * Every component this component is directly or indirectly dependent on.
     */
    final HashSet<LicenseComponent> dependencies = new HashSet<>();

    public static final LicenseComponent LC_GABIEN = new LicenseComponent(
            "gabien-common",
            "https://github.com/20kdc/gabien-common/",
            "gabien/licensing/common/COPYING.txt",
            "gabien/licensing/common/CREDITS.txt"
    );

    public LicenseComponent(String n, String u, String licenseFile, String creditsFile) {
        name = n;
        url = u;
        this.licenseFile = licenseFile;
        this.creditsFile = creditsFile;
    }
}
