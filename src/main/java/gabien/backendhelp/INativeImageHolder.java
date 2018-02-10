/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

/**
 * All images must be backed by one of these.
 * Created on August 21th, 2017
 */
public interface INativeImageHolder {
    // Only to be called from ThreadForwardingGrDriver or other stuff that needs this kind of direct poking,
    //  hence the N to cause compile errors on older code.
    // Should be called when the call is *scheduled by the game.*
    Runnable[] getLockingSequenceN();
    Object getNative();
}
