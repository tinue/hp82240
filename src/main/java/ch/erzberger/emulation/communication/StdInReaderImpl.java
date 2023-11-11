package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * The StdInReaderImpl reads from stdin. Write requests are ignored.
 */
@Log
public class StdInReaderImpl implements PrintDataReader {
    ByteProcessor byteProcessor; // Callback that gets the received bytes sent to one by one

    @Override
    public void registerCallback(ByteProcessor byteProcessor) {
        this.byteProcessor = byteProcessor;
        // Start the stdin reader loop
        AtomicInteger currentByte= new AtomicInteger();
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    currentByte.set(System.in.read());
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Error while reading from StdIn", e);
                }
                if (currentByte.get() == -1) {
                    // End of file received, exit
                    break;
                }
                log.log(Level.FINEST, "Read byte from StdIn: {0}", currentByte);
                byteProcessor.processByte((byte)currentByte.get());
            }
        });
        t.start();
    }

    @Override
    public String getDeviceName() {
        return "StdIn";
    }

    @Override
    public boolean waitUntilReady(long timeout) {
        return true; // StdIn is always ready
    }
}
