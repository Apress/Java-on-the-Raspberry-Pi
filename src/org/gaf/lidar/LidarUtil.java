/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gaf.lidar;

import java.io.IOException;
import java.util.List;
import org.gaf.util.SerialUtil;

/**
 * Lidar utilities.
 */
public class LidarUtil {
    
    /**
     * Finds the device file for a Lidar Unit. 
     * @param usbVendorId the USB vendor ID
     * @param usbProductId the USB product ID
     * @return device file (null if not found)
     * @throws IOException 
     */
    public static String findDeviceFile(String usbVendorId, String usbProductId) 
            throws IOException {
        // identity verification - phase 1
        List<String> deviceFles = SerialUtil.findDeviceFiles(usbVendorId,
                usbProductId);
        // identity verification - phase 2
        if (!deviceFles.isEmpty()) {
            for (String deviceFile : deviceFles) {
                System.out.println(deviceFile);
                Lidar lidar = new Lidar(deviceFile);
                boolean verified = lidar.verifyIdentity();
                lidar.close();
                if (verified) return deviceFile;
            }
        }    
        return null;        
    }  
}
