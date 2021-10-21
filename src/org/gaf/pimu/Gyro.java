package org.gaf.pimu;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * A gyroscope that is "data ready interrupt" driven. It delivers results via
 * a FIFO queue. The results range from raw readings to an absolute heading
 * calculated from data that has a zero offset and a dead zone applied.
 */
public class Gyro implements AutoCloseable {
    
    private final BlockingQueue queue;
    private FXAS21002C fxas = null;
    private DigitalInputDevice catcher = null;
    private FXAS21002C.ODR odr;
    
    private long tsLast;
    private boolean active = false;
    
    private static final int BAD_DATA = 5;
    private final long[] acc = new long[3];
    private int total;
    private final float[] zeroOffset = new float[3];
    
    private static final long DEAD_ZONE = 20;

    private float angle;    
    private float sensitivity;
    private float period;    
    
    /**
     * Constructs a new gyroscope.
     * 
     * @param interruptPin The GPIO pin for interrupts from the device.
     * @param queue The queue used to provide data
     * @throws IOException 
     */
    public Gyro(int interruptPin, BlockingQueue queue) throws IOException {
        this.queue = queue;
        // create a FXAS21002C
        this.fxas = new FXAS21002C();
        // create a interrupt catcher
        try {
            catcher = new DigitalInputDevice(
                    interruptPin, 
                    GpioPullUpDown.NONE,
                    GpioEventTrigger.RISING);
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());            
        }        
    }

    /**
     * Closes the devices used in the gyroscope.
     */
    @Override
    public void close() {
        System.out.println("Gyro close");
        if (fxas != null) {
            fxas.close();
            fxas = null; 
        }
        if (catcher != null) {
            catcher.close();
            catcher = null;
        }
    }
    
    /**
     * Set the configuration for the underlying FXAS21002C.
     * @param lpfCutoff The low pass filter cutoff option.
     * @param odr The desired output data rate.
     * @throws RuntimeIOException 
     */
    public void begin(FXAS21002C.LpfCutoff lpfCutoff, FXAS21002C.ODR odr) 
            throws RuntimeIOException {
        this.odr = odr;
        // start the FXAS21002C
        fxas.begin(lpfCutoff, odr);       
    }

    /**
     * Activates an interrupt handler. The desired interrupt handler is assumed
     * to be configured prior to activation.
     * @throws RuntimeIOException
     */
    public void activateIH() throws RuntimeIOException {               
        // read to clear interrupt status
        fxas.readRaw();

        // empty the queue
        queue.clear();

        // set active
        tsLast = 0;
        this.active = true;
    }

     /**
     * Activates the interrupt handler to deliver raw results.
     * @throws RuntimeIOException
     */
    public void activateRaw() throws RuntimeIOException {       
        // identify interrupt handler
        catcher.whenActivated(this::queueRaw);
        
        activateIH();
    }
   
    /**
     * Deactivates the interrupt handler.
     */
    public void deactivate() {
        this.active = false;
    }

     /**
     * Activates the interrupt handler to deliver results adjusted 
     * by zero offset.
     * @throws RuntimeIOException
     */
    public void activateZO() throws RuntimeIOException {       
        // identify interrupt handler
        catcher.whenActivated(this::queueO);
        
        activateIH();
    }
   
     /**
     * Activates the interrupt handler to deliver results adjusted
     * by zero offset and restricted by dead zone.
     * @throws RuntimeIOException
     */
    public void activateZODZ() throws RuntimeIOException {       
        // identify interrupt handler
        catcher.whenActivated(this::queueOD);

        activateIH();
    }
   
    /**
     * Calculates the zero offset for all three axes.
     * <p>
     * It activates an interrupt handler to accumulate the raw data 
     * necessary to calculate the zero offset.
     * </p>
     * @param period Time in milliseconds to gather data
     * @throws RuntimeIOException
     */
    public void calcZeroOffset(int period) throws RuntimeIOException {
        
        // clear any accumulation
        acc[0] = 0;
        acc[1] = 0;
        acc[2] = 0;
        total = 0;
        
        // identify interrupt handler that accumulates raw data and activate
        catcher.whenActivated(this::accumulateRaw);
        activateIH();
            
        // sleept to gather raw data
        SleepUtil.sleepMillis(period);
        deactivate();
        
        // calculate the zero offsets
        float denom = (float) (total - BAD_DATA);
        zeroOffset[0] = (float) acc[0] / denom;
        zeroOffset[1] = (float) acc[1] / denom;
        zeroOffset[2] = (float) acc[2] / denom;
        System.out.println("Total = " + denom);
        System.out.format("Zero offsets: z=%f ", zeroOffset[2]);
    }
    
    /**
     * Activates the interrupt handler to deliver a Z axis heading. 
     * @param range The range used by the FXAS21002C instance
     * @throws RuntimeIOException
     */
    public void activateHeading(FXAS21002C.Range range) 
            throws RuntimeIOException {
        // initialize
        angle = 0;
        sensitivity = range.sensitivity;
        period = 1 / odr.odr;
//        System.out.println("sensitivity = " + sensitivity + " period = " + period);

        // identify interrupt handler
        catcher.whenActivated(this::queueHeading);

        activateIH();        
    }
        
    
    /**
     * Interrupt handler: Reads raw data. Calculates time delta between this
     * and last interrupt. Queues the Z axis value.
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    private void queueRaw(long timestamp) throws RuntimeIOException {
        if (active) {
            int[] xyz = fxas.readRaw();
            
            long tsDelta = timestamp - tsLast;
            tsLast = timestamp;
            long[] sample = {xyz[2], tsDelta};

            // queue it if queue not full
            if (!queue.offer(sample))
                System.err.println("Queue Full!");
        }       
    }    
        
    /**
     * Interrupt handler: Reads raw data; adds to the accumulation
     * to used to calculate the zero offsets. It skips the first few 
     * readings to account for anomalies in initial readings.
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    private void accumulateRaw(long timestamp) throws RuntimeIOException {
        if (active) { 
            int[] xyz = fxas.readRaw();

            if (total >= BAD_DATA) {
                acc[0] += xyz[0];
                acc[1] += xyz[1];
                acc[2] += xyz[2];
            }
            total++;
        }        
    } 
    
    /**
     * Interrupt handler: Reads the raw data, subtracts the zero offset. 
     * Calculates time delta between this
     * and last interrupt. Queues the results for the Z axis. 
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    public void queueO(long timestamp) throws RuntimeIOException {        
        if (active) {
            int[] xyz = fxas.readRaw();

            long tsDelta = timestamp - tsLast;
            tsLast = timestamp;

            long z =  xyz[2] - (long) zeroOffset[2];
            long[] sample = {z, tsDelta};

            // queue it if queue not full
            if (!queue.offer(sample))
                System.err.println("Queue Full!");
        }              
    }

    /**
     * Interrupt handler: Reads the raw data, subtracts the zero offset, 
     * restricts result to outside the dead zone. 
     * Calculates time delta between this
     * and last interrupt. Queues the results for the Z axis. 
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    public void queueOD(long timestamp) throws RuntimeIOException {
        if (active) {
            int[] xyz = fxas.readRaw();

            long tsDelta = timestamp - tsLast;
            tsLast = timestamp;
            long z =  xyz[2] - (long) zeroOffset[2];

            if ((-DEAD_ZONE <= z) && (z <= DEAD_ZONE)) {
                 z = 0;
            }

            long[] sample = {z, tsDelta};

            // queue it if queue not full
            if (!queue.offer(sample))
                System.err.println("Queue Full!");
        }              
    }
    
    /**
     * Interrupt handler: Reads the raw data, calculates
     * the Z axis minus the zero offset, determines if the result is outside
     * the "dead zone", integrates the result or 0 (as appropriate) to produce
     * an updated heading.
     * 
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    public void queueHeading(long timestamp) throws RuntimeIOException {
        if (active) {
            int[] xyz = fxas.readRaw();
            float z =  (float) xyz[2] - zeroOffset[2];
            if ((-DEAD_ZONE <= z) && (z <= DEAD_ZONE)) {
                 z = 0;
            }
            // integrate
            angle += (z * sensitivity) * period;

            // put the angle in queue if queue not full
            if (!queue.offer(angle))
                System.err.println("Queue Full!");
        }       
    }    
}
