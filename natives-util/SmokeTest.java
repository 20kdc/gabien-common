/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import gabien.natives.Loader;
import gabien.natives.BadGPUUnsafe;

/**
 * Used to quickly test alternate configurations to ensure they bind properly.
 * javac SmokeTest.java -source 1.8 -target 1.8 -cp target/classes
 * java -cp .:target/classes SmokeTest
 * wine java -cp ".;target/classes" SmokeTest
 * Created May 29th, 2023.
 */
public class SmokeTest {
    public static void main(String[] args) {
        Loader.defaultLoader();
        long instance = BadGPUUnsafe.newInstance(BadGPUUnsafe.BADGPUNewInstanceFlags_CanPrintf, RuntimeException.class);
        if (instance == 0)
            throw new RuntimeException("Instance is null!");
        System.out.println("Instance: " + instance);
        String vendor = BadGPUUnsafe.getMetaInfo(instance, BadGPUUnsafe.BADGPUMetaInfoType_Vendor);
        System.out.println("Vendor: " + vendor);
        String renderer = BadGPUUnsafe.getMetaInfo(instance, BadGPUUnsafe.BADGPUMetaInfoType_Renderer);
        System.out.println("Renderer: " + renderer);
        String version = BadGPUUnsafe.getMetaInfo(instance, BadGPUUnsafe.BADGPUMetaInfoType_Version);
        System.out.println("Version: " + version);
    }
}
