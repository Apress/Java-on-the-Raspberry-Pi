package org.gaf.metronome.test;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.Diozero;

public class TestWait {

    public static void main(String[] args) throws InterruptedException {
        try (
            DigitalInputDevice did = DigitalInputDevice.Builder.builder(20).
                    setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).build();  
            DigitalOutputDevice dod = new DigitalOutputDevice(21, true, false)) {
            
            int num = 3;
            for (int i = 0; i < num; i++) {

                System.out.println("Waiting ...");
                boolean status = did.waitForActive(10000);
                dod.on();
                if (status) {
                    System.out.println("Got it");
                }
                Thread.sleep(5);
                dod.off();
                
                System.out.println("Killing time");
                if (i < (num - 1)) Thread.sleep(4000);
            }            
            System.out.println("Done");
        } finally {
            Diozero.shutdown();
        }
    }
}
