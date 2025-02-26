/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

/**
 * Created 26th February 2025.
 */
@SuppressWarnings("serial")
public class AlreadyReportedRuntimeException extends RuntimeException {
    public AlreadyReportedRuntimeException() {
        super("The exception this covers should have already been reported. If it has not been reported, this is a bug.");
    }
}
