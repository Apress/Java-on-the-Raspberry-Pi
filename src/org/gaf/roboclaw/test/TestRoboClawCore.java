package org.gaf.roboclaw.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import org.gaf.roboclaw.RoboClaw;
import org.gaf.roboclaw.RoboClawUtil;

/**
 * Tests RoboClaw non-motor commands
 */
public class TestRoboClawCore {

    private final static int ADDRESS = 0x80;
    
    public static void main(String[] args) throws IOException, InterruptedException {        
        // identity verification 
        String clawFile = RoboClawUtil.findDeviceFile("03eb", "2404", ADDRESS);   
        if (clawFile == null) {
            throw new IOException("No matching device!");
        }  
        
        try (RoboClaw claw = new RoboClaw(clawFile, ADDRESS)) {
            
            Diozero.registerForShutdown(claw);
                        
            long[] encoders = new long[2];
            boolean ok = claw.setEncoderM1(123456l);
            if (!ok) {
                System.out.println("writeN failed!");
            }   ok = claw.getEncoders(encoders);
            if (!ok) {
                System.out.println("readN failed");
            } else {
                System.out.println("Encoder M1:" + encoders[0]);
            }
            int[] voltage = new int[1];
            ok = claw.getMainBatteryVoltage(voltage);
            if (!ok) {
                System.out.println("read2 failed");
            } else {
                System.out.println("Main battery voltage: " + voltage[0]);
            }

            RoboClaw.VelocityPID m1PID = new RoboClaw.VelocityPID();
            ok = claw.getM1VelocityPID(m1PID);
            if (!ok) {
                System.out.println("readN failed");
            } else {
                System.out.println("M1:" + m1PID);
            }

            m1PID = new RoboClaw.VelocityPID(8, 7, 6, 2000);
            ok = claw.setM1VelocityPID(m1PID);
            if (!ok) {
                System.out.println("writeN failed");
            }

            ok = claw.getM1VelocityPID(m1PID);
            if (!ok) {
                System.out.println("readN failed");
            } else {
                System.out.println("M1:" + m1PID);
            }
            
        } finally {
            Diozero.shutdown();
        }
    }
}
