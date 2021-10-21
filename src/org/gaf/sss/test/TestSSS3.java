package org.gaf.sss.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.gaf.sss.SilentStepStick;

/**
 * Test the distance capability of SilentStepStick.
 */
public class TestSSS3 {
    
    private static AtomicBoolean done;

    public static void main(String[] args) throws IOException, InterruptedException {
        try (SilentStepStick stepper = new SilentStepStick(4, 27, 17, 200,
                SilentStepStick.Resolution.Quarter)) {
            
            done = new AtomicBoolean(false);
            
            stepper.enable(true);
            
            System.out.println("Run CW");
            done.set(false);
            boolean status = stepper.stepCount(100, 
                    SilentStepStick.Direction.CW, 4f, true, TestSSS3::whenDone);
            
            while (!done.get()) {
                Thread.sleep(100);
            }
            System.out.println("DONE");
//            System.out.println("Count = " + stepper.getStepCount());
                        
            System.out.println("Run CCW");
            done.set(false);
            status = stepper.stepCount(200, 
                    SilentStepStick.Direction.CCW, 2f, true, TestSSS3::whenDone);

            while (!done.get()) {
                Thread.sleep(100);
            }
            System.out.println("DONE");
//            System.out.println("Count = " + stepper.getStepCount());
                                    
            System.out.println("Disabling");
            stepper.enable(false);
            
            System.out.println("Closing");
        } finally {
            Diozero.shutdown();
        }       
    } 
    
    private static void whenDone() {
        System.out.println("Device done");
        done.set(true); 
    }   
}
