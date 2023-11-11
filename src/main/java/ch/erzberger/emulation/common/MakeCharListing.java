package ch.erzberger.emulation.common;

import java.util.HexFormat;

/**
 * Throw-away helper to produce parts of the input for the file "charlisting.yaml"
 */
public class MakeCharListing {
    public static void main(String[] args) {
        // The "String" is actually a hex sequence
        HexFormat hex = HexFormat.of();
        for (int i = 32; i < 256; i++) {
            String hexVerOfI = hex.toHexDigits((byte)i).toUpperCase();
            StringBuilder line = new StringBuilder();
            line.append("    - hex: ");
            // The Character number in decimal ( 32 until 255) and two blanks
            String charNo = Integer.toString(i);
            if (charNo.length() == 2) {
                charNo = " " + charNo;
            }
            charNo = charNo + "  ";
            byte[] charNoBytes = charNo.getBytes();
            line.append(hex.formatHex(charNoBytes));
            // i, in hex as String (e.g. for i=32 we need the string "20")
            line.append(hexVerOfI);
            // Two more blanks, and the escape sequence to start ECMA mode
            line.append("20201BF9");
            // Again i in hex
            line.append(hexVerOfI);
            // Back to Roman 8, and line feed
            line.append("1BF804");
            System.out.println(line.toString());
        }
    }

}
