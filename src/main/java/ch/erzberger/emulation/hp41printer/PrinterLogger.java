package ch.erzberger.emulation.hp41printer;

import lombok.extern.java.Log;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Log public class PrinterLogger {
    // Prevent instantiation
    private PrinterLogger() {
        super();
    }
    public static void logPrintData(byte[] data) {
        String asAscii = new String(data, StandardCharsets.UTF_8);
        StringBuilder asHex = new StringBuilder();
        for (byte currentByte : data) {
            asHex.append(String.format("%02X ", currentByte));
        }
        log.log(Level.FINE, "Parsed data in ASCII format: {0}", asAscii);
        log.log(Level.FINE, "Parsed data in Hex: {0}", asHex);
    }
}
