    package org.gaf.io.test;

import com.diozero.api.SpiConstants;
import com.diozero.api.SpiDevice;

/**
 * Perform some tests of the BME280 using SPI.
 */
public class PlayReal {

    private static SpiDevice device = null;
    /**
     * Determines the startup time and the average status read time.
     * @param args 
     */
    public static void main(String[] args) {
        device = SpiDevice.builder(SpiConstants.CE0).
                setFrequency(1_000_000).build();

        // reset
        writeByte(0xe0, (byte)0xb6);      
        long tStart = System.nanoTime();

        int cnt = 1;
        while (readByte(0xf3) == 0x01) { // copy not done      
            cnt++;
        }

        long tEnd = System.nanoTime();

        long deltaT = (tEnd  - tStart) / 1000;

        System.out.println("Startup time = " + deltaT + " micros" );

        System.out.println("Status read iterations = " + cnt + 
                "; Iteration duration = " + (deltaT/cnt) + " micros");

        // close
        device.close();
    }
    
    private static byte readByte(int address) {
            byte[] tx = {(byte) (address | 0x80), 0};           
            byte[] rx = device.writeAndRead(tx);
            
            return rx[1];        
    }
    
    private static void writeByte(int address, byte value) {
        byte[] tx = new byte[2];
        tx[0] = (byte) (address & 0x7f); // msb must be 0
        tx[1] = value;

        device.write(tx);
    }       
}
