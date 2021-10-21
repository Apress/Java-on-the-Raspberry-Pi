package org.gaf.bme280.test;

import com.diozero.api.SpiConstants;
import com.diozero.devices.BME280; 
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;
import java.io.IOException;

/**
 * Tests BME280 using I2C and SPI
 */
public class TestBME280 {

    /**
     * Reads the BME280 using either I2C or SPI every second 
     * for a given number of seconds.
     * @param args the command line arguments define the interface and
     * the number of iterations; accepts 0, 1, or 2 arguments; 
     * 0 arguments uses I2C and 3 iterations; if first argument = i use I2C,
     * otherwise use SPI; the second argument if present indicates the number
     * if iterations
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        boolean useI2C = true;
        int number = 3;
        switch (args.length) {
            case 2: // set interface type AND iterations
                number = Integer.parseInt(args[1]);
            case 1: // set device type
                if (!args[0].toLowerCase().equals("i")) useI2C = false;
                break;
            default: // use defaults               
        } 
                
        BME280 bme280;
        if (useI2C) 
            bme280 = new BME280();
        else 
            bme280 = new BME280(SpiConstants.CE0);

        try (bme280) {
            for (int i = 0; i < number; i++) {
                bme280.waitDataAvailable(10, 5);
                float[] tph = bme280.getValues();
                float tF = tph[0] * (9f/5f) + 32f;
                float pHg = tph[1] * 0.02953f;

                System.out.format("T=%.1f\u00B0C or %.1f\u00B0F "
                        + " P=%.1f hPa or %.1f inHg "
                        + " RH=%.1f%% %n", tph[0], tF, tph[1], pHg, tph[2]);
                
		SleepUtil.sleepSeconds(1);
            }
        } finally {
            Diozero.shutdown();
        }
    }
}
