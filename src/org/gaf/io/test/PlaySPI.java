package org.gaf.io.test;

import com.diozero.api.SpiDevice;
import com.diozero.api.SpiConstants;

/**
 * Play with SPI using the BME280.
 */
public class PlaySPI {
    
    private static SpiDevice device = null;

    /**
     * Tests various I/O operations with SPI: read a byte; read a byte block;
     * write a byte.
     * @param args 
     */
    public static void main(String[] args) {
        device = new SpiDevice(SpiConstants.CE0);

        // 1: test read a register
        byte reg = readByte(0xd0);
        System.out.format("ID=0x%02X%n", reg);

        // 2: test read register block 
        byte[] ret = readByteBlock(0xe1, 7);
        System.out.print("cal");
        for (int i = 0; i < 7; i++) {
            System.out.format(" %d=0x%02X ", i, ret[i]);
        }
        System.out.println();
        reg = readByte(0xe1);
        System.out.format("cal 0=0x%02X%n", reg);
        reg = readByte(0xe7);
        System.out.format("cal 6=0x%02X%n", reg);

        // 3: test write a register
        reg = readByte(0xf4);
        System.out.format("reg before=0x%02X%n", reg);

        writeByte(0xf4, (byte)0x55);

        reg = readByte(0xf4);
        System.out.format("reg after=0x%02X%n", reg);

        // reset
        writeByte(0xe0, (byte)0xb6);      

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
    
    private static byte[] readByteBlock(int address, int length) {
        byte[] tx = new byte[length + 1];
        tx[0] = (byte) (address | 0x80);
        /* NOTE: array initialized to 0 */

        byte[] rx = device.writeAndRead(tx);

        byte[] data = new byte[length];
        System.arraycopy(rx, 1, data, 0, length);

        return data;
    }
}
