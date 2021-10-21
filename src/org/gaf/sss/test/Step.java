package org.gaf.sss.test;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.util.Diozero;

/**
 * Test the DigitalOutPutDevice onOffLoop method for driving 
 * a SilentStepStick.
 */
public class Step {

    public static void main(String[] args) throws InterruptedException {

        try (DigitalOutputDevice pwm = new DigitalOutputDevice(17, true, false)) {
                         
            pwm.onOffLoop(0.009375f, 0.009375f,
                    DigitalOutputDevice.INFINITE_ITERATIONS,
                    true, null);
            
            System.out.println("Waiting ...");
            Thread.sleep(5000);
            
            pwm.stopOnOffLoop();
            System.out.println("Done");
        } finally {
            Diozero.shutdown();
        }
    } 
}
