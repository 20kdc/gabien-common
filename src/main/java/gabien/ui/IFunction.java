/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

/**
 * Created on 22/04/16.
 */
public interface IFunction<A, R> {
    public R apply(A a);
}
