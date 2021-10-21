package org.gaf.lidar;

import static com.diozero.api.SerialConstants.BAUD_115200;
import com.diozero.api.SerialDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * This class represents a Lidar Unit connected to a Raspberry Pi via USB.
 * It produces a 180 degree scan with range readings at 0.5 degree intervals 
 * (approximately).
 */
public class Lidar implements AutoCloseable {
    
    /**
     * The ID of the Lidar Unit. 
     */
    public static final int LIDAR_ID = 600; // known ID
    
    private SerialDevice device; // the Lidar serial device
    
    /**
     * Creates a Lidar instance with serial characteristics appropriate for 
     * the Lidar Unit. 
     * @param deviceFile the OS device file for the Lidar Unit
     * @throws IOException if fails due to I/O or identity errors
     */
    public Lidar(String deviceFile) throws IOException {
        // open a serial port at baudrate 115200; other parameter are defaults        
        try {
            device = SerialDevice.builder(deviceFile).
                setBaud(BAUD_115200).build();

        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }
    }
    
    /**
     * Close an instance.
     */
    @Override
    public void close() {
        if (device != null) {
            device.close();
            device = null;
        }
    } 
    
    /**
     * Verifies the identity using the "Get ID" command and checking
     * the response.
     * @return true if identity verified
     * @throws RuntimeIOException 
     * @throws java.io.IOException if no power
     */
    public boolean verifyIdentity() throws RuntimeIOException, IOException {
        return LIDAR_ID == getID();            
    }

    /**
     * Returns the device ID.
     * @return device ID
     * @throws RuntimeIOException 
     * @throws java.io.IOException if no power
     */
    protected short getID() throws RuntimeIOException, IOException {
        writeCmdType(CommandTypes.ID.code);

        SleepUtil.sleepMillis(100);
        if (!(device.bytesAvailable() > 1)) {
            throw new IOException("Lidar not powered!");
        }
        return readShort();
    }
    
    /**
     * Echos the parameter.
     * @param parm an arbitrary parameter
     * <p>
     * This method is used primarily for functional verification.
     * </p>
     * @return the parameter to echo
     * @throws RuntimeIOException 
     */
    protected short echoParameter(short parm) throws RuntimeIOException {
        writeCmdTypeParm(CommandTypes.ECHO.code, parm);
        return readShort();
    }
    
    /**
     * Sets the angular position of the servo rotating the Lidar sensor.
     * @param positionHalfDeg the position in 0.5 degrees; e.g. 
     * 180 = 90 degrees.
     * <p>
     * This method is primarily intended for testing and calibration.
     * </p>
     * @return -1 on failure, else the parameter
     * @throws RuntimeIOException
     */
    protected int setServoPosition(int positionHalfDeg) throws RuntimeIOException {
        writeCmdTypeParm(CommandTypes.SERVO_POS.code, positionHalfDeg);
        return (int) readShort();
    }
    
    /**
     * Returns the "parameters" for servo. The "parameters" provide 
     * the information needed to estimate the exact 
     * servo angle at which a reading is taken.
     * @return Array containing the values (in microseconds) used to position 
     * the servo to 0º, 90º, and 180º 
     * @throws RuntimeIOException
     */
    public short[] getServoParms() throws RuntimeIOException {
        writeCmdType(CommandTypes.SERVO_PARMS.code);            
        return readNShort(3);
    }

    /**
     * Produces the requested number of ranges readings from the Lidar sensor.
     * The servo is not moved during this command.
     * @param number the number of range readings requested 
     * <p>
     * This method is primarily intended for testing and calibration.
     * </p>
     * @return Array of range readings
     * @throws RuntimeIOException
     */
    protected short[] getRanges(int number) throws RuntimeIOException {
        writeCmdTypeParm(CommandTypes.MULTIPLE.code, number);
        return readNShort(number);
    }
    
    /**
     * Start a Lidar scan with range readings from 0 to 180 degree, with
     * readings taken at 0.5 degree intervals. <b>Does not</b> wait for
     * scan completion.
     * @param delay the delay (in milliseconds) between a servo movement 
     * and a Lidar range reading; 0 produces the default of 80 ms
     * @throws RuntimeIOException
     */
    public void scanStart(int delay) throws RuntimeIOException {
        writeCmdTypeParm(CommandTypes.SCAN.code, delay);
    }

