package ch.erzberger.emulation.sender;

import ch.erzberger.emulation.common.CmdLineArgsChecker;
import ch.erzberger.emulation.common.FileParserHelper;
import ch.erzberger.emulation.communication.PrintDataWriter;
import ch.erzberger.emulation.communication.SerialArduinoWriterImpl;
import ch.erzberger.emulation.communication.StdOutWriterImpl;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static ch.erzberger.emulation.common.CmdLineArgsChecker.*;

@Log
public class RedEyeSender {
    static {
        // Load the logging properties from the jar file
        try (InputStream is = RedEyeSender.class.getClassLoader().
                getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot load log properties from jar file");
        }
    }

    public static void main(String[] args) {
        Map<String, String> arguments = new CmdLineArgsChecker().checkSenderArgs(args);
        String port = arguments.get(PORTARG);
        if (port == null) {
            System.exit(-1); // Error message will be written by the Cmd Line checker
        }
        PrintDataWriter handler;
        if (STDOUT.equals(port)) {
            handler = new StdOutWriterImpl();
        } else {
            handler = new SerialArduinoWriterImpl(port);
        }
        log.log(Level.INFO, "Using port: {0}", handler.getDeviceName());
        // Wait for the Sending device to get ready
        if (!handler.waitUntilReady(5000L)) {
            log.log(Level.SEVERE, "Device {0} did not become ready", handler.getDeviceName());
        }
        log.log(Level.INFO, "Sender is ready");
        // Start reading the file byte by byte, and sending each line to the Arduino
        FileParserHelper helper = new FileParserHelper(Paths.get(arguments.get(FILEARG)));
        byte[] buffer;
        while (helper.hasNextLine()) {
            buffer = helper.readNextLine();
            handler.sendBytes(buffer);
        }
        handler.flush();
        System.exit(0);
    }
}
