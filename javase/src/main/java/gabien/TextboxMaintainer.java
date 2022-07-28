/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.uslx.append.*;

import javax.swing.*;

import org.eclipse.jdt.annotation.Nullable;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Maintains a textbox.
 * Created on 01/06/17.
 */
public class TextboxMaintainer implements ITextEditingSession {
    public final IGJSEPeripheralsInternal peripheralsInternal;
    public final Panel parent;
    public JTextField target;
    private KeyListener kl;
    // null means unmaintained!
    public @Nullable String maintainedString;
    public boolean sessionIsDead = false;
    public boolean enterFlag = false;

    public TextboxMaintainer(IGJSEPeripheralsInternal pi, Panel panel, KeyListener k, int textHeight, final IFunction<String, String> feedback) {
        peripheralsInternal = pi;
        if (textHeight < 16)
            textHeight = 16;
        parent = panel;
        kl = k;
        target = new JTextField();
        parent.add(target);
        // apparently it's not capable of setting sensible defaults
        target.setBounds(0, 0, 32, textHeight + 1);
        // use a sane font
        target.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, textHeight));
        target.addKeyListener(kl);
        target.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    enterFlag = true;
            }
        });
        // Allows some access for debugging purposes to the mobile feedback.
        if (GaBIEnImpl.mobileEmulation) {
            if (feedback != null)
                target.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseExited(MouseEvent mouseEvent) {
                        System.err.println("on mobile, feedback says:" + feedback.apply(target.getText()));
                    }
                });
        }
    }

    @Override
    public String maintain(int x, int y, int width, int height, String text) {
        return peripheralsInternal.aroundTheBorderworldMaintain(this, x, y, width, height, text);
    }

    public String maintainActual(int x, int y, int width, int height, String text) {
        // Adjust stuff
        y = y + (height / 2);
        // Continue
        boolean needToMove = false;
        if (target.getX() != x)
            needToMove = true;
        if (target.getY() != (y - (target.getHeight() / 2)))
            needToMove = true;
        if (target.getWidth() != width)
            needToMove = true;
        if (needToMove) {
            target.setLocation(x, y - (target.getHeight() / 2));
            target.setSize(width, target.getHeight());
        }

        if (maintainedString != null) {
            if (!maintainedString.equals(text))
                target.setText(text);
        } else {
            target.setText(text);
            target.setVisible(true);
            target.grabFocus();
        }

        maintainedString = target.getText();
        return maintainedString;
    }

    @Override
    public boolean isEnterJustPressed() {
        return enterFlag;
    }

    @Override
    public void endSession() {
        if (sessionIsDead)
            return;
        target.setVisible(false);
        parent.remove(target);
        parent.transferFocusUpCycle();
        target = null;
        sessionIsDead = true;
        peripheralsInternal.finishRemovingEditingSession();
    }

    @Override
    public boolean isSessionDead() {
        return sessionIsDead;
    }
}
