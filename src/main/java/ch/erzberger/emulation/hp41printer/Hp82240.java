package ch.erzberger.emulation.hp41printer;

import ch.erzberger.emulation.common.*;
import ch.erzberger.emulation.communication.FileReaderImpl;
import ch.erzberger.emulation.communication.PrintDataReader;
import ch.erzberger.emulation.communication.SerialHpIrReaderImpl;
import ch.erzberger.emulation.communication.StdInReaderImpl;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static ch.erzberger.emulation.common.CmdLineArgsChecker.*;

@Log
public class Hp82240 {
    static {
        // Load the logging properties from the jar file
        try (InputStream is = Hp82240.class.getClassLoader().
                getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot load log properties from jar file");
        }
    }

    public static void main(String[] args) {
        Map<String, String> arguments = new CmdLineArgsChecker().checkReceiverArgs(args);
        boolean useModelA = arguments.get(MODELA) != null;
        String file = arguments.get(FILEARG);
        if (file != null) {
            // Do not open a port, instead read from the file and then end the application
            PrintDataReader handler = new FileReaderImpl(Paths.get(file));
            handler.registerCallback(new PrinterByteProcessorImpl(new PaperImpl(), useModelA));
            System.exit(0);
        }
        String port = arguments.get(PORTARG);
        if (port == null) {
            System.exit(-1); // Error message will be written by the Cmd Line checker
        }
        PrintDataReader handler;
        if (STDIN.equals(port)) {
            handler = new StdInReaderImpl();
        } else {
            handler = new SerialHpIrReaderImpl(port);
        }
        handler.registerCallback(new PrinterByteProcessorImpl(new PaperImpl(), useModelA));
        log.log(Level.INFO, "Using reader on port: {0}", handler.getDeviceName());
    }
}
