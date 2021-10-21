package org.gaf.util;

import com.diozero.api.SerialDevice;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods for diozero serial I/O.
 */
public class SerialUtil {
    
    /**
     * Prints the content of DeviceInfo for all serial ports.
     */
    public static void printDeviceInfo() {
        List<SerialDevice.DeviceInfo> devs = SerialDevice.getLocalSerialDevices();
        for (SerialDevice.DeviceInfo di : devs) {
            System.out.println( 
                    "device name = " + di.getDeviceName() + " : " +
                    "device file = " + di.getDeviceFile() + " : " +
                    "description = " + di.getDescription() + " : " +
                    "manufacturer = " + di.getManufacturer()+ " : " +

                    "driver name = " + di.getDriverName() + " : " +
                    "vendor ID = " + di.getUsbVendorId() + " : " +
                    "product ID = " + di.getUsbProductId());            
        }        
    }
    
    /**
     * Finds the device file for all connected USB devices that match the
     * device identity described by the parameters {vendor ID, product ID}.
     * See the javadoc for the diozero SerialDevice class for more 
     * information.
     * @param vendorID the vendor ID for the USB device
     * @param productID the product ID for the USB device
     * @return 
     */
    public static List<String> findDeviceFiles(String vendorID, String productID) {
        ArrayList<String> deviceFiles = new ArrayList<>();

        List<SerialDevice.DeviceInfo> dis = SerialDevice.getLocalSerialDevices();
        for (SerialDevice.DeviceInfo di : dis) {
            if (vendorID.equals(di.getUsbVendorId())) {
                if (productID.equals(di.getUsbProductId())) {
                    deviceFiles.add(di.getDeviceFile());
                }
            }          
        }        
        return deviceFiles;                
    }    
    
    public static void main(String[] args) {
        printDeviceInfo();
    }    
}