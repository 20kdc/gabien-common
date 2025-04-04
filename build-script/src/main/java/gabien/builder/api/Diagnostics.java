/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

/**
 * Diagnostics logger.
 * Created 17th February, 2025.
 */
public interface Diagnostics {
    /**
     * Logs information.
     */
    void info(String text);

    /**
     * Logs a warning.
     */
    void warn(String text);

    /**
     * Logs and causes error status.
     */
    void error(String text);

    /**
     * Returns true if any error has occurred.
     */
    boolean hasAnyErrorOccurred();

    /**
     * Diagnostics instance that downgrades errors to warnings.
     * However, it also tracks errors independently.
     */
    default Diagnostics warningScope() {
        Diagnostics parent = this;
        return new Diagnostics() {
            boolean internalErrorFlag = false;

            @Override
            public void warn(String text) {
                parent.warn(text);
            }
            
            @Override
            public void info(String text) {
                parent.info(text);
            }
            
            @Override
            public boolean hasAnyErrorOccurred() {
                return parent.hasAnyErrorOccurred() || internalErrorFlag;
            }
            
            @Override
            public void error(String text) {
                // downgrade to warning and mark
                parent.warn(text);
                internalErrorFlag = true;
            }
        };
    }

    default void report(String msg, Exception ex) {
        error(msg + ": " + ex);
        ex.printStackTrace();
    }

    default void reportAndThrowARRE(String msg, Exception ex) {
        report(msg, ex);
        throw new AlreadyReportedRuntimeException();
    }
}
