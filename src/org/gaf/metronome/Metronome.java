package org.gaf.metronome;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.gaf.sss.SilentStepStick;

public class Metronome {
    
    private static SilentStepStick eStop; 
    private static final AtomicBoolean emergency = new AtomicBoolean(false);
    
    public static void main(String[] args) throws IOException, InterruptedException {
        try (
            SilentStepStick stepper = new SilentStepStick(4, 27, 17, 200,
                SilentStepStick.Resolution.Quarter);
            DigitalInputDevice swCW = DigitalInputDevice.Builder.builder(20).
                setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).build();
            DigitalInputDevice swCCW = DigitalInputDevice.Builder.builder(21).
                setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).build()
            ) {
            
            // set up for emergency stop
            eStop = stepper;
                        
            // engage Java shutdown safety net
            Diozero.registerForShutdown(stepper);
            
            // wait for start 
            System.out.println("Waiting to start .... Press CW switch.");
            boolean status = swCW.waitForActive(10000);
            if (status) {
                System.out.println("Starting");
            } else {
                System.out.println("Failure to start!");
                System.exit(1);
            }
            // make sure switch not bouncing
            Thread.sleep(100);
           
            // run to CW switch
            stepper.enable(true);
            System.out.println("Run CW");
            stepper.run(SilentStepStick.Direction.CW, 1f);
            System.out.println("Waiting to hit switch ...");
            status = swCW.waitForActive(20000);
            stepper.stop();        
            if (status) {
                System.out.println("Got it");
            } else {
                System.out.println("Motor not running");
                System.exit(1);
            }

            // run to CCW switch
            System.out.println("Run CCW");
            stepper.run(SilentStepStick.Direction.CCW, 1f);
            System.out.println("Waiting to hit switch ...");
            status = swCCW.waitForActive(20000);
            stepper.stop();        
            if (status) {
                System.out.println("Got it");
            } else {
                System.out.println("Motor not running");
                System.exit(1);
            }
            
            // get step count; calculate moves
            int sw2sw = stepper.getStepCount();
            System.out.println("Step Count = " + sw2sw);            
            int buffer = 15;
            int first = sw2sw - buffer;
            int nominal = sw2sw - (2 * buffer);
            int middle = sw2sw/2 - buffer;
            
            // move to CW
            stepper.stepCount(first, SilentStepStick.Direction.CW, 4f, 
                    false, null);            
            
            // set up limit switches for emergency stop
            swCW.whenActivated(Metronome::limitHit);
            swCCW.whenActivated(Metronome::limitHit);
            
            // move back and forth 
            for (int i = 0; i < 4; i++) {
                // move to CCW
                stepper.stepCount(nominal, SilentStepStick.Direction.CCW, 4f, 
                        false, null);
                if (emergency.get()) break;
                // move to CW
                stepper.stepCount(nominal, SilentStepStick.Direction.CW, 4f, 
                        false, null);            
                if (emergency.get()) break;
            }
            
            // move to middle
            if (!emergency.get())
                stepper.stepCount(middle, SilentStepStick.Direction.CCW, 4f, 
                        false, null);

            stepper.enable(false);
        } finally {
            Diozero.shutdown();
        }
    }  
    
    private static void limitHit(long ts) {
        emergency.set(true);
        eStop.stop();
    }
}
