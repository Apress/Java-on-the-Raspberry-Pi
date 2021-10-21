package org.gaf.pimu.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import org.gaf.pimu.FXAS21002C;
import org.gaf.pimu.Gyro;

/**
 * Tests Gyro zero offset and dead zone
 */
public class TestGyro_ZO {

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

//            gyro.activateZO();
            gyro.activateZODZ();

            for (int cnt = 0; cnt < 100; cnt++) {
                long[] sample = (long[]) queue.take();
                System.out.println(sample[0] + ", " + sample[1] / 100000);
            }

            gyro.deactivate();
        } finally {
            Diozero.shutdown();
        }
    }
}
