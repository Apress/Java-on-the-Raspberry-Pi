package org.gaf.roboclaw;

import com.diozero.api.SerialDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * This class represents the Basicmicro RoboClaw 2x15A DC motor controller
 * connected to a Raspberry Pi via USB.
 * It implements a subset of the commands supported by the device.
 * <p>
 * The primary source of porting assistance came from the RoboClaw.cpp library 
 * for the Arduino, with supplemental assistance from the Roboclaw.cs library
 * for Windows. Both came from the Basicmicro web site.
 * </p>
 */
public class RoboClaw implements AutoCloseable {
    
    private SerialDevice device; // the serial device instance
    private final int address; // device address
    
    private int crc = 0; // for CRC generation; only least significant 2 bytes meaningful
    private int MAX_RETRIES = 2; // maximum number of retries

    /**
     * Constructs a RoboClaw instance connected via USB with the specified
     * address.
     * @param deviceFile device file of USB port
     * @param deviceAddress address to use for RoboClaw (0x80-0x87)
     * 
     * @throws IOException thrown when fails to open the serial port
     */
    public RoboClaw(String deviceFile, int deviceAddress) 
            throws IOException {
        try {
            this.device = new SerialDevice(deviceFile); 
            this.address = deviceAddress;
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }        
    }
    
    /**
     * Close the RoboClaw device.
     */
    @Override
    public void close() {
        if (this.device != null) {
            // stop the motors, just in case
            speedM1M2(0, 0);
            // close SerialDevice
            this.device.close();
            this.device = null;
        }
    }  

    /**
     * Clears the accumulated CRC, readying it for a new CRC calculation.
     */
    private void crcClear() {
        this.crc = 0;
    }

    /**
     * Updates the CRC with a singe byte.
     *
     * @param data byte with which to update the CRC
     */
    private void crcUpdate(byte data) {
        this.crc = this.crc ^ (data << 8); 

        for (int i = 0; i < 8; i++) {
            if ((this.crc & 0x8000) == 0x8000) {
                this.crc = (this.crc << 1) ^ 0x1021;
            } else {
                this.crc <<= 1;
            }
        }
    }

    /**
     * Returns the current value of the CRC.
     * 
     * @return value of CRC
     */
    private int crcGet() {
         return this.crc;
    }
    
    /**
     * Writes a byte buffer and the calculated CRC to the device, and 
     * reads and checks the returned status byte from the device.
     * <p>
     * Will retry the entire operation per the desired number of retries. 
     * Assumes blocking reads.
     * </p>
     * 
     * @param retries number of retries
     * @param readResponse means method should read the response
     * @param data bytes containing parameters
     * @return true if communication successful
     */
    private boolean writeN(int retries, boolean readResponse, byte ... data) {

        do { // retry per desired number
            crcClear();
            try {
                for (byte b : data) { 
                    crcUpdate(b);
                    device.writeByte(b);
                }
                int crcLocal = crcGet();
                device.writeByte((byte) (crcLocal >> 8));
                device.writeByte((byte) crcLocal);
                if (readResponse) {
                    if (device.readByte() == ((byte) 0xFF)) return true;
                }
            } catch (RuntimeIOException ex) {
                // do nothing but retry
            }      
        } while (retries-- != 0);
        return false;        
    }

    /**
     * Writes a byte buffer and the calculated CRC to the device, and 
     * reads and checks the returned status byte from the device.
     * 
     * @param data bytes containing parameters
     * @return true if communication successful
     */
    private boolean writeN(byte ... data) {
        return writeN(MAX_RETRIES, true, data);
    }
    
