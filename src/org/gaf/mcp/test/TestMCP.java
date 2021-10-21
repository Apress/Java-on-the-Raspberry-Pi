package org.gaf.mcp.test;

import com.diozero.api.SpiConstants;
import com.diozero.api.SpiDevice;

/**
 * Test reading MCP3008: 
 * <ul>
 * <li>per diozero implementation (section 5 of datasheet)
 * </li>
 * <li>per section 6.1 of datasheet
 * </li>
 * </ul>
 */
public class TestMCP {

    private static SpiDevice device = null;

    public static void main(String[] args) {
        // use CE1; frequency = 1.35MHz        
        device = SpiDevice.builder(SpiConstants.CE1).
                setFrequency(1_350_000).build();

        // read the first 5 channels
        int[] value = new int[5];
        for  (int i = 0; i < 5; i++) {
            value[i] = getValueD(i);
//            value[i] = getValueM(i);
        }

        // print the information
        for (int i = 0; i < 5; i++) {
            System.out.format("C%1d = %4d, %.2f FS, %.2fV %n", i, value[i], 
                    getFS(value[i]), getVoltage(value[i], 3.3f));
        }

        device.close();
    }
    
    /**
     * Read a sample per datasheet section 5.
     * @param channel channel number
     * @return raw value from channel
     */
    private static int getValueD(int channel) {
        // create start bit & channel code; assume single-ended
        byte code = (byte) ((channel | 0x18));
        // first byte: start bit, single ended, channel
        // second and third bytes create total of 3 frames
        byte[] tx = {code, 0, 0};
        byte[] rx = device.writeAndRead(tx);

        int lsb = rx[2] & 0xf0;
        int msb = rx[1] << 8;
        int value = ((msb | lsb) >>> 4) & 0x3ff;
        
        return value;
    }

    /**
     * Read a sample per datasheet section 6.1.
     * @param channel channel number
     * @return raw value from channel
     */
    private static int getValueM(int channel) {
        // create channel code; assume single-ended
        byte code = (byte) ((channel << 4) | 0x80);
        // first byte has start bit
        // second byte says single-ended, channel
        // third byte for creating third frame
        byte[] tx = {(byte)0x01, code, 0};
        byte[] rx = device.writeAndRead(tx);

        int lsb = rx[2] & 0xff;
        int msb = rx[1] & 0x03;
        int value = (msb << 8) | lsb;

        return value;
    }
    
    /**
     * Calculates the percentage of the full scale value for a raw sample.
     * @param value raw sample
     * @return percentage of full scale
     */
    private static float getFS(int value) {
        float fs = ((float)value / 1024f);        
        return fs;
    }   

    /**
     * Calculates the voltage for a channel.
     * @param value percent of full scale
     * @param vRef full scale voltage
     * @return voltage for a channel
     */
    private static float getVoltage(int value, float vRef) {
        float voltage = ((float)value / 1024f) * vRef;       
        return voltage;
    }    
}
