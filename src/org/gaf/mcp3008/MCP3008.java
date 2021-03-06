package org.gaf.mcp3008;

import com.diozero.api.RuntimeIOException;
import static com.diozero.api.SpiConstants.DEFAULT_SPI_CONTROLLER;
import com.diozero.api.SpiDevice;
import java.io.IOException;

/**
 * Represents an MCP3008 analog to digital converter.
 */
public class MCP3008 implements AutoCloseable {
    
    private SpiDevice device = null;
    private final float vRef;
    
    /**
     * Creates an instance using the desired chip select and reference voltage.It accepts the diozero default SPI controller and byte ordering.
     * It
 sets an SPI clock frequency to 1.35MHz.
     * @param chipSelect the chip select pin
     * @param vRef the reference voltage
     * @throws java.io.IOException
     */
    public MCP3008(int chipSelect, float vRef) throws IOException {
        this(DEFAULT_SPI_CONTROLLER, chipSelect, vRef);
    }

    /**
     * Creates an instance using the desired SPI controller, chip select,
     * and reference voltage. It accepts the diozero default byte ordering.
     * It sets an SPI clock frequency to 1.35MHz.
     * @param controller the SPI controller
     * @param chipSelect the chip select pin
     * @param vRef the reference voltage
     * @throws java.io.IOException
     */
    public MCP3008(int controller, int chipSelect, float vRef) throws IOException {
        try {device = SpiDevice.builder(chipSelect).setController(controller).
                setFrequency(1_350_000).build();
            this.vRef = vRef;
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }
    }

    /**
     * Close the device.
     */
    @Override
    public void close() {
//        System.out.println("close");
        if (device != null) {
            device.close();
            device = null;
        }
    }   
    
    /**
     * Reads a channel and returns the raw value.
     * @param channel channel to read
     * @return raw sample value
     * @throws RuntimeIOException
     */
    public int getRaw(int channel) throws RuntimeIOException {
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
     * Reads a channel and returns its value as a fraction of full scale. 
     * @param channel channel to read
     * @return sample value as a fraction of full scale
     * @throws RuntimeIOException
     */
    public float getFSFraction(int channel) throws RuntimeIOException {
        int raw = getRaw(channel);
        float value = raw / (float) 1024;
        return value;
    }
    
    /**
     * Reads a channel and returns its value as a voltage based on the
     * reference voltage.
     * @param channel channel to read
     * @return sample value as a voltage 
     * @throws RuntimeIOException
     */
    public float getVoltage(int channel)  throws RuntimeIOException {
        return (getFSFraction(channel) * vRef);
    }    
}
