package org.gaf.roboclaw.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import org.gaf.roboclaw.RoboClaw;
import org.gaf.roboclaw.RoboClawUtil;

/**
 * Tests RoboClaw motor commands. See internal comments.
 */
public class TestClawMotor {

    private final static int ADDRESS = 0x80;
    
    /**
     * Tests various forms of motor control.
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // identity verification 
        String clawFile = RoboClawUtil.findDeviceFile("03eb", "2404", ADDRESS);   
        if (clawFile == null) {
            throw new IOException("No matching device!");
        }  
        
        try (RoboClaw claw = new RoboClaw(clawFile, ADDRESS)) {
        
            Diozero.registerForShutdown(claw);
                        
        /*
        Uncomment the form of motor control you wish to test. The code
        as written is designed to test only one form at a time. If you 
        wish to test more than one, you should inject delays between the
        different forms.
        */        
// -----------------------------------------------------------
        
//            boolean ok = claw.speedAccelDistanceM1M2(400, 
//                    400, 2400, 
//                    400, 2400, 
//                    true);
//            ok = claw.speedAccelDistanceM1M2(400, 
//                    0, 0, 
//                    0, 0, 
//                    true);
//            // wait for buffered commands to finish
//            Thread.sleep(10000);

//------------------------------------------------------------

//            claw.speedDistanceM1M2(200, 1200, 200, 1200, true);
//            claw.speedDistanceM1M2(0, 0, 0, 0, true);
//            // wait for buffered commands to finish
//            Thread.sleep(10000);

//------------------------------------------------------------

//            claw.speedAccelM1M2(400, 400, 400);
//            // wait for unbuffered command to complete
//            Thread.sleep(4000);
//            claw.speedAccelM1M2(400, 0, 0);
//            // wait for unbuffered command to complete
//            Thread.sleep(4000);
        
//------------------------------------------------------------

//            claw.speedM1M2(200, 200);
//            // wait for unbuffered command to complete
//            Thread.sleep(4000);
//            claw.speedM1M2(0, 0);
//            // wait for unbuffered command to complete
//            Thread.sleep(1000);

        } finally {
            Diozero.shutdown();
        }
    }
}
