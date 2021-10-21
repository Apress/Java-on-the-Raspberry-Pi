package org.gaf.lidar;

import com.diozero.api.RuntimeIOException;
import java.text.DecimalFormat;

/**
 * Describes a Lidar range reading in terms of its position in a 180
 * degree (361 ranges) scan, its polar coordinates, and its Cartesian coordinates.
 * @author gregflurry
 */
public class LidarPoint {
    
    /**
     * Index in a 361 member array representing a 180 degree with
     * a range reading every 0.5 degrees.
     */
    public int index;
    /**
     * The radial coordinate for a range in a polar coordinate system.
     */
    public float rho;
    /**
     * The angular coordinate for a range in a polar coordinate system.
     */
    public float theta;
    /**
     * The x coordinate for a range in a polar Cartesian coordinate system.
     */
    public float x; 
    /**
     * The y coordinate for a range in a polar Cartesian coordinate system.
     */
    public float y;
    
    /**
     * Creates an instance with just the index and rho, basically the "raw"
     * information from a Lidar Unit scan.
     * @param index
     * @param rho 
     */
    public LidarPoint(int index, float rho) {
        this.index = index;
        this.rho = rho;
    }
    
    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("###.00");
        String out = String.format("index = %3d : "
                + "(\u03c1,\u03b8)=(%2$6s,%3$6s) : "
                + "(x,y)= (%4$7s,%5$6s)", 
                index, df.format(rho), df.format(theta),
                df.format(x), df.format(y));
        return out;
    }
    
    private static float servoStepsIn1; // the number of 0.25 microsecond "steps" in 1 degrees
    private static boolean configured = false;
    
    /**
     * Allows setting the servo parameters needed to calculate the
     * exact angle at which any range is taken.
     * @param parms the servo parameters (the servo controller value need 
     * to produce 0, 90, and 180 degree rotations 
     */
    public static void setServoParms(short[] parms) {
        configured = true;
    
        float servoStepsIn180 = (parms[2] - parms[0]) * 4; // four steps per microsec
        servoStepsIn1 = servoStepsIn180 / 180;
    }
    
    /**
     * For a set of "raw" range reading, produces an array of 
     * corresponding LidarPoint. 
     * <p>
     * The servo parameters must be set prior to calling this method.
     * </p>
     * @param scan a "raw" scan of 361 ranges
     * @return an array of LidarPoint
     * @throws RuntimeIOException if servo parameters not set
     */
    public static LidarPoint[] processScan(short[] scan) 
            throws RuntimeIOException {
        
        if (!configured) throw new RuntimeIOException("Servo parameters unset.");
        
        LidarPoint[] lp = new LidarPoint[scan.length];
        
        for (int i = 0; i < scan.length; i++) {
            // create the point
            lp[i] = new LidarPoint(i, scan[i]);
            // indicate invalid information
            lp[i].rho = (lp[i].rho <= 5) ? -1 : lp[i].rho;
            // calculate ideal theta (degrees)
            lp[i].theta = (float)i / 2;
            
            // calculate exact angle (degrees)
            lp[i].theta = ((int) (lp[i].theta * servoStepsIn1 + 0.5)) / servoStepsIn1;

            // convert angle to radians 
            lp[i].theta = (float) Math.toRadians((float) lp[i].theta);     
            
            // calculate Cartesian coordinates
            if (lp[i].rho != -1) {
                lp[i].x = (float) Math.cos(lp[i].theta) * lp[i].rho;
                lp[i].y = (float) Math.sin(lp[i].theta) * lp[i].rho;                               
            }
        }
        return lp;
    }
}
