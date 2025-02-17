/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

import java.io.File;

/**
 * Maven repository utilities.
 * Created 17th February, 2025.
 */
public final class MavenRepository {
    /**
     * Expected to match with umvn
     */
    public static final File REPOSITORY;
    static {
        REPOSITORY = new File(System.getProperty("maven.repo.local", new File(System.getProperty("user.home"), ".m2/repository").toString()));
    }

    private MavenRepository() {
    }

    public static File getJARFile(String groupId, String artifactId, String version) {
        return new File(REPOSITORY, groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
    }
}
