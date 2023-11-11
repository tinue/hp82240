package ch.erzberger.emulation.communication;

import ch.erzberger.emulation.common.ByteProcessor;
import ch.erzberger.emulation.hp41printer.PrinterLogger;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Helper class for serial ports. Will (auto) detect ports, depending on operating system.
 */
@Log
public class SerialPortWrapper {
    private final SerialPort port;
    private ByteProcessor byteProcessor;

    public SerialPortWrapper(String portName) {
        if (portName == null || portName.isEmpty()) {
            log.log(Level.FINE, "Autodetect serial port");
            this.port = autoDetectPort();
        } else {
            log.log(Level.FINE, "Detect serial port {0}", portName);
            this.port = detectPort(portName);
        }
        if (port == null) {
            throw new NoClassDefFoundError("Could not open serial port");
        }
    }

    private SerialPort detectPort(String portName) {
        SerialPort[] ports = SerialPort.getCommPorts();
        int numPorts = 0; // Ports found with the specified name. More than one is not good.
        SerialPort lastDetected = null; // If only one is found, it will be in this variable
        for (SerialPort currentPort : ports) {
            if (currentPort.getSystemPortName().contains(portName)) {
                log.log(Level.FINE, "Match found: {0}", currentPort.getSystemPortName());
                // There is a match, remember it and add one to the number of found ports
                // But specific on the Mac: Filter out the 'tty.usbmodem' part, it does not count
                if (currentPort.getSystemPortName().startsWith("tty.usbmodem")) {
                    log.log(Level.FINE, "Filtering match on the Mac: {0}", currentPort.getSystemPortName());
                    break;
                } else {
                    lastDetected = currentPort;
                    numPorts++;
                }
            } else {
                log.log(Level.FINE, "Ignoring not matching serial port {0}", currentPort.getSystemPortName());
            }
        }
        // Check if a port has been found
        if (numPorts == 1) {
            log.log(Level.FINE, "Returning the only matching port: {0}", lastDetected.getSystemPortName());
            return lastDetected;
        } else {
            log.log(Level.FINE, "No port did match, returning null");
            return null;
        }
    }

    private SerialPort autoDetectPort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        // If there is only one port, then we have a match. This never happens on a Mac, see below.
        if (ports.length == 1) {
            log.log(Level.FINE, "One port found: {0}", ports[0].getSystemPortName());
            return ports[0];
        }
        // Search for some well known port names. Count them up, and if only one is found we are good
        int numFound = 0;
        SerialPort lastFound = null;
        for (SerialPort currentPort : ports) {
            if (currentPort.getSystemPortName().startsWith("cu.usb")) {
                log.log(Level.FINE, "Mac port found: {0}", currentPort.getSystemPortName());
                lastFound = currentPort;
                numFound++;
            }
            if (currentPort.getSystemPortName().startsWith("ttyACM")) {
                log.log(Level.FINE, "Debian port found: {0}", currentPort.getSystemPortName());
                lastFound = currentPort;
                numFound++;
            }
        }
        if (numFound == 1) {
            log.log(Level.FINE, "Exactly one port found: {0}", lastFound.getSystemPortName());
            return lastFound;
        } else {
            // Too many ports found
            log.log(Level.FINE, "Too many ports found: {0}", ports.length);
            return null;
        }
    }

    public void openPort(ByteProcessor byteProcessor) {
        this.byteProcessor = byteProcessor;
        port.addDataListener(new Listener());
        boolean success = openPort(port);
        if (success) {
            log.log(Level.FINEST, "Port {0} opened successfully for reading", port.getSystemPortName());
        } else {
            log.log(Level.SEVERE, "Port open for reading failed on port {0}", port.getSystemPortName());
        }
    }

    public int writeBytes(byte[] bytesToWrite) {
        return port.writeBytes(bytesToWrite, bytesToWrite.length);
    }

    public void flush() {
        boolean success = port.flushIOBuffers();
        if (success) {
            log.log(Level.FINEST, "Flush successful on port {0}", port.getSystemPortName());
        } else {
            log.log(Level.WARNING, "Flush not successful on port {0}", port.getSystemPortName());
        }
    }

    public String getSystemPortName() {
        return port.getSystemPortName();
    }

    private boolean openPort(SerialPort port) {
        log.log(Level.FINE, "Found port: {0}", port.getSystemPortName());
        // Setup comm parameters (115200, 8N1)
        port.setParity(SerialPort.NO_PARITY);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setNumDataBits(8);
        port.setBaudRate(115200);
        log.log(Level.FINEST, "Opening port");
        return port.openPort();
    }

    class Listener implements SerialPortDataListener {
        @Override
        public int getListeningEvents() {
            log.log(Level.FINEST, "GetListeningEvents called");
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; // Currently only check for data
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (port == null) {
                // This should not happen, but the compiler insists that it can happen.
                log.log(Level.SEVERE, "Unexpected error, port is null when it should not be");
                return;
            }
            byte[] buffer = new byte[2000];  // The max number of bytes that can be received in one go
            log.log(Level.FINEST, "serialEvent called");
            // Check for library malfunction
            if (serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                log.log(Level.SEVERE, "Unexpected SerialPortEvent received: {0}", serialPortEvent);
                return;
            }
            int bytesAvailable = port.bytesAvailable();
            if (bytesAvailable <= 0) {
                log.log(Level.SEVERE, "Handler was called, but no data is available. Number of bytes available: {0}", bytesAvailable);
                return;
            }
            // Process the data
            int bytesRead = port.readBytes(buffer, Math.min(buffer.length, bytesAvailable));
            log.log(Level.FINEST, "Bytes read: {0}", bytesRead);
            // Copy the bytes into an appropriately sized array.
            byte[] realBytes = new byte[bytesRead];
            System.arraycopy(buffer, 0, realBytes, 0, bytesRead);
            // Log the buffer that was read
            PrinterLogger.logPrintData(realBytes);
            // Pass the bytes to the parser
            byteProcessor.processBytes(realBytes);
        }
    }
}
