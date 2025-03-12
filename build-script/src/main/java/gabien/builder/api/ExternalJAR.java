/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

/**
 * Fixes everything once and for all.
 * Created 12th March 2025.
 */
public final class ExternalJAR {
    public final String id;
    public final MavenCoordinates coordinates;
    public final String url;
    public final String license;
    public final Runnable prerequisite;

    public ExternalJAR(String id, MavenCoordinates coordinates, String url, String license) {
        this.id = id;
        this.coordinates = coordinates;
        this.url = url;
        this.license = license;
        prerequisite = () -> {
            if (!appearsInstalled())
                throw new RuntimeException("Artifact "+ coordinates + " is not installed. Try gabien-do install-extern " + id);
        };
    }

    /**
     * True if this appears installed.
     */
    public boolean appearsInstalled() {
        return MavenRepository.getJARFile(coordinates).exists();
    }

    /**
     * Installs the dependency automatically.
     */
    public void install(CommandEnv diag) {
        diag.run(CommandEnv.UMVN_COMMAND, "install-file", "-Durl=" + url, "-DgroupId=" + coordinates.groupId, "-DartifactId=" + coordinates.artifactId, "-Dversion=" + coordinates.version, "-Dpackaging=jar");
    }
}
