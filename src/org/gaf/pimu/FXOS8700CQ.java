package org.gaf.pimu;

import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * Provides access to the Freescale FXOS8700CQ accelerometer/magnetometer.
 */
public class FXOS8700CQ implements AutoCloseable {

    private static final int FXOS8700CQ_ADDRESS = 0x1F; 
    private static final byte FXOS8700CQ_ID = (byte) 0xC7; 

    // the device used for communication
    private I2CDevice device = null;

    /**
     * Creates an FXOS8700CQ instance.
     * @throws IOException if fails due to I/O or identity errors
     */
    public FXOS8700CQ() throws IOException {
        try {
            // create an I2C device for an FXOS8700CQ
            device = I2CDevice.builder(FXOS8700CQ_ADDRESS).build();

            // check the "who am I" register         
            byte whoID = device.readByteData(Registers.WHO_AM_I.register);
            System.out.format("who am I: %2x%n", whoID);
            if (whoID != FXOS8700CQ_ID) {
                throw new IOException("FXOS8700CQ not found at address " +
                        FXOS8700CQ_ADDRESS);
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
        System.out.println("FXOS close");
        if (device != null) device.close();
    }
 
    /**
     * Configures the FXOS8700CQ and activates the device.
     * @throws RuntimeIOException 
     */
    public void begin() throws RuntimeIOException {
        // reset    
        device.writeByteData(Registers.CTRL_REG1.register, 
                PowerState.StandBy.stateCode);
        try {
            device.writeByteData(Registers.CTRL_REG2.register, RESET);
        } catch (RuntimeIOException ex) { // this causes exception because device does not ACK
            // expected so do nothing
        }
        // delay for settling
        SleepUtil.sleepMillis(10);
              
                
        // go to standby state so can change other control registers
        device.writeByteData(Registers.CTRL_REG1.register, 
                PowerState.StandBy.stateCode);
        
        // configure a data ready interrupt
        device.writeByteData(Registers.CTRL_REG3.register, INTERRUPT_HIGH_PP);
        device.writeByteData(Registers.CTRL_REG4.register, INTERRUPT_DATA_READY);
        device.writeByteData(Registers.CTRL_REG5.register, INTERRUPT_PIN1);

        // set up high res OSR for magnetometer, and hybrid mode
        int m_ctrl_reg1 = MagOSR.R7.osrCode | OperatingMode.Hybrid.mode;
        System.out.println("m_ctrl_reg1 = " + String.format("0x%02X", m_ctrl_reg1));
        device.writeByteData(Registers.M_CTRL_REG1.register, m_ctrl_reg1);

        // set address increment to read ALL registers in a block
        device.writeByteData(Registers.M_CTRL_REG2.register, 
                HYBRID_AUTO_INC);

        // set accel sensitivity and no HPF
        device.writeByteData(Registers.XYZ_DATA_CFG.register, 
                AccelRange.RANGE_2G.rangeCode);

        // set up high res OSR for accelerometer
        device.writeByteData(Registers.CTRL_REG2.register, 
                AccelOSR.HighResolution.osrCode);

        // set ODR, normal read speed, and device ACTIVE
        int cntl_reg1 = ODR.ODR_100.odrCode | NoiseMode.Reduced.noiseCode |
                ReadSpeed.Normal.speedCode | PowerState.Active.stateCode;
        System.out.println("ctrl_reg1 = " + String.format("0x%02X", cntl_reg1));
        device.writeByteData(Registers.CTRL_REG1.register, cntl_reg1);
    }

    /**
     * Reads raw data for all three axes of the accelerometer and magnetometer.
     * @return array with magnetometer XYZ then accelerometer XYZ
     * @throws RuntimeIOException 
     */
    public int[] readRaw() throws RuntimeIOException {
        // read the data from the device
        byte[] buffer = new byte[12];
        device.readI2CBlockData(Registers.M_OUT_X_MSB.register, buffer);

        // construct the response as an int[]
        int[] res = new int[6];        
        // magnetometer
        res[0] = (int) (buffer[0] << 8);
        res[0] = res[0] | Byte.toUnsignedInt(buffer[1]);
        res[1] = (int) (buffer[2] << 8);
        res[1] = res[1] | Byte.toUnsignedInt(buffer[3]);
        res[2] = (int) (buffer[4] << 8);
        res[2] = res[2] | Byte.toUnsignedInt(buffer[5]);
        // accelerometer
        res[3] = (int) (buffer[6] << 8);
        res[3] = ((res[3] | Byte.toUnsignedInt(buffer[7])) << 18) >> 18 ;
        res[4] = (int) (buffer[8] << 8);
        res[4] = ((res[4] | Byte.toUnsignedInt(buffer[9])) << 18) >> 18 ;
        res[5] = (int) (buffer[10] << 8);
        res[5] = ((res[5] | Byte.toUnsignedInt(buffer[11])) << 18) >> 18 ;

        return res;
    }

    /**
     * Checks if data ready for all 6 axes. 
     * @param wait if want to wait
     * @return if data ready
     * @throws RuntimeIOException 
     */
    public boolean isDataReady(boolean wait) throws RuntimeIOException {
        do {
            byte status = device.readByteData(Registers.M_DR_STATUS.register);
            if ((status & 0x08) > 0) {
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

        WHO_AM_I(0x0D),
        XYZ_DATA_CFG(0x0E),

        CTRL_REG1(0x2A),
        CTRL_REG2(0x2B),
        CTRL_REG3(0x2C),
        CTRL_REG4(0x2D),
        CTRL_REG5(0x2E),
        OFF_X(0x2F),
        OFF_Y(0x30),
        OFF_Z(0x31),
        M_DR_STATUS(0x32),
        M_OUT_X_MSB(0x33),
        M_OUT_X_LSB(0x34),
        M_OUT_Y_MSB(0x35),
        M_OUT_Y_LSB(0x36),
        M_OUT_Z_MSB(0x37),
        M_OUT_Z_LSB(0x38),

        M_CTRL_REG1(0x5B),
        M_CTRL_REG2(0x5C),
        M_CTRL_REG3(0x5D);

        public final int register;

        Registers(int code) {
            this.register = code;
        }
    }

    public enum AccelRange
    {
        RANGE_2G(0x00),
        RANGE_4G(0x01),
        RANGE_8G(0x02);

        public final int rangeCode;

        AccelRange(int rangeCode) {
            this.rangeCode = rangeCode;
        }
    }

    public enum ODR {
        ODR_800(0 << 3),
        ODR_400(1 << 3),
        ODR_200(2 << 3),
        ODR_100(3 << 3),
        ODR_50(4 << 3),
        ODR_12_5(5 << 3),
        ODR_06_25(6 << 3),
        ODR_01_56(7 << 3);

        public final int odrCode;

        ODR(int odrCode) {
            this.odrCode = odrCode;
        }
    }
    
    public enum ReadSpeed {
        Normal(0 << 1),
        Fast(1 << 1);
        
        public final int speedCode;

        private ReadSpeed(int speedCode) {
            this.speedCode = speedCode;
        }
    }

    public enum NoiseMode {
        Normal(0 << 2),
        Reduced(1 << 2);
        
        public final int noiseCode;

        private NoiseMode(int noiseCode) {
            this.noiseCode = noiseCode;
        }
    }

    public enum AccelOSR {
        Normal(0),
        LowNoiseLowPower(1),
        HighResolution(2),
        LowPower(3);

        public final int osrCode;

        AccelOSR(int osrCode) { this.osrCode = osrCode; }
    }

    public enum PowerState {
        StandBy(0),
        Active(1);

        public final int stateCode;

        PowerState(int stateCode) {
            this.stateCode = stateCode;
        }
    }

    public enum OperatingMode {
        OnlyAccelerometer(0),
        OnlyMagnetometer(1),
        Hybrid(3);

        public final int mode;

        OperatingMode(int mode) { this.mode = mode; }
    }
    
    private static final int HYBRID_AUTO_INC = 0x20;
    private static final int RESET = 0x40;
    
    public enum MagOSR {
        R0(0 << 2),
        R1(1 << 2),
        R2(2 << 2),
        R3(3 << 2),
        R4(4 << 2),
        R5(5 << 2),
        R6(6 << 2),
        R7(7 << 2);

        public final int osrCode;

        MagOSR(int osrCode) { this.osrCode = osrCode; }
    }

    /*
    The contents of register CNTL_REG3 (see datasheet) to set up an 
    interrupt:
    -- active high
    -- push-pull output driver
    */
    private static final byte INTERRUPT_HIGH_PP = 0x02;
    /*
    The contents of register CNTL_REG4 (see datasheet) to set up an 
    interrupt:
    -- on data ready
    */
    private static final byte INTERRUPT_DATA_READY = 0x01;
    /*
    The contents of register CNTL_REG5 (see datasheet) to set up an 
    interrupt:
    -- to pin 1
    */
    private static final byte INTERRUPT_PIN1 = 0x01;
}
