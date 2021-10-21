package org.gaf.io.test;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;

/**
 * Play with I2C using the BME280.
 */
public class PlayI2C {
    
    private static I2CDevice device = null;

    /**
     * Tests various I/O operations with I2C: read a byte; read a byte block;
     * write a byte.
     * @param args 
     */
    public static void main(String[] args) {
        device = new I2CDevice(I2CConstants.CONTROLLER_1, 0x76);

        // 1: test read a register
        byte reg = device.readByteData(0xd0);
        System.out.format("ID=0x%02X%n", reg);

        // 2: test read register block 
        byte[] ret = new byte[7];
        device.readI2CBlockData(0xe1, ret);
        System.out.print("cal");
        for (int i = 0; i < 7; i++) {
            System.out.format(" %d=0x%02X ", i, ret[i]);
        }
        System.out.println();
        reg = device.readByteData(0xe1);
        System.out.format("cal 0=0x%02X%n", reg);
        reg = device.readByteData(0xe7);
        System.out.format("cal 6=0x%02X%n", reg);

        // 3: test write a register
        reg = device.readByteData(0xf4);
        System.out.format("reg before=0x%02X%n", reg);
        device.writeByteData(0xf4, (byte)0x55); 
        reg = device.readByteData(0xf4);
        System.out.format("reg after=0x%02X%n", reg);

        // reset
        device.writeByteData(0xe0, (byte)0xb6);

        // close       
        device.close();
    }
}
