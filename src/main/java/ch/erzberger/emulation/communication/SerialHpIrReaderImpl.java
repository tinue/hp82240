package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;
import lombok.extern.java.Log;

/**
 * The SerialHpIrReaderImpl works with the IR reader from HP, attached via USB/Serial
 * The reader works with this device: <a href="https://github.com/mjakuipers/HP-RedEye-Receiver">HP-RedEye-Receiver</a>
 */
@Log
public class SerialHpIrReaderImpl implements PrintDataReader {
    final SerialPortWrapper port;
    ByteProcessor byteProcessor; // Callback that gets the received bytes sent to one by one

    public SerialHpIrReaderImpl(String serialPortName) {
        this.port = new SerialPortWrapper(serialPortName);
    }

    @Override
    public void registerCallback(ByteProcessor byteProcessor) {
        this.byteProcessor = byteProcessor;
        port.openPort(byteProcessor);
    }

    @Override
    public String getDeviceName() {
        return port.getSystemPortName();
    }

    @Override
    public boolean waitUntilReady(long timeout) {
        return true; // The IR reader is always ready
    }
}

