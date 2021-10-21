package org.gaf.pimu.test;

import com.diozero.api.RuntimeIOException;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;
import java.io.IOException;
import org.gaf.pimu.FXAS21002C;

/**
 * For testing the FXAS21002C core: just reads data.
 */
public class TestFXASCore {

    public static void main(String[] args) throws IOException, InterruptedException {

        try (FXAS21002C device = new FXAS21002C()) {
            
            device.begin(FXAS21002C.LpfCutoff.Lowest, FXAS21002C.ODR.ODR_50);

            int num = Integer.valueOf(args[0]);

            int axis = 2;

            readXYZ(device, num, axis);

            System.out.println("\n ... MORE ... \n");
            Thread.sleep(2000);

            readXYZ(device, num, axis);                        
        } finally {
            Diozero.shutdown();
        }
    }
    
    private static void readXYZ(FXAS21002C device, int num, int axis) 
            throws RuntimeIOException {
        int[] xyz;
        for (int i = 0; i < num; i++) {
            xyz = device.readRaw();
            System.out.println(xyz[axis]); 
            
            SleepUtil.sleepMillis(20);
        }        
    }   
}
