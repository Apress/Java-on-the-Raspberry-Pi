package org.gaf.pimu.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import org.gaf.pimu.AccelMag;

/**
 * Tests AccelMag
 */
public class TestAccelMag {

    public static void main(String[] args) throws IOException, InterruptedException {
        // set up queue
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        
        try (AccelMag am = new AccelMag(18, queue)) {
            
            am.begin();
           
            am.activateRaw();
            
            for (int cnt = 0; cnt < 100; cnt++) {
                long[] sample = (long[]) queue.take();
                System.out.println(sample[0] + ", " + sample[1]/100000);
            }
            
            am.deactivate();
        } finally {
            Diozero.shutdown();
        }
    }    
}