    /**
     * Does a Lidar scan with range readings from 0 to 180 degree, with
     * readings taken at 0.5 degree intervals.<b>Waits</b> 
     * for scan completion.
     * @param delay the delay (in milliseconds) between a servo movement 
     * and a Lidar range reading; 0 produces the default of 80 ms
     * @throws RuntimeIOException
     * @throws java.lang.InterruptedException
     */
    public void scan(int delay) throws RuntimeIOException, InterruptedException {
        writeCmdTypeParm(CommandTypes.SCAN.code, delay);
        isTaskDone(true);
    }

    /**
     * Determines if a long running task (a scan or a warmup) had completed.Can simply check or wait for completion.
     * @param wait indicates if should wait for task completion
     * @return an indication if task has completed
     * @throws RuntimeIOException
     */
    public boolean isTaskDone(boolean wait) throws RuntimeIOException {
        if (device.bytesAvailable() > 1) {
            readShort(); // to keep sync
            return true;
        } else {
            if (!wait) {
                return false;
            } else { // wait
                while (device.bytesAvailable() < 2) {
                    SleepUtil.sleepMillis(1000);
                }
                readShort(); // to keep sync
                return true;
            }
        }       
    }

    /**
     * Provides the "raw" ranges from a scan of 180 degrees. 
     * @return the range readings for a 180 degree scan (361 ranges)
     * @throws RuntimeIOException
     * @throws java.io.IOException if scan cannot be retrieved
     */
    public short[] scanRetrieve() throws RuntimeIOException, IOException {
        writeCmdType(CommandTypes.SCAN_RETRIEVE.code);
        // check the status code
        if (readShort() == -1 ) throw new IOException("No scan to retrieve");
        // get the ranges
        short[] ranges = readNShort(361);
        return ranges;
    }

    /**
     * Starts a warmup task. <b>Does not</b> wait for
     * warmup completion.
     * @param period code for the warmup period; valid values 0-5; 
     * 0 = a few seconds; 5 = a few minutes
     * @throws RuntimeIOException
     */
    public void warmupStart(int period) throws RuntimeIOException {
        writeCmdTypeParm(CommandTypes.WARMUP.code, period);
    }

    /**
     * Does a Lidar Unit warmup.<b>Waits</b> for scan completion.
     * @param period code for the warmup period; valid values 0-5; 
     * 0 = a few seconds; 5 = a few minutes
     * @throws RuntimeIOException
     * @throws java.lang.InterruptedException
     */
    public void warmup(int period) throws RuntimeIOException, InterruptedException {
        writeCmdTypeParm(CommandTypes.WARMUP.code, period);
        isTaskDone(true);
    }
    
    /**
     * Writes a single byte that is the command type.
     * @param type command code
     * @throws RuntimeIOException
     */
    private void writeCmdType(int type) throws RuntimeIOException {
        device.writeByte((byte) type);
    }
    
    /**
     * Writes three bytes, the single byte command type and a 2-byte 
     * command parameter.
     * @param type command code
     * @param parm command parameter
     * @throws RuntimeIOException
     */
    private void writeCmdTypeParm(int type, int parm) throws RuntimeIOException {
        device.write((byte) type, (byte) (parm >> 8), (byte) parm);        
    }
    
    /**
     * Reads a 2-byte integer or Java short.
     * @return a 2-byte integer
     * @throws RuntimeIOException
     */
    private short readShort() throws RuntimeIOException {
        byte[] res = new byte[2];
        // read the response byte array
        device.read(res);           
        // construct response as short
        short value = (short)(res[0] << 8);
        value = (short) (value | (short) Byte.toUnsignedInt(res[1]));
        return value;
    }
    
    /** 
     * Reads a number of 2-byte integers from the device.
     * @param number of 2-byte integers to read.
     * @return an array of 2-byte integers
     * @throws RuntimeIOException
     */
    private short[] readNShort(int number) throws RuntimeIOException {
        short[] values = new short[number];
        for (int i = 0; i < number; i++) {
            values[i] = readShort();
        }
        return values;
    }
        
    private enum CommandTypes {
        ID(10),
        ECHO(11),
        SERVO_POS(30),
        SERVO_PARMS(32),
        MULTIPLE(50),
        SCAN(52),
        SCAN_RETRIEVE(54),
        WARMUP(60);

        public final int code;

        CommandTypes(int code) {
            this.code = code;
        }
    }        
}
