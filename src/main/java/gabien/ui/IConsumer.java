/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien.ui;

/**
 * replacement for 1.8 stuff
 * Created on 22/04/16.
 */
public interface IConsumer<T> {
    void accept(T t);
}
