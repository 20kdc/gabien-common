/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.backendhelp;

/**
 * All images must be backed by one of these.
 * Created on August 21th, 2017
 */
public interface INativeImageHolder {
    // Can return null if none needed.
    Runnable[] getLockingSequence();
    Object getNative();
}
