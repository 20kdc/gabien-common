/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.wsi.ITextEditingSession;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.eclipse.jdt.annotation.NonNull;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

/**
 * Maintains a textbox.
 * Created on 01/06/17.
 */
public class TextboxMaintainer implements ITextEditingSession {
    public final IGJSEPeripheralsInternal peripheralsInternal;
    public final Panel parent;
    public final boolean multiLine;
    public JTextComponent textComponent;
    public JComponent placeComponent;
    private KeyListener kl;
    public @NonNull String maintainedString;
    private boolean isFirstMaintain = true;
    public boolean sessionIsDead = false;
    public boolean enterFlag = false;

    public TextboxMaintainer(IGJSEPeripheralsInternal pi, Panel panel, KeyListener k, boolean ml, int textHeight, final Function<String, String> feedback, String initText) {
        peripheralsInternal = pi;
        if (textHeight < 16)
            textHeight = 16;
        parent = panel;
        kl = k;
        multiLine = ml;
        maintainedString = initText;
        textComponent = ml ? new JTextArea() : new JTextField();
        textComponent.setText(maintainedString);
        placeComponent = ml ? new JScrollPane(textComponent) : textComponent;
        parent.add(placeComponent);
        // apparently it's not capable of setting sensible defaults
        placeComponent.setBounds(0, 0, 32, textHeight + 1);
        // use a sane font
        textComponent.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, textHeight));
        textComponent.addKeyListener(kl);
        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    enterFlag = true;
            }
        });
        // Allows some access for debugging purposes to the mobile feedback.
        if (GaBIEnImpl.mobileEmulation) {
            if (feedback != null)
                textComponent.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseExited(MouseEvent mouseEvent) {
                        System.err.println("on mobile, feedback says:" + feedback.apply(textComponent.getText()));
                    }
                });
        }
    }

    @Override
    public String maintain(int x, int y, int width, int height) {
        return peripheralsInternal.aroundTheBorderworldMaintain(this, x, y, width, height);
    }

    public String maintainActual(int x, int y, int width, int height) {
        if (sessionIsDead)
            return maintainedString;
        if (!multiLine) {
            // Adjust stuff
            y = y + (height / 2);
            y -= placeComponent.getHeight() / 2;
            height = placeComponent.getHeight();
        }
        // Continue
        boolean needToMove = false;
        if (placeComponent.getX() != x)
            needToMove = true;
        if (placeComponent.getY() != y)
            needToMove = true;
        if (placeComponent.getWidth() != width)
            needToMove = true;
        if (placeComponent.getHeight() != height)
            needToMove = true;
        if (needToMove)
            placeComponent.setBounds(x, y, width, height);

        if (isFirstMaintain) {
            textComponent.setVisible(true);
            textComponent.grabFocus();
            isFirstMaintain = false;
        }

        maintainedString = textComponent.getText();
        return maintainedString;
    }

    @Override
    public void setText(String text) {
        textComponent.setText(text);
        maintainedString = text;
    }

    @Override
    public boolean isEnterJustPressed() {
        return enterFlag;
    }

    @Override
    public void endSession() {
        if (sessionIsDead)
            return;
        parent.remove(placeComponent);
        parent.transferFocusUpCycle();
        sessionIsDead = true;
        peripheralsInternal.finishRemovingEditingSession();
    }

    @Override
    public boolean isSessionDead() {
        return sessionIsDead;
    }
}
