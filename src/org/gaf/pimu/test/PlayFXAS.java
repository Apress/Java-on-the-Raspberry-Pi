package org.gaf.pimu.test;

import com.diozero.api.I2CDevice;

/**
 * Play with the FXAS21002C.
 */
public class PlayFXAS {

    public static void main(String[] args) {
        I2CDevice device = I2CDevice.builder(0x21).build();
        byte whoID = device.readByteData(0x0C);
        System.out.format("who am I: 0x%2x%n", whoID);
        device.close();
    }   
}
