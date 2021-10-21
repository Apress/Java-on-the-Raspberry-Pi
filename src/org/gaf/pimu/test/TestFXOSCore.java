package org.gaf.pimu.test;

import com.diozero.api.RuntimeIOException;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;
import java.io.IOException;
import org.gaf.pimu.FXOS8700CQ;

/**
 * For testing the FXOS8700CQ core.
 */
public class TestFXOSCore {
    
    public static void main(String[] args) throws IOException, InterruptedException {
   
        try (FXOS8700CQ device = new FXOS8700CQ()) {

            device.begin();

            int num = Integer.valueOf(args[0]);

            int axis = 3;

            readAM(device, num, axis); 
        } finally {
            Diozero.shutdown();
        }
    }    

    private static void readAM(FXOS8700CQ device, 
            int num, int axis) throws RuntimeIOException {
        int[] am;
        long tCurrent, tLast, tDelta;
        tLast = System.nanoTime();
        device.readRaw();
        for (int i = 0; i < num; i++) {
            device.isDataReady(true);
            tCurrent = System.nanoTime();
            tDelta = (tCurrent - tLast) / 100000;
            tLast = tCurrent;
            am = device.readRaw();
            System.out.println(am[axis] + ", " + tDelta); 
//            System.out.println(am[0] + ", " + am[1]);

            SleepUtil.sleepMillis(15);
        }        
    }
}
