
package org.gaf.pimu.test;

import com.diozero.api.RuntimeIOException;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;
import java.io.IOException;
import org.gaf.pimu.FXAS21002C;

/**
 * For testing the FXAS21002C core plus status.
 */
public class TestFXASCore_S {

//    private static FXAS21002C device;

    public static void main(String[] args) throws IOException, InterruptedException {
        // create and configure the gyro
        
        try (FXAS21002C device = new FXAS21002C()) {
            
            device.begin(FXAS21002C.LpfCutoff.Lowest, FXAS21002C.ODR.ODR_50);
        
            int num = Integer.valueOf(args[0]);

            readZ(device, num);
        } finally {
            Diozero.shutdown();
        }
    }
    
    private static void readZ(FXAS21002C device, int num) 
            throws RuntimeIOException {
        
        long tCurrent, tLast, tDelta;
        tLast = System.nanoTime();
        device.readRawZ();
        for (int i = 0; i < num; i++) {
            device.isZReady(true);
            tCurrent = System.nanoTime();
            tDelta = (tCurrent - tLast) / 100000;
            tLast = tCurrent;
            int z = device.readRawZ();
            System.out.println(z + ", " + tDelta); 
            
            SleepUtil.sleepMillis(15);
        }        
    }   
}
