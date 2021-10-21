package org.gaf.pimu;

import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * Provides access to the Freescale FXAS21002C gyroscope.
 */
public class FXAS21002C implements AutoCloseable {

    private static final int FXAS21002C_ADDRESS = 0x21; 
    private static final byte FXAS21002C_ID = (byte) 0xD7;

    // the device used for communication
    private I2CDevice device = null;
    
    /**
     * Constructs the FXAS21002C.
     * <p>
     * Creates the diozero I2CDevice for the FXAS21002C and checks 
     * the "who am I" register.
     * <p>
     *
     * @throws IOException if fails due to I/O or identity errors
     */
    public FXAS21002C() throws IOException {
        try {
            // create an I2C device for an FXAS21002C
            device = I2CDevice.builder(FXAS21002C_ADDRESS).build();

            // check the "who am I" register         
            byte whoID = device.readByteData(Registers.WHO_AM_I.register);
            System.out.format("who am I: 0x%2x%n", whoID);
            if (whoID != FXAS21002C_ID) {
                throw new IOException("FXAS21002C not found at address " +
                        FXAS21002C_ADDRESS);
            }
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }
    }

    /**
     * Close an instance.
     */
    @Override
    public void close() {
        System.out.println("FXAS close");
        if (device != null) device.close();
    } 
    
    /**
     * Configures and activates the FXAS21002C.
     *
     * @param lpfCutoff the low pass filter cutoff desired
     * @param odr the output data rate desired
     * @throws RuntimeIOException
     */
    public void begin(LpfCutoff lpfCutoff, ODR odr) throws RuntimeIOException {
        // reset
        device.writeByteData(Registers.CTRL_REG1.register, 
                PowerState.StandBy.state);
        try {
            device.writeByteData(Registers.CTRL_REG1.register, 
                    PowerState.Reset.state);
        } catch (RuntimeIOException ex) { // this causes exception because device does not ACK
            // expected so do nothing
        }

        // go to standby state so can change other control registers
        device.writeByteData(Registers.CTRL_REG1.register, 
                PowerState.StandBy.state);

        // set the lpf value
        int cntl_reg0 = lpfCutoff.level;
        // set the default full scale range
        cntl_reg0 |= Range.DPS250.rangeCode;
        // write the FSR and LPF cutoff
        device.writeByteData(Registers.CTRL_REG0.register, (byte) cntl_reg0);

        // set data ready interupt 
        device.writeByteData(Registers.CTRL_REG2.register, DATA_READY_INTERRUPT);

        // set the odr value
        int cntl_reg1 = odr.odrCode;
        // write ODR as requested and active state
        cntl_reg1 |= PowerState.Active.state;
        device.writeByteData(Registers.CTRL_REG1.register, (byte) cntl_reg1);   
        // delay for settling
        SleepUtil.sleepMillis(100);
    }
    
    /**
     * Read raw data from the gyroscope, all three axis.
     *
     * @return a array containing the x,y,z axis readings
     * @throws RuntimeIOException
     */
    public int[] readRaw() throws RuntimeIOException {
        // read the data from the device
        byte[] buffer = new byte[6];
        device.readI2CBlockData(Registers.OUT_X_MSB.register, buffer);

        // construct the response as an int[]
        int[] res = new int[3];
        res[0] = (int) (buffer[0] << 8);
        res[0] = res[0] | Byte.toUnsignedInt(buffer[1]);
        res[1] = (int) (buffer[2] << 8);
        res[1] = res[1] | Byte.toUnsignedInt(buffer[3]);
        res[2] = (int) (buffer[4] << 8);
        res[2] = res[2] | Byte.toUnsignedInt(buffer[5]);
        return res;
    }

    /**
     * Read raw data from the gyroscope, Z axis only.
     *
     * @return the z axis reading
     * @throws RuntimeIOException
     */
    public int readRawZ() throws RuntimeIOException {
        // read only the Z axis 
        byte[] buffer = new byte[2];
        device.readI2CBlockData(Registers.OUT_Z_MSB.register, buffer);

        // construct the int data
        int value = buffer[0] << 8;
        value = value | Byte.toUnsignedInt(buffer[1]);       
        return value;
    }   
    
    /**
     * Checks if data for all three axes ready.
     * @param wait if want to wait
     * @return if data ready
     * @throws RuntimeIOException 
     */
    public boolean isXYZReady(boolean wait) throws RuntimeIOException {
        return isDataReady(0x08, wait);
    }
    
    /**
     * Checks if data for Z axis ready.
     * @param wait if want to wait
     * @return if data ready
     * @throws RuntimeIOException 
     */
    public boolean isZReady(boolean wait) throws RuntimeIOException {
        return isDataReady(0x04, wait);
    }

    /**
     * Reads DR_STATUS register and checks status per the type.
     * @param type what status to check (all or Z only)
     * @param wait if want to wait
     * @return if data ready
     * @throws RuntimeIOException 
     */
    private boolean isDataReady(int type, boolean wait) throws RuntimeIOException {
        do {
            byte status = device.readByteData(Registers.DR_STATUS.register);
            if ((status & type) > 0) {
                return true;
            }     
        } while (wait);
        return false;
    }

    
    private enum Registers {
        STATUS(0x00),
        OUT_X_MSB(0x01),
        OUT_X_LSB(0x02),
        OUT_Y_MSB(0x03),
        OUT_Y_LSB(0x04),
        OUT_Z_MSB(0x05),
        OUT_Z_LSB(0x06),
        DR_STATUS(0x07),
        F_STATUS(0x08),
        F_SETUP(0x09),
        F_EVENT(0x0A),
        INT_SOURCE_FLAG(0x0B),
        WHO_AM_I(0x0C),    
        CTRL_REG0(0x0D),
        CTRL_REG1(0x13), 
        CTRL_REG2(0x14), 
        CTRL_REG3(0x15);

        public final int register;

        Registers(int register) {
            this.register = register;
        }
    }    
    
    public enum Range {
        DPS250(250, 3, 0.0078125F),
        DPS500(500, 2, 0.015625F),
        DPS1000(1000, 1, 0.03125F),
        DPS2000(2000, 0, 0.0625F);

        public final int range;
        public final int rangeCode;
        public final float sensitivity;

        Range(int range, int rangeCode, float sensitivity) {
            this.range = range;
            this.rangeCode = rangeCode;
            this.sensitivity = sensitivity;
        }
    }

    public enum ODR {
        ODR_800(800f, 0 << 2),
        ODR_400(400f, 1  << 2),
        ODR_200(200f, 2 << 2),
        ODR_100(100f, 3 << 2),
        ODR_50(50f, 4 << 2),
        ODR_25(25f, 5 << 2),
        ODR_12_5(12.5f, 6 << 2);

        public final float odr;
        public final int odrCode;

        ODR(float odr, int odrCode) {
            this.odr = odr;
            this.odrCode = odrCode;
        }
    }
    
    public enum LpfCutoff {
        Highest(0 << 6),
        Medium(1 << 6),
        Lowest(2 << 6);

        public final int level;

        LpfCutoff(int level) {
            this.level = level;
        }        
    }

    public enum PowerState {
        StandBy(0),
        Ready(1),
        Active(2),
        Reset(0x40);

        public  final int state;

        PowerState(int state) {
            this.state = state;
        }
    } 
    
    /*
    The contents of register CNTL_REG2 (see datasheet) to set up an 
    interrupt:
    -- on data ready (all three axes)
    -- to pin 1
    -- active high
    -- push-pull output driver
    */
    private static final byte DATA_READY_INTERRUPT = 0x0e;   
}
