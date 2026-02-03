/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package org.eclipse.jdt.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Vaguely-compatible implementation of Eclipse JDT annotation class.
 * This one's particularly bad and might theoretically cause build issues if non-GaBIEn code is involved.
 * If it does, please tell me?
 */
@Retention(CLASS)
@Target({ TYPE, PACKAGE })
public @interface NonNullByDefault {

}
