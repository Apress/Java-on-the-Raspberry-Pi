package org.gaf.sss;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.Action;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * Represents the Watterott SilentStepStick stepper motor driver.
 */
public class SilentStepStick implements AutoCloseable {
    
    private DigitalOutputDevice dir;
    private DigitalOutputDevice enable;
    private DigitalOutputDevice step;

    private final float microstepsPerRev;            
    private boolean running = false;

    /**
     * Constructor for Watterott SilentStepStick.
     * @param enablePin GPIO pin for the enable signal
     * @param directionPin GPIO pin for the direction signal
     * @param stepPin GPIO pin for the step signal
     * @param stepsPerRev full steps per revolution for the motor
     * @param resolution micro-steps per step set by the configuration of 
     * SilentStepStick signals CFG1 and CFG2
     * @throws IOException 
     */
    public SilentStepStick(int enablePin, int directionPin, int stepPin,
            int stepsPerRev, Resolution resolution) throws IOException {
        try {
            // set up GPIO
            enable = new DigitalOutputDevice(enablePin, false, false);
            dir = new DigitalOutputDevice(directionPin, true, false);
            step = new DigitalOutputDevice(stepPin, true, false);        

            // set configuration
            microstepsPerRev = 
                    (float) (stepsPerRev * resolution.resolution);        
        } catch (RuntimeIOException ex) {
            throw new IOException(ex.getMessage());
        }
    }    
    
    /**
     * Closes device by stopping any movement and closing all internal
     * devices.
     * @throws IOException 
     */
    @Override
    public void close() throws IOException {
        // disable
        if (enable != null) {
            enable.off();
            enable.close();
            enable = null;
        }
        // stop
        if (step != null) {
            // turn it off
            step.stopOnOffLoop();
            step.close();
            step = null;
        }        
        if (dir != null) {
            dir.close(); 
            dir = null;
        }
    }
    
    /**
     * Enables or disables the stepper driver.
     * <p>
     * When disabled, the driver does not power the motor, and thus there is
     * no torque applied. It can be turned manually. When enabled, the driver
     * powers the motor, and thus there is torque. It cannot be turned 
     * manually.
     * </p>
     * @param enableIt true to enable, false to disable
     * @throws RuntimeIOException for IO errors
     */
    public void enable(boolean enableIt) throws RuntimeIOException {
        if (enableIt) {
            enable.on();
        }
        else {
            enable.off();
        }
    }
    
    /**
     * Sets the direction of rotation, either clockwise or counterclockwise.
     * @param direction 
     * @throws RuntimeIOException for IO errors
     */
    private void setDirection(Direction direction) throws RuntimeIOException {
        if (direction == Direction.CW) dir.off();
        else dir.on();
    } 
    
    /**
     * Causes the driver to run the stepper motor in the desired
     * direction at the desired speed forever, in a background thread. 
     * Once turned on, the driver runs until stopped. 
     * <p>
     * The motor runs only if the driver is enabled.
     * <p>
     * If the driver is already running, it is stopped and then restarted.
     *
     * @param direction direction to rotate
     * @param speedRPM rotation speed in RPM
     * @throws RuntimeIOException for IO errors
     */
    public void run(Direction direction, float speedRPM) 
        throws RuntimeIOException {
        if (running) step.stopOnOffLoop();
        // let motor rest (see p.9 of datasheet)
        SleepUtil.sleepMillis(100);
        // set direction
        setDirection(direction);
        // start the step signal in background
        float halfPeriod = getHalfPeriod(speedRPM);
        step.onOffLoop(halfPeriod, halfPeriod, 
                DigitalOutputDevice.INFINITE_ITERATIONS, 
                true, null);
        running = true;
    }  
    
    /**
     * Stops the step signal to the driver.
     * @throws RuntimeIOException for IO errors
     */
    public void stop() throws RuntimeIOException {       
        step.stopOnOffLoop();
        running = false;
    }
        
    /**
     * Calculates the half period of step signal from the desired 
     * rotational speed of the motor.
     * @param speedRPM in revolutions per minute
     * @return half period
     */
    private float getHalfPeriod(float speedRPM) {
        float speedRPS = speedRPM/60f; // speed in revolutions per second
        float frequency = speedRPS * microstepsPerRev; // microsteps per second
        float halfPeriod = 0.5f / frequency;
        return halfPeriod;
    }
    
    /**
     * Causes the driver to run the requested number of steps in the requested
     * direction at the requested speed.
     * <p>
     * The motor runs only if the driver is enabled.
     * </p>
     * @param count number of steps to take
     * @param direction desired direction of travel
     * @param speedRPM desired speed in RPM
     * @param background run in background
     * @param stopAction Action to take at completion
     * @return true if action started in background or completed in foreground;
     * false if already running
     * @throws RuntimeIOException for IO errors
     */
    public boolean stepCount(int count, Direction direction, float speedRPM, 
            boolean background, Action stopAction) 
            throws RuntimeIOException {
        
        if (running) {
            return false;
        } else {
            // let motor rest (see p.9 of datasheet)
            SleepUtil.sleepMillis(100);
            
            // set up an intercept so will know when stepping finished
            Action intercept = () -> {
//                System.out.println("intercept");
                running = false;
            };
            
            // set direction
            setDirection(direction);
            
            // start stepping
            running = true;
            float halfPeriod = getHalfPeriod(speedRPM);
            if (stopAction != null) {
                step.onOffLoop(halfPeriod, halfPeriod,
                        count, background,
                        intercept.andThen(stopAction));                
            } else {
                step.onOffLoop(halfPeriod, halfPeriod,
                        count, background, intercept);
            }
             
            return true;
        }
    }
    
    /**
     * Get the count of steps taken.
     * @return number of steps taken
     */
    public int getStepCount() {
        return step.getCycleCount();
    }
        
    public enum Direction {
        CW,
        CCW;
    }
    
    public enum Resolution {
        Full(1),
        Half(2),
        Quarter(4),
        Eighth(8),
        Sixteenth(16);  
        
        public  final int resolution;

        Resolution(int resolution) {
            this.resolution = resolution;
        }
    }       
}
