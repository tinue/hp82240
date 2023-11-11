package ch.erzberger.emulation.communication;

import lombok.extern.java.Log;

import java.io.IOException;
import java.util.logging.Level;

/**
 * The StdOutWriterImpl writes to stdout. The reading part is only simulated to emulate the Arduino getting ready.
 */
@Log
public class StdOutWriterImpl implements PrintDataWriter {
    @Override
    public String getDeviceName() {
        return "StdOut";
    }

    @Override
    public boolean waitUntilReady(long timeout) {
        return true;  // Stdout is always ready
    }

    @Override
    public void sendBytes(byte[] sendBuffer) {
        try {
            System.out.write(sendBuffer); // NOSONAR Wanted
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot write to StdOut", ex);
        }
    }

    @Override
    public void flush() {
        System.out.flush(); // NOSONAR Wanted
    }
}
