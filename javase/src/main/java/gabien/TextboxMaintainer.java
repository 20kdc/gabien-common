/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Maintains a textbox.
 * Created on 01/06/17.
 */
public class TextboxMaintainer {
    public final Panel parent;
    public JTextField target;
    private KeyListener kl;
    // null means unmaintained!
    public String maintainedString = null;
    public boolean maintainedThisFrame = false;

    public TextboxMaintainer(Panel panel, KeyListener k) {
        parent = panel;
        kl = k;
    }

    public void newFrame() {
        if (!maintainedThisFrame)
            clear();
        maintainedThisFrame = false;
    }

    public String maintain(int x, int y, int width, String text, final IFunction<String, String> feedback) {
        if (target == null) {
            // wait as long as possible because of font loading perf.
            // IDK if it's loading every font on the system or something but this is a real issue...
            target = new JTextField();
            parent.add(target);
            // apparently it's not capable of setting sensible defaults
            target.setBounds(0, 0, 32, 17);
            // use a sane font
            target.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            target.addKeyListener(kl);
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

        maintainedThisFrame = true;
        maintainedString = target.getText();
        return maintainedString;
    }

    public void clear() {
        maintainedThisFrame = false;
        maintainedString = null;
        if (target != null) {
            target.setVisible(false);
            parent.remove(target);
            parent.transferFocusUpCycle();
            target = null;
        }
    }
}
