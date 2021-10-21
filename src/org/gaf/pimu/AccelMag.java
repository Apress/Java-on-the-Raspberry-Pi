    package org.gaf.pimu;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * A accelerometer/magnetometer that is "data ready interrupt" driven. 
 * It delivers results via
 * a FIFO queue. The results are raw readings.
 */
public class AccelMag implements AutoCloseable {
    
    private final BlockingQueue queue;
    private FXOS8700CQ fxos = null;
    private DigitalInputDevice catcher = null;
    
    private long tsLast;
    private boolean active = false;
    
    
    /**
     * Constructs a new accelerometer/magnetometer.
     * 
     * @param interruptPin The GPIO pin for interrupts from the device.
     * @param queue The queue used to provide data
     * @throws IOException 
     */
    public AccelMag(int interruptPin, BlockingQueue queue) throws IOException {
        this.queue = queue;
        // create a FXOS8700CQ
        this.fxos = new FXOS8700CQ();
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
        System.out.println("AccelMag close");
        if (fxos != null) {
            fxos.close();
            fxos = null; 
        }
        if (catcher != null) {
            catcher.close();
            catcher = null;
        }
    }
    
    /**
     * Configures the FXOS8700CQ and activates the device.
     * @throws RuntimeIOException 
     */
    public void begin() throws RuntimeIOException {
        fxos.begin();
    }

    /**
     * Activates an interrupt handler. The interrupt handler is assumed
     * to be configured prior to activation.
     * @throws RuntimeIOException
     */
    public void activateIH() throws RuntimeIOException {               
        // read to clear interrupt status
        fxos.readRaw();

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
     * Interrupt handler: Reads raw data. Calculates time delta between this
     * and last interrupt. Queues the accelerometer X axis value.
     * @param timestamp timestamp for the interrupt in nanoseconds
     * @throws RuntimeIOException
     */
    private void queueRaw(long timestamp) throws RuntimeIOException {
        if (active) {
            int[] xyzxyz = fxos.readRaw();
            
            long tsDelta = timestamp - tsLast;
            tsLast = timestamp;
            long[] sample = {xyzxyz[3], tsDelta};

            // queue it if queue not full
            if (!queue.offer(sample))
                System.err.println("Queue Full!");
        }       
    }           
}
