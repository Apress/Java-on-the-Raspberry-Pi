package org.gaf.metronome.test;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.Diozero;

public class TestWhen {

    private static int cnt;
    private static DigitalOutputDevice dodP;

    public static void main(String[] args) throws InterruptedException {
        try (
            DigitalInputDevice did = DigitalInputDevice.Builder.builder(20).
                    setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).build();  
             DigitalOutputDevice dod = new DigitalOutputDevice(21, true, false)) {
            
            did.whenActivated(TestWhen::when);
            
            dodP = dod;
            
            cnt = 0;
            
            System.out.println("Waiting ...");
            Thread.sleep(10000);
            
            System.out.println("Count = " + cnt);            
        } finally {
            Diozero.shutdown();
        }
    }
    
    private static void when(long ts) {
        cnt++;
        dodP.on();
        dodP.off();       
    }     
}
