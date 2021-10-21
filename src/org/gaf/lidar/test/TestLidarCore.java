package org.gaf.lidar.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import org.gaf.lidar.Lidar;
import org.gaf.lidar.LidarUtil;

/**
 * Test the core Lidar implementation.
 */
public class TestLidarCore extends Lidar {
    
    public TestLidarCore(String fileName) throws IOException {
        super(fileName);
    }

    public static void main(String arg[]) throws IOException {
        final short parm = 1298;
        // identity verification
        String deviceFile = LidarUtil.findDeviceFile("1ffb", "2300");    

        if (deviceFile == null) {
            throw new IOException("No matching device!");
        }    
        
        try (TestLidarCore tester = new TestLidarCore(deviceFile)) {
            // issue and check echo command
            short echo = tester.echoParameter(parm);
            if (echo == parm)
                System.out.println("Echo GOOD");
            else
                System.out.println("Echo BAD");            
        } finally {
            Diozero.shutdown();
        }       
    }    
}
