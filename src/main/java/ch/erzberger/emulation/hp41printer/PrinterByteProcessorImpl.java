package ch.erzberger.emulation.hp41printer;

import ch.erzberger.emulation.common.ByteProcessor;
import ch.erzberger.emulation.common.Hp82240EscapeCodes;
import ch.erzberger.emulation.common.Hp82240aCharsetProvider;
import ch.erzberger.emulation.common.RplCharsetProvider;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
@Getter
/*
 * The main printer simulator class. Each line of the printer has 8 columns, and a maximum of 166 columns.
 * One character is in a 5x8 matrix, with one blank column left and right. The bottommost row is only used for
 * descenders (e.g. lowercase "g"). The first and the last blank column is skipped.
 * Mixed in with characters can be graphics blocks. Each block consists of 1 up to 166 bytes, and each byte represents
 * one column of pixels.
 * The line is printed when either a 0x04 or a 0x0A is received. 0x04 is special in that it leaves the print head
 * on the right side of the paper. In a real printer, mixing 0x04 and 0x0A results in vertical mis-alignment of the
 * pixels. This is not currently emulated, however. Therefore, 0x04 and 0x0A work exactly the same in the emulation.
 * In addition to text and graphic blocks, a number of graphic sequences can be processed, e.g. to enable/disable double
 * wide printing.
 * The printer produces two types of output: Pure text (ignoring all graphic blocks, underline or double wide),
 * and a bitmap of one row.
 * The "Paper" class is responsible for "printing" these two outputs as desired.
 *
 */
public class PrinterByteProcessorImpl implements ByteProcessor {
    // graphic sequences
    private static final int ESC = 0x1B;
    private static final int REGULAR_LINEFEED = 0x0A;
    private static final int HP_SPECIAL_LINEFEED = 0x04;

    // Other constants
    private static final int ROWS = 8;
    private static final int COLUMNS = 166;

    // global variables
    private final Paper paper; // Callback for printing the generated output
    private boolean escInProgress = false;  // Last character has been "ESC"
    private boolean doubleWide = false; // Double wide is currently active
    private boolean underline = false; // Underline is currently active
    private boolean iso8859 = false; // Not currently used, would emulate the 82240 B printer

    // Force model A
    private final boolean useModelA;

    // Bitmap and Text buffers, and counter for the current row in the bitmap
    private boolean[][] bitmapBuffer = new boolean[COLUMNS][ROWS];
    private int currentColumn = 0;
    private StringBuilder textBuffer = new StringBuilder();

    // Counter for receiving graphic bytes (rows). The number of rows are part of the byte stream and must be checked.
    private int graphicsBytesRemaining = 0;

    public PrinterByteProcessorImpl(Paper paper, boolean useModelA) {
        this.useModelA = useModelA;
        this.paper = paper;
    }

