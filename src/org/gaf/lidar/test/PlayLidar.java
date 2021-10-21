package org.gaf.lidar.test;

import static com.diozero.api.SerialConstants.*;
import com.diozero.api.SerialDevice;

/**
 * Play with Lidar Unit. Get the ID and echo a parameter.
 */
public class PlayLidar {

    public static void main(String[] args) throws InterruptedException {
        SerialDevice device = new SerialDevice("/dev/ttyACM0", BAUD_115200, 
                    DEFAULT_DATA_BITS, DEFAULT_STOP_BITS, DEFAULT_PARITY);
        
        // get the ID        
        device.writeByte((byte) 10);
        byte[] res = new byte[2];
        
        // see if active
        Thread.sleep(100);
        if (!(device.bytesAvailable() > 1)) {
            System.out.println("Lidar not powered!");
            System.exit(-1);
        }
        
        // read the response byte array
        device.read(res);           
        // construct response as short
        short value = (short)(res[0] << 8);
        value = (short) (value | (short) Byte.toUnsignedInt(res[1]));
        System.out.println("ID= " + value);
        
        // echo a parameter
        short parameter = 12345;
        device.write((byte) 11, (byte) (parameter >> 8), (byte) parameter);        
        // read the response byte array
        device.read(res);           
        // construct response as short
        value = (short)(res[0] << 8);
        value = (short) (value | (short) Byte.toUnsignedInt(res[1]));
        System.out.println("Parameter= " + value);
    }    
}
