package org.gaf.mcp.test;

import static com.diozero.api.SpiConstants.CE1;
import com.diozero.devices.McpAdc;
import com.diozero.devices.McpAdc.Type;
import com.diozero.util.Diozero;

/**
 * Test diozero McpAdc using MCP3008
 */
public class TestMcpAdc {

    public static void main(String[] args) {
        try (McpAdc adc = new McpAdc(Type.MCP3008, CE1, 3.3f)) {            
            for (int i = 0; i < 5; i++) {
                System.out.format("V%1d = %.2f FS%n", i , adc.getValue(i));
            }
        } finally {
            Diozero.shutdown();
        }
    }
}