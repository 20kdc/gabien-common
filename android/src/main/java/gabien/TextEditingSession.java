/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.IFunction;

/**
 * Text editing session object
 * Created July 28th 2022
 */
public class TextEditingSession implements ITextEditingSession {
    private final Peripherals parent;
    private boolean enterPressed;
    private boolean sessionDead, sessionOfficiallyDead;
    private final boolean multiLine;
    private String lastTextSentToTextbox;
    private String lastTextReceivedFromTextbox;
    private IFunction<String, String> lastFeedback;

    public TextEditingSession(Peripherals par, @NonNull String text, boolean ml, int textHeight, @Nullable IFunction<String, String> feedback) {
        parent = par;
        if (parent.currentTextEditingSession != null)
            parent.currentTextEditingSession.endSession();
        parent.currentTextEditingSession = this;

        multiLine = ml;
        lastTextSentToTextbox = text;
        lastTextReceivedFromTextbox = text;
        lastFeedback = feedback;

        AndroidPortGlobals.mainActivityLock.lock();
        ITextboxImplementation impl = TextboxImplObject.getInstanceHoldingMALock();
        impl.setActive(text, multiLine, feedback);
        AndroidPortGlobals.mainActivityLock.unlock();
    }

    @Override
    public String maintain(int x, int y, int w, int h, String text) {
        if (sessionDead)
            return lastTextReceivedFromTextbox;
        AndroidPortGlobals.mainActivityLock.lock();
        ITextboxImplementation impl = TextboxImplObject.getInstanceHoldingMALock();
        if (!lastTextSentToTextbox.equals(text)) {
            impl.setActive(text, multiLine, lastFeedback);
            lastTextSentToTextbox = text;
        }
        if (impl.isTrustworthy())
            lastTextReceivedFromTextbox = impl.getLastKnownText();
        AndroidPortGlobals.mainActivityLock.unlock();
        return lastTextReceivedFromTextbox;
    }

    @Override
    public boolean isEnterJustPressed() {
        return enterPressed;
    }

    @Override
    public void endSession() {
        sessionOfficiallyDead = true;
        // actually end session
        if (sessionDead)
            return;
        sessionDead = true;
        if (parent.currentTextEditingSession == this)
            parent.currentTextEditingSession = null;

        AndroidPortGlobals.mainActivityLock.lock();
        ITextboxImplementation impl = TextboxImplObject.getInstanceHoldingMALock();
        if (impl.isTrustworthy())
            lastTextReceivedFromTextbox = impl.getLastKnownText();
        impl.setInactive();
        AndroidPortGlobals.mainActivityLock.unlock();
    }

    @Override
    public boolean isSessionDead() {
        return sessionOfficiallyDead;
    }

    public void gdUpdateTextboxHoldingMALock() {
        ITextboxImplementation tio = TextboxImplObject.getInstanceHoldingMALock();
        boolean alive = tio.checkupUsage();
        // detect specifically the *event* of textbox closure
        if (!alive) {
            // end the session but mark us as having not actually ended the session
            // this should cause app to see the enterPressed and formally end session
            // if not,  who cares?
            enterPressed = true;
            endSession();
            sessionOfficiallyDead = false;
        }
    }

}
