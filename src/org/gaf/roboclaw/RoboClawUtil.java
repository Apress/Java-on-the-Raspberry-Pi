package org.gaf.roboclaw;

import java.io.IOException;
import java.util.List;
import org.gaf.util.SerialUtil;

/**
 * A RoboClaw utility class.
 * 
 */
public class RoboClawUtil {
    
    /**
     * Implements the two phases of identity verification for the RoboClaw.
     * @param usbVendorId vendor ID of RoboClaw USB device
     * @param usbProductId product  ID of RoboClaw USB device
     * @param instanceId internal address of RoboClaw instance
     * @return device file for RoboClaw instance or null
     * @throws IOException if communication fails
     */
    public static String findDeviceFile(String usbVendorId, String usbProductId,
            int instanceId) throws IOException {
        // identity verification - phase 1
        List<String> deviceFles = SerialUtil.findDeviceFiles(usbVendorId,
                usbProductId);
        // identity verification - phase 2
        if (!deviceFles.isEmpty()) {
            for (String deviceFile : deviceFles) {
                System.out.println(deviceFile);
                RoboClaw claw = new RoboClaw(deviceFile, instanceId);
                boolean verified = claw.verifyIdentity();
                claw.close();
                if (verified) return deviceFile;                
            }
        }    
        return null;        
    }  
}
