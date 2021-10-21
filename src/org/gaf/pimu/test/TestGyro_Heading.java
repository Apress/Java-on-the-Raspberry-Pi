package org.gaf.pimu.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import org.gaf.pimu.FXAS21002C;
import org.gaf.pimu.Gyro;

/**
 * Tests Gyro heading delivery
 */
public class TestGyro_Heading {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // set up queue
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);

        try ( Gyro gyro = new Gyro(18, queue)) {

            gyro.begin(FXAS21002C.LpfCutoff.Lowest, FXAS21002C.ODR.ODR_50);

            System.out.println("\n... Calculating offset ...\n");
            gyro.calcZeroOffset(4000);

            gyro.activateHeading(FXAS21002C.Range.DPS250);

            for (int cnt = 0; cnt < 5000; cnt++) {
                float heading = (float) queue.take();
                System.out.println(heading);
            }

            gyro.deactivate();
        } finally {
            Diozero.shutdown();
        }
    }
}
