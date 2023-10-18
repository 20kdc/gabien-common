/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.licensing;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Collates licensing data.
 * A caveat is that this requires each library to self-register all license components, though.
 * But this was still the best option I could think of.
 * Created 17th October, 2023.
 */
public enum LicenseManager {
    I;

    private final HashSet<LicenseComponent> components = new HashSet<>();

    private LicenseManager() {
        register(LicenseComponent.GABIEN_COMMON);
    }

    /**
     * Registers a license component.
     */
    public synchronized void register(LicenseComponent a) {
        components.add(a);
    }

    /**
     * Registers a dependency, which controls ordering.
     */
    public synchronized void dependency(LicenseComponent x, LicenseComponent y) {
        // catches indirect loops due to the rules below
        if (y.dependencies.contains(x))
            throw new RuntimeException("Dependency loop: " + x.name + " x " + y.name);
        // X depends on Y
        x.dependencies.add(y);
        y.dependents.add(x);
        // Z which is dependent on X...
        for (LicenseComponent z : x.dependents) {
            // Z depends on Y because Z depends on X which depends on Y
            z.dependencies.add(y);
            y.dependents.add(z);
        }
    }

    /**
     * Returns a HashSet of all current license components.
     */
    public synchronized HashSet<LicenseComponent> getLicenseComponents() {
        return new HashSet<>(components);
    }

    /**
     * Returns a sorted list of all current license components.
     */
    public LinkedList<LicenseComponent> getSortedLicenseComponents() {
        LinkedList<LicenseComponent> ll = new LinkedList<>();
        HashSet<LicenseComponent> seen = new HashSet<>();
        HashSet<LicenseComponent> pass = new HashSet<>();
        // because loops are not allowed, there's always at least one root for any given component
        // so use that to setup the "initial pass"
        for (LicenseComponent lc : getLicenseComponents())
            if (lc.dependents.size() == 0)
                pass.add(lc);
        while (pass.size() > 0) {
            HashSet<LicenseComponent> thisPass = pass;
            pass = new HashSet<>();
            for (LicenseComponent lc : thisPass) {
                // don't repeat components (they can occur multiple times in the tree, but we only care about the 1st)
                if (seen.contains(lc))
                    continue;
                seen.add(lc);
                ll.add(lc);
                pass.addAll(lc.dependencies);
            }
        }
        return ll;
    }
}
