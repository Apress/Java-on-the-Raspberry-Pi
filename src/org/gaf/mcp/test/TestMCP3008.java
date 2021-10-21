package org.gaf.mcp.test;

import static com.diozero.api.SpiConstants.CE1;
import com.diozero.util.Diozero;
import java.io.IOException;
import org.gaf.mcp3008.MCP3008;

/**
 * Tests MCP3008.
 */
public class TestMCP3008 {

    public static void main(String[] args) throws IOException {
        try (MCP3008 adc = new MCP3008(CE1, 3.3f)) {            
            for (int i = 0; i < 5; i++) {
            System.out.format("C%1d = %4d, %.2f FS, %.2fV %n", i, adc.getRaw(i), 
                    adc.getFSFraction(i), adc.getVoltage(i));
            }
        } finally {
            Diozero.shutdown();
        }
    }    
}
