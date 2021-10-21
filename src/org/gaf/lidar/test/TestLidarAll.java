package org.gaf.lidar.test;

import com.diozero.util.Diozero;
import java.io.IOException;
import java.util.Scanner;
import org.gaf.lidar.Lidar;
import org.gaf.lidar.LidarPoint;
import org.gaf.lidar.LidarUtil;

/**
 * Test all Lidar commands.
 */
public class TestLidarAll extends Lidar {
    
    public TestLidarAll(String fileName) throws IOException {
        super(fileName);
    }

    public static void main(String arg[]) throws IOException, InterruptedException {
        // identity verification
        String deviceFile = LidarUtil.findDeviceFile("1ffb", "2300");    

        if (deviceFile == null) {
            throw new IOException("No matching device!");
        }    
        
        try (TestLidarAll tester = new TestLidarAll(deviceFile)) {
            // enable keyboard input
            Scanner input = new Scanner(System.in);

            String command = "";
            while (true) {
                System.out.print("Command (type,parm; 'q' is quit): ");
                command = input.next();
                System.out.println();
                // parse
                String delims = "[,]";
                String[] tokens = command.split(delims);
                if (tokens[0].equalsIgnoreCase("q")) {
                    tester.close();
                    System.exit(0);
                }
                int type = Integer.parseInt(tokens[0]);
                int parm = 0;
                if (tokens.length > 1) {
                    parm = Integer.parseInt(tokens[1]);
                }
                System.out.println("type: " + type + " parm: " + parm);

                switch (type) {
                    case 10: 
                        int id = tester.getID();
                        System.out.println("ID=" + id);
                        break;
                    case 11:
                        short echo = tester.echoParameter((short)parm);
                        System.out.println("Echo= " + echo);
                        break;
                    case 30:
                        int rc = tester.setServoPosition(parm);
                        System.out.println("rc= " + rc);
                        break;
                    case 32:
                        short[] p = tester.getServoParms();
                        for (short pv : p) {
                            System.out.println("pv= " + pv);
                        }
                        LidarPoint.setServoParms(p);
                        break;
                    case 50:
                        short[] ranges = tester.getRanges(parm);
                        for (short r : ranges) {
                            System.out.println("r= " + r);
                        }
                        break;
                    case 52:
                        tester.scanStart(parm);
                        break;
                    case 53: // fake code
                        tester.scan(parm);
                        break;
                    case 54:
                        ranges = tester.scanRetrieve();
                        for (short r : ranges) {
                            System.out.println("r= " + r);
                        }
                        break;
                    case 55: // fake code
                        boolean wait;
                        if (parm == 0) wait = false;
                        else wait = true;
                        boolean status = tester.isTaskDone(wait);
                        System.out.println("status= " + status);
                        break;
                    case 57:
                        ranges = tester.scanRetrieve();
                        LidarPoint[] lps = LidarPoint.processScan(ranges);
                        for (LidarPoint pt : lps) {
                            System.out.println(pt);
                        }                  
                        break;
                    case 60:
                        tester.warmupStart(parm);
                        break;
                    case 61: // fake code
                        tester.warmup(parm);
                        break;
                    default:
                        System.out.println("BAD Command!");
                }
            }              
        } finally {
            Diozero.shutdown();
        }       
    }    
}
