package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * The SerialReaderWriterImpl works with the IR reader / writer that is attached via USB.
 */
@Log
public class SerialArduinoWriterImpl implements PrintDataWriter {
    final SerialPortWrapper serialPort;

    private boolean ready = false;

    public SerialArduinoWriterImpl(String serialPortName) {
        this.serialPort = new SerialPortWrapper(serialPortName);
    }

    @Override
    public void flush() {
        serialPort.flush();
    }

    @Override
    public String getDeviceName() {
        return serialPort.getSystemPortName();
    }

    @SuppressWarnings("BusyWait")
    @Override
    public boolean waitUntilReady(long timeout) {
        // Technically, the SerialArduinoWriterImpl is a pure writer. To check for readiness it needs to read, though.
        // The Arduino sketch sends a dollar sign to signal that it is ready to send data via infrared.
        // This is necessary because some Arduino models (specifically the Uno) perform a reset once
        // right after the serial port is opened. This reset takes time.
        serialPort.openPort(new ReadinessChecker());
        long totalWaitTime = 0;
        final long waitInterval = 500L;
        while (!ready && totalWaitTime < timeout) {
            totalWaitTime += waitInterval;
            // If not ready yet, wait a bit
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                log.log(Level.FINE, "Thread was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        // Either ready, or the timeout has been reached
        if (totalWaitTime >= timeout) {
            log.log(Level.SEVERE, "Arduino did not become ready, exiting");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void sendBytes(byte[] sendBuffer) {
        int bytesWritten = serialPort.writeBytes(sendBuffer);
        if (bytesWritten != sendBuffer.length) {
            log.log(Level.WARNING, "Only {0} of {1} were sent to the IR device", new Object[]{bytesWritten, sendBuffer.length});
        }
        waitAfterPrint(sendBuffer.length, sendBuffer[sendBuffer.length - 1]);
    }

    private void waitAfterPrint(int bufferSize, byte lastByte) {
        try {
            // The IR protocol is less than 80 bytes per second; Arduino is 115200. Hence: Wait for the serial data to be sent.
            // Specific: One byte needs 12.82 milliseconds to transmit
            long transmissionTime = (long)Math.ceil(12.82 * bufferSize);
            Thread.sleep(transmissionTime);
            if (lastByte == 0x04 || lastByte == 0x0A) {
                // The printer will print on a line feed. Wait 1.8 Seconds as specified in the printer manual
                Thread.sleep(1800);
            }
        } catch (InterruptedException e) {
            log.log(Level.INFO, "Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private class ReadinessChecker implements ByteProcessor {
        @Override
        public void processByte(byte byteReceived) {
            if (byteReceived == '$') {
                log.log(Level.FINE, "Dollar sign received, Arduino is ready");
                ready = true;
            } else {
                log.log(Level.FINEST, "Character received from the Arduino: {0}", new String(new byte[]{byteReceived}));
            }
        }
    }
}

