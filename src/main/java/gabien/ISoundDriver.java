/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien;

/**
 * Represents a 22050 hertz sound system.
 */
public interface ISoundDriver {
    
    public static interface IChannel {
        // Note: If looping is false, the channel will set it's own volume to 0 at the end.
        public void playSound(double Pitch,double VolL,double VolR,short[] sound,boolean looping);
        public void setVolume(double VolL,double VolR);
        public double volL();
        public double volR();
    }
    public IChannel createChannel();
    public void deleteChannel(IChannel ic);
}
