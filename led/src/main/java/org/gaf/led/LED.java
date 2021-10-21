package org.gaf.led;

import com.diozero.api.DigitalOutputDevice;

public class LED implements AutoCloseable {
    
    private final DigitalOutputDevice led;
    
    public LED(int pin) {
        led = new DigitalOutputDevice(pin);
        System.out.println("Starting LED now.");
    }
    
    @Override
    public void close() {
        led.close();
    }
    
    public void on() {
        led.on();
    }
    
    public void off() {
        led.off();
    }
}
