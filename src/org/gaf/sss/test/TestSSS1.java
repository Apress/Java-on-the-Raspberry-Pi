package org.gaf.sss.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import org.gaf.sss.SilentStepStick;

/**
 * Simple test of SilentStepStick. 
 */
public class TestSSS1 {

    public static void main(String[] args) throws IOException, InterruptedException {
        try (SilentStepStick stepper = new SilentStepStick(4, 27, 17, 200,
                SilentStepStick.Resolution.Quarter)) {
            
            stepper.enable(true);
            
            System.out.println("Run CW");
            stepper.run(SilentStepStick.Direction.CW, 4f);
            
            Thread.sleep(5000);
                        
            System.out.println("Stopping");
            stepper.stop();
//            System.out.println("Count = " + stepper.getStepCount());
            
            System.out.println("Disabling");
            stepper.enable(false);
            
            System.out.println("Closing");
        } finally {
            Diozero.shutdown();
        }           
    } 
}
