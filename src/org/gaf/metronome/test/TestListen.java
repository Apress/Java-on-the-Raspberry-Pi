package org.gaf.metronome.test;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.sbc.DeviceFactoryHelper;

public class TestListen {

    private static int cnt;
    private static DigitalOutputDevice dodP;

    public static void main(String[] args) throws InterruptedException {
        try (
            DigitalInputDevice did = DigitalInputDevice.Builder.builder(20).
                    setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).
                    setTrigger(GpioEventTrigger.FALLING).build();  
            DigitalOutputDevice dod = new DigitalOutputDevice(21, true, false)) {
            
            did.addListener(TestListen::listen);
            
            dodP = dod;
            
            cnt = 0;
            
            System.out.println("Waiting ...");
            Thread.sleep(10000);
            
            System.out.println("Count = " + cnt);
        } finally {
            DeviceFactoryHelper.shutdown();
        }
    }
    
    private static void listen(DigitalInputEvent event) {
        cnt++;
        dodP.on();
        dodP.off();       
    }       
}