    /**
     * Main handler, accepts a single byte from the serial stream
     *
     * @param rawInput One byte from the serial stream
     */
    @Override
    public void processByte(byte rawInput) {
        int input = rawInput & 0xFF;
        log.log(Level.FINEST, "Processing byte: {0}", input);
        // Are graphic rows currently being received? If so, append one column to the bitmap buffer.
        if (graphicsBytesRemaining != 0) {
            log.log(Level.FINEST, "Graphics bytes remaining: {0}", graphicsBytesRemaining);
            // Check if the row fits. If not, print the line and then continue.
            if (currentColumn >= COLUMNS) {
                log.log(Level.WARNING, "Received a graphic column beyond the end of the line. Column is: {0}", currentColumn);
                sendToPaper();
            }
            // Append the 8 bits of one column in the bitmap buffer and increase the current column counter
            appendGraphicsColumns(input);
            log.log(Level.FINEST, "Done appending graphics to column, byte is {0}", input);
            // Decrement the graphics bytes counter. Once it hits zero, the next byte is interpreted normally again (ESC, CR or text).
            graphicsBytesRemaining--;
            // Nothing further is checked in graphics mode (CR, ESC etc.). We are done here.
            log.log(Level.FINEST, "Done processing graphics, bytes remaining: {0}", graphicsBytesRemaining);
            return;
        }
        // Check for a line feed
        if (!escInProgress && (input == REGULAR_LINEFEED || input == HP_SPECIAL_LINEFEED)) {
            log.log(Level.FINEST, "Linefeed received");
            sendToPaper();
            return;
        }
        // Check for graphic character, starting an graphic sequence
        if (input == ESC) {
            log.log(Level.FINEST, "Start ESC sequence");
            escInProgress = true;
            return;
        }
        if (!escInProgress) {
            log.log(Level.FINEST, "Appending character as text: {0}", new String(new byte[]{(byte) input}, Hp82240aCharsetProvider.hp82240a()));
            // If no graphic sequence is in progress, then append the character to the current line
            if (iso8859 && !useModelA) {
                textBuffer.append(new String(new byte[]{(byte) input}, RplCharsetProvider.Rpl()));
            } else {
                textBuffer.append(new String(new byte[]{(byte) input}, Hp82240aCharsetProvider.hp82240a()));
            }
            // Also append the character to the graphics buffer
            appendCharToGraphicsBuffer(input);
            return;
        }
        // escape sequence is in progress, check which one it is
        Hp82240EscapeCodes code = Hp82240EscapeCodes.getEscapeCodeByCode(input);
        switch (code) {
            case START_DOUBLEWIDE:
                log.log(Level.FINE, "Start doublewide");
                doubleWide = true;
                break;
            case STOP_DOUBLEWIDE:
                log.log(Level.FINE, "Stop doublewide");
                doubleWide = false;
                break;
            case START_UNDERLINE:
                log.log(Level.FINE, "Start underline");
                underline = true;
                break;
            case STOP_UNDERLINE:
                log.log(Level.FINE, "Stop underline");
                underline = false;
                break;
            case START_ISO8859:
                log.log(Level.FINE, "Start 82240B mode");
                iso8859 = true;
                break;
            case STOP_ISO8859:
                log.log(Level.FINE, "Stop 82240B mode");
                iso8859 = false;
                break;
            case RESET:
                log.log(Level.FINE, "Reset printer");
                reset();
                break;
            case SELFTEST:
                log.log(Level.FINE, "Self test");
                reset();
                selfTest();
                break;
            default:
                log.log(Level.FINE, "Start graphics mode for {0} bytes", input);
                startGraphicMode(input);
                break;
        }
        escInProgress = false;
    }

    private void reset() {
        doubleWide = false;
        underline = false;
        iso8859 = false;
        graphicsBytesRemaining = 0;
        textBuffer = new StringBuilder();
    }

    private void startGraphicMode(int length) {
        // Start the graphics mode by setting the remaining columns to the specified number.
        graphicsBytesRemaining = length;
        // Check the length: It must be between 1 and 166
        if (graphicsBytesRemaining < 1 || graphicsBytesRemaining > COLUMNS) {
            log.log(Level.SEVERE, "Graphics mode with invalid length requested. Length is {0}", graphicsBytesRemaining);
        }
    }

    private void appendCharToGraphicsBuffer(int character) {
        log.log(Level.FINEST, "Appending one character to the bitmap buffer. Currrent column: {0}", currentColumn);
        // Check if the character fits onto the current line.
        // The last one needs 6 pixels (single wide) or 12 pixels (double wide) to fit
        if ((doubleWide && currentColumn > COLUMNS-12) || (!doubleWide && currentColumn > COLUMNS-6)) {
            sendToPaper();
        }
        // Left of any char is en empty row, except for the first character on a line
        padCharLeft();
        // Get the bitmap for the character
        int[] bitmap;
        if (useModelA) {
            bitmap = HpPrinterFonts.getHp82240aBitmap(character);
        } else {
            if (iso8859) {
                bitmap = HpPrinterFonts.getRplBitmap(character);
            } else {
                bitmap = HpPrinterFonts.getHp82240bBitmap(character);
            }
        }
        // Patch the bitmap to include an underscore if necessary
        addUnderLineIfNecessary(bitmap);
        // The bitmap contains the 5 bytes that make up the columns of the character.
        for (int charColumn = 0; charColumn < 5; charColumn++) {
            int bitsForCharColumn = bitmap[charColumn];
            appendColumnToBitmap(bitmapBuffer, currentColumn, bitsForCharColumn);
            currentColumn++;
            if (doubleWide) {
                // Append the same row again
                appendColumnToBitmap(bitmapBuffer, currentColumn, bitsForCharColumn);
                currentColumn++;
            }
        }
        // Append one or two blank rows at the right
        padCharRight();
    }

    private void addBlankOrUnderlinedColumn() {
        int charToAdd = underline ? 0b10000000 : 0; // The extra column may be underlined
        appendGraphicsColumns(charToAdd);
    }