    /**
     * Writes a command type and reads a given number of 4-byte integers
     * into an array. The signed or unsigned nature of the integers
     * must be interpreted by the caller.
     * 
     * @param commandCode command code for the command
     * @param response array of 4-byte integers 
     * @return true if communication successful
     */
    private boolean readN(int commandCode, int[] response) {

        int trys = MAX_RETRIES;

        do { // retry per desired number
            crcClear();
            try {                
                device.writeByte((byte) address);
                crcUpdate((byte) address);
                device.writeByte((byte) commandCode);
                crcUpdate((byte) commandCode);

                // for each int to be read, get each byte individually
                for (int i = 0; i < response.length; i++) { 
                    byte data = device.readByte();
                    crcUpdate(data);
                    int value = Byte.toUnsignedInt(data) << 24;

                    data = device.readByte();
                    crcUpdate(data);
                    value |= Byte.toUnsignedInt(data) << 16;

                    data = device.readByte();
                    crcUpdate(data);
                    value |= Byte.toUnsignedInt(data) << 8;

                    data = device.readByte();
                    crcUpdate(data);
                    value |= Byte.toUnsignedInt(data);
                    response[i] = value;
                }

                // check the CRC
                int dataI = device.read();
                int crcDevice = dataI << 8;
                dataI = device.read();
                crcDevice |= dataI;
                return ((crcGet() & 0x0000ffff) == (crcDevice & 0x0000ffff));
            } catch (RuntimeIOException ex) {
                // do nothing but retry
            }      
        } while (trys-- != 0);
        return false;                
    } 
    
    /**
     * Writes a command type and reads two bytes into an array.  
     * The nature of the bytes
     * must be interpreted by the caller.
     * 
     * @param commandCode command code for the command
     * @param response array of two bytes
     * @return true if communication successful
     */
    private boolean read2(int commandCode, byte[] response) {

        int trys = MAX_RETRIES;

        do { // retry per desired number
            crcClear();
            try {                
                device.writeByte((byte) address);
                crcUpdate((byte) address);
                device.writeByte((byte) commandCode);
                crcUpdate((byte) commandCode);

                byte data = device.readByte();
                crcUpdate(data);
                response[0] = data; 
                data = device.readByte();
                crcUpdate(data);
                response[1] = data; 

                // check the CRC
                int crcDevice;
                int dataI;
                dataI = device.read();
                crcDevice = dataI << 8;
                dataI = device.read();
                crcDevice |= dataI;

                return ((crcGet() & 0x0000ffff) == (crcDevice & 0x0000ffff));
            } catch (RuntimeIOException ex) {
                // do nothing but retry
            }      
        } while (trys-- != 0);
        return false;                
    }     

    /**
     * Reads a byte with a timeout of the given number of milliseconds.
     * @param timeout
     * @return the byte if successful (in LSB); -1 if unsuccessful
     * @throws RuntimeIOException 
     */
    private int readWithTimeout(int timeout) throws RuntimeIOException {
        int count = 0;
        while(device.bytesAvailable() < 1) {
            SleepUtil.sleepMillis(1);
            if (++count >= timeout) break;            
        }
        if (count >= timeout) return -1;
        else return device.read();
    }

    /**
     * Verifies the identity by sending a "write only" command and waiting for
     * the expected single byte response.
     * @return true if identity verified
     * @throws IOException fails due to I/O errors
     */
    public boolean verifyIdentity() throws IOException {
        try {
            // send a command
            writeN(0, false, (byte) address, (byte) Commands.RESETENC);  
            // read response, if any, waiting for 20 ms
            return readWithTimeout(20) >= 0;        
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }        
    }

    /**
     * Sets the count for the encoder for motor 1.
     *
     * @param count the encoder count (only the lower 32 bits get written to the
     * RoboClaw)
     * @return true if communication successful
     */
    public boolean setEncoderM1(long count) {
        byte[] buffer = new byte[6]; // address + command code + 4 bytes
        buffer[0] = (byte) address;
        buffer[1] = (byte) Commands.SETM1ENCCOUNT;
        insertIntInBuffer((int) count, buffer, 2);
        return writeN(buffer);
    }
    
    /**
     * Resets both motor encoders.
     * 
     * @return true if communication successful
     */
    public boolean resetEncoders() {        
        return writeN((byte) address, (byte) Commands.RESETENC);        
    }

    /**
     * Gets the encoder counts for both motors from the RoboClaw.
     *
     * @param encoderCount array for encoders 
     * @return true if communication successful
     */
    public boolean getEncoders(long[] encoderCount) {
        int[] response = new int[2];
        boolean valid = readN(Commands.GETENCODERS, response);

        if (valid) {
            encoderCount[0] = Integer.toUnsignedLong(response[0]);
            encoderCount[1] = Integer.toUnsignedLong(response[1]);
        }
        return valid;
    }
    
