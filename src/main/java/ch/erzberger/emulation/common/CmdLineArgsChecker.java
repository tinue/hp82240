package ch.erzberger.emulation.common;

import lombok.extern.java.Log;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class CmdLineArgsChecker {
    public static final String FILEARG = "inputFile";

    public static final String MODELA = "modelA";
    public static final String PORTARG = "port";
    public static final String STDOUT = "stdout";
    public static final String STDIN = "stdin";

    private static final String SENDER = "sender";
    private static final String RECEIVER = "receiver";

    public Map<String, String> checkReceiverArgs(String[] args) {
        return checkArgs(args, RECEIVER);
    }

    public Map<String, String> checkSenderArgs(String[] args) {
        return checkArgs(args, SENDER);
    }

    private Map<String, String> checkArgs(String[] args, String role) {
        boolean isSender = SENDER.equals(role);
        String prefix = isSender ? "RedEyeSender" : "Hp82240";
        HashMap<String, String> result = new HashMap<>();
        // Set up the command line parameters
        Options options = new Options();
        if (isSender) {
            options.addOption(Option.builder("i").longOpt(FILEARG)
                    .desc("File that is sent to the PORT")
                    .hasArg().argName("FILE")
                    .required()
                    .build());
        } else {
            options.addOption(Option.builder("i").longOpt(FILEARG)
                    .desc("Input file, will be used instead of opening and listening to a serial port")
                    .hasArg().argName("FILE")
                    .build());
            options.addOption(Option.builder("a").longOpt(MODELA)
                    .desc("Force HP 82240A; Ignores the RPL charset escape code and uses the model A bitmaps")
                    .build());
        }
        String portMsg = isSender ? STDOUT : STDIN;
        String description = "Serial port to use (will auto-detect if not specified). '" + portMsg + "' will use the console";
        options.addOption(Option.builder("p").longOpt(PORTARG)
                .desc(description)
                .hasArg().argName("PORT")
                .build());
        // create the command line parser and parse the arguments
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        String port = null;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            result.put(FILEARG, line.getOptionValue("i"));
            if (line.hasOption('a')) {
                result.put(MODELA, MODELA);
            }
            port = line.getOptionValue("p");
            if (port != null) {
                // If the option is given as '-p com1:' instead of '-p=com1:', then the blank will be present. Remove it.
                port = port.trim();
            }
        } catch (MissingOptionException ex) {
            formatter.printHelp(prefix, options);
            System.exit(-1);
        } catch (ParseException exp) {
            log.log(Level.SEVERE, "Unexpected exception:", exp);
            System.exit(-1);
        }
        if (port == null) {
            result.put(PORTARG, ""); // Blank means auto-detect
            return result;
        }
        // Check special cases
        if (isSender && STDOUT.equalsIgnoreCase(port)) {
            result.put(PORTARG, STDOUT);
            return result;
        }
        if (!isSender && STDIN.equalsIgnoreCase(port)) {
            result.put(PORTARG, STDIN);
            return result;
        }
        // Check the port for validity
        boolean isOk = port.contains("com"); // Windows style com port is ok so far
        isOk |= !(port.contains("dev") || port.contains(".")); // Either way: No /dev or .tty or anything
        if (isOk) {
            result.put(PORTARG, port);
        } else {
            result.put(PORTARG, null);
            formatter.printHelp(prefix + ": Specified serial port is invalid. Give only the name, and do not include /dev or a prefix such as tty.", options);
        }
        return result;
    }
}