    private void padCharLeft() {
        // Left of any char is an empty row, except for the first character on a line
        if (currentColumn == 0) {
            return;
        }
        addBlankOrUnderlinedColumn();
    }

    private void padCharRight() {
        // Right of any char is an empty row, except for the last character on a line
        if (currentColumn > COLUMNS - (doubleWide ? 2 : 1)) {
            return;
        }
        addBlankOrUnderlinedColumn();
    }

    private void addUnderLineIfNecessary(int[] bitmap) {
        if (underline) {
            // Character is 5 bytes, and on each byte the leftmost bit needs to be set
            for (int i = 0; i < bitmap.length; i++) {
                bitmap[i] |= 0b10000000;
            }
        }
    }

    private void appendGraphicsColumns(int column) {
        if (currentColumn >= COLUMNS) {
            log.log(Level.SEVERE, "Column too big: {0}", column);
            return;
        }
        // Add the underline if necessary
        if (underline) {
            column |= 0b10000000;
        }
        appendColumnToBitmap(bitmapBuffer, currentColumn, column);
        currentColumn++;
        // Double if necessary
        if (doubleWide) {
            appendColumnToBitmap(bitmapBuffer, currentColumn, column);
            currentColumn++;
        }
    }

    private void appendColumnToBitmap(boolean[][] buffer, int indexToAppend, int byteToAppend) {
        buffer[indexToAppend][0] = (byteToAppend & 0b00000001) > 0;
        buffer[indexToAppend][1] = (byteToAppend & 0b00000010) > 0;
        buffer[indexToAppend][2] = (byteToAppend & 0b00000100) > 0;
        buffer[indexToAppend][3] = (byteToAppend & 0b00001000) > 0;
        buffer[indexToAppend][4] = (byteToAppend & 0b00010000) > 0;
        buffer[indexToAppend][5] = (byteToAppend & 0b00100000) > 0;
        buffer[indexToAppend][6] = (byteToAppend & 0b01000000) > 0;
        buffer[indexToAppend][7] = (byteToAppend & 0b10000000) > 0;
    }

    private void sendToPaper() {
        // Text output
        paper.printLine(textBuffer.toString());
        textBuffer = new StringBuilder(); // Clear the current line
        // Graphic output
        paper.printGraphic(bitmapBuffer);
        bitmapBuffer = new boolean[COLUMNS][ROWS]; // Clear the bitmap buffer
        currentColumn = 0;
    }

    private void selfTest() {
        // Start with an empty line
        sendToPaper();
        // Fill an array with all code points
        byte[] allChars = new byte[223];
        for (int i = 32; i < 255; i++) {
            allChars[i - 32] = (byte) i;
        }
        // Specifically put an underscore in the first position
        allChars[0] = 95;
        // Dump the array in increments of 24 chars
        char[] fullOutputArray = new String(allChars, Hp82240aCharsetProvider.hp82240a()).toCharArray();
        // 10 Lines, last one is incomplete
        for (int i = 0; i < fullOutputArray.length; i++) {
            char currentChar = fullOutputArray[i];
            textBuffer.append(currentChar);
            // For the graphics part we need the original codepoint, not the UTF-8 code. This is simply "i" plus 32.
            // Except for position 0: The real printer prints an underscore there
            if (i == 0) { //NOSONAR False positive
                appendCharToGraphicsBuffer(95);
            } else {
                appendCharToGraphicsBuffer(i + 32);
            }
            if ((i + 1) % 24 == 0) {
                sendToPaper();
            }
        }
        // Some character is appended with the real printer. Could be the firmware release, or some factory indicator
        // In my printer it is "D", so this is what we use here
        textBuffer.append(" D");
        appendCharToGraphicsBuffer(32); // Blank
        appendCharToGraphicsBuffer(68); // D
        sendToPaper();
        sendToPaper(); // Blank line
        // Finally a battery indicator is printed. Use a middle value here (1-5, i.e. 3)
        textBuffer.append("BAT: 3"); // Level goes from 1 to 5, just use a middle value here.
        appendCharToGraphicsBuffer(66); // B
        appendCharToGraphicsBuffer(65); // A
        appendCharToGraphicsBuffer(84); // T
        appendCharToGraphicsBuffer(58); // :
        appendCharToGraphicsBuffer(32); // Blank
        appendCharToGraphicsBuffer(51); // 3
        sendToPaper();
        // End with an empty line
        sendToPaper();
    }
}