    /**
     * Gets the main battery voltage in 10ths of a volt. 
     * @param voltage the main battery voltage x 10
     * @return true if communication successful
     */
    public boolean getMainBatteryVoltage(int[] voltage) {
        byte[] response = new byte[2];
        boolean ok = read2(Commands.GETMBATT, response);

        if (ok) {
            int value = Byte.toUnsignedInt(response[0]) << 8;
            value |= Byte.toUnsignedInt(response[1]);
            voltage[0] = value;
        }
        return ok;
    }    

    /**
     * Sets the velocity PID constants.
     *
     * @param commandCode determines which motor
     * @param velocityPID
     * @return true if communication successful
     */   
    private boolean setVelocityPID(int commandCode, VelocityPID velocityPID) {
        byte[] buffer = new byte[18];

        // calculate the integer values for device
        int kPi = (int) (velocityPID.kP * 65536);
        int kIi = (int) (velocityPID.kI * 65536);
        int kDi = (int) (velocityPID.kD * 65536);

        // insert parameters into buffer
        buffer[0] = (byte) address;
        buffer[1] = (byte) commandCode;
        insertIntInBuffer(kDi, buffer, 2);
        insertIntInBuffer(kPi, buffer, 6);
        insertIntInBuffer(kIi, buffer, 10);
        insertIntInBuffer(velocityPID.qPPS, buffer, 14);
        return writeN(buffer);       
    }

    /**
     * Sets the velocity PID constants for motor 1.
     *
     * @param velocityPID
     * @return true if communication successful
     */   
    public boolean setM1VelocityPID(VelocityPID velocityPID) {     
        return setVelocityPID(Commands.SETM1PID, velocityPID);       
    }

    /**
     * Sets the velocity PID constants for motor 2.
     *
     * @param velocityPID
     * @return true if communication successful
     */   
    public boolean setM2VelocityPID(VelocityPID velocityPID) {     
        return setVelocityPID(Commands.SETM2PID, velocityPID);       
    }
    
    /**
     * Reads the PID constants for velocity control for a motor.
     *
     * @param commandCode determines which motor
     * @param velocityPID
     * @return true if communication successful
     */
    private boolean getVelocityPID(int commandCode, VelocityPID velocityPID) {
        int[] response = new int[4];
        boolean valid = readN(commandCode, response);

        if (valid) {
            velocityPID.kP = ((float) response[0]) / 65536f;
            velocityPID.kI = ((float) response[1]) / 65536f;
            velocityPID.kD = ((float) response[2]) / 65536f;
            velocityPID.qPPS = response[3];
        }
        return valid;
    }
    
    /**
     * Reads the PID constants for velocity control for motor 1.
     *
     * @param velocityPID
     * @return true if communication successful
     */
    public boolean getM1VelocityPID(VelocityPID velocityPID) {
        return getVelocityPID(Commands.READM1PID, velocityPID);
    }

    /**
     * Reads the PID constants for velocity control for motor 2.
     *
     * @param velocityPID
     * @return true if communication successful
     */
    public boolean getM2VelocityPID(VelocityPID velocityPID) {
        return getVelocityPID(Commands.READM2PID, velocityPID);
    }

    /**
     * Drives both motors with a signed speed.
     * @param speedM1 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param speedM2 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @return true if communication successful
     */
    public boolean speedM1M2(int speedM1, int speedM2) {
        byte[] buf = new byte[10];

        // insert parameters into buffer
        buf[0] = (byte) address;
        buf[1] = (byte) Commands.MIXEDSPEED;
        insertIntInBuffer(speedM1, buf, 2);
        insertIntInBuffer(speedM2, buf, 6);

        return writeN(buf);                      
    }
    
    /**
     * Drives motors with an unsigned acceleration and a signed speed for
     * each motor.
     * @param acceleration is in quadrature pulses per second per second
     * @param speedM1 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param speedM2 is in quadrature pulses per second; a negative number
     * @return true if communication successful
     */
    public boolean speedAccelM1M2(long acceleration, int speedM1, int speedM2) {
        byte[] buf = new byte[14];

        // insert parameters into buffer
        buf[0] = (byte) address;
        buf[1] = (byte) Commands.MIXEDSPEEDACCEL;
        insertIntInBuffer((int) acceleration, buf, 2);
        insertIntInBuffer(speedM1, buf, 6);
        insertIntInBuffer(speedM2, buf, 10);

        return writeN(buf);       
    }
    
