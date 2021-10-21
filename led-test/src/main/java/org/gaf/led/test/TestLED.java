package org.gaf.led.test;

import com.diozero.util.Diozero;
import org.gaf.led.LED;

public class TestLED {
    
    public static void main(String[] args) throws InterruptedException {
        try (LED led = new LED(18)) {    
            System.out.println("Doing Test!");
            
            for (int i = 0; i < 5; i++) {
                led.on();
                Thread.sleep(500);
                led.off();
                Thread.sleep(500);
            }
        } finally {
            Diozero.shutdown();
        }
    }    
}