    /**
     * Drives motors with signed speed and unsigned distance; this can be
     * buffered. See the User's Manual for additional detail.
     *
     * @param speedM1 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param distanceM1 is in quadrature pulses
     * @param speedM2 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param distanceM2 is in quadrature pulses
     * @param buffer indicates buffer or execute immediately
     * @return true if communication successful
     */
    public boolean speedDistanceM1M2(int speedM1,
            long distanceM1, int speedM2, long distanceM2, boolean buffer) {
        byte[] buf = new byte[19];

        // insert parameters into buffer
        buf[0] = (byte) address;
        buf[1] = (byte) Commands.MIXEDSPEEDDIST;
        insertIntInBuffer(speedM1, buf, 2);
        insertIntInBuffer((int) distanceM1, buf, 6);
        insertIntInBuffer(speedM2, buf, 10);
        insertIntInBuffer((int) distanceM2, buf, 14);
        buf[18] = (buffer) ? (byte) 0 : 1;

        return writeN(buf);        
    }
    
    /**
     * Drives motors with signed speed and unsigned acceleration and distance
     * for each motor; this can be buffered. 
     * See the User's Manual for additional detail.
     *
     * @param acceleration is in quadrature pulses per second per second
     * @param speedM1 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param distanceM1 is in quadrature pulses
     * @param speedM2 is in quadrature pulses per second; a negative number
     * drives backward; 0 is stop; a positive number drives forward
     * @param distanceM2 is in quadrature pulses
     * @param buffer indicates buffer or execute immediately
     * @return true if communication successful
     */
    public boolean speedAccelDistanceM1M2(long acceleration, int speedM1,
            long distanceM1, int speedM2, long distanceM2, boolean buffer) {
        byte[] buf = new byte[23];

        // insert parameters into buffer
        buf[0] = (byte) address;
        buf[1] = (byte) Commands.MIXEDSPEEDACCELDIST;
        insertIntInBuffer((int) acceleration, buf, 2);
        insertIntInBuffer(speedM1, buf, 6);
        insertIntInBuffer((int) distanceM1, buf, 10);
        insertIntInBuffer(speedM2, buf, 14);
        insertIntInBuffer((int) distanceM2, buf, 18);
        buf[22] = (buffer) ? (byte) 0 : 1;

        return writeN(buf);              
    }

    /**
     * Inserts a 4 byte value into byte array as 4 bytes, most significant byte
     * first. The buffer is assumed to be large enough for the bytes to be
     * added.
     *
     * @param value 32 bit value to insert
     * @param buffer byte array into which to insert the value
     * @param start location in byte array to start insertion
     */
    private void insertIntInBuffer(int value, byte[] buffer, int start) {
        buffer[start] = (byte) (value >>> 24);
        buffer[start + 1] = (byte) (value >>> 16);
        buffer[start + 2] = (byte) (value >>> 8);
        buffer[start + 3] = (byte) (value);
    }
    
    private class Commands {
//        static final int GETM1ENC = 16;
        static final int RESETENC = 20;
        static final int SETM1ENCCOUNT = 22;
        static final int GETMBATT = 24;
        static final int SETM1PID = 28;
        static final int SETM2PID = 29;
        static final int MIXEDSPEED = 37;
        static final int MIXEDSPEEDACCEL = 40;
        static final int MIXEDSPEEDDIST = 43;
        static final int MIXEDSPEEDACCELDIST = 46;
        static final int READM1PID = 55;
        static final int READM2PID = 56;
        static final int GETENCODERS = 78;        
    }   
    
    /**
     * A class containing constants for velocity control.
     */
    public static class VelocityPID {
        
        /**
         * The proportional constant.
         */
        public float kP;
        /**
         * The integral constant.
         */
        public float kI;
        /**
         * The derivative constant.
         */
        public float kD;
        /**
         * The quadrature pulses per second constant.
         */
        public int qPPS;

        /**
         * Default constructor.
         */
        public VelocityPID() {            
        }
        
        /**
         * Parameterized constructor.
         * @param kP
         * @param kI
         * @param kD
         * @param qPPS 
         */
        public VelocityPID(float kP, float kI, float kD, int qPPS) {
            this.kP = kP;
            this.kI = kI;
            this.kD = kD;
            this.qPPS = qPPS;
        }
        
        @Override
        public String toString() {
            return "Velocity PID kP: " + kP + "  kI: " + kI + "  kD: " + kD + 
                    "  qpps: " + qPPS;
        }
    }
}
