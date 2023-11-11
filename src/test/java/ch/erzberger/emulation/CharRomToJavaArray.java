package ch.erzberger.emulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Throw-away class to convert the character rom of the HP 82143a printer into
 * a Java array. Copy / paste the result into HpPrinterFonts.
 * The printer ROM was found in this <a href="https://www.hpmuseum.org/forum/thread-20043-post-177650.html#pid177650">post</a>.
 * Starting on 0x0D80, each character is 5 bytes. They need to be reversed to match the way HpPrinterFonts.java expects
 * them.
 * Use a text editor to extract the bytes from 0x0D80 into one lone string, without line breaks or blanks in between.
 */
public class CharRomToJavaArray {
    public static void main(String[] args) {
        File file = new File("src/test/resources/charrom.txt");
        byte[] buffer = new byte[10];
        try (FileInputStream fis = new FileInputStream(file)) {
            for (int charPos = 0; charPos < 128; charPos++) {
                int read = fis.read(buffer);
                if (read != 10) {
                    System.err.printf("Not enough data in file. I is: %s; Could only read %s chars%n", charPos, read);
                    System.exit(-1);
                }
                String wrongOrder = new String(buffer);
                String theArray = String.format("tempMap.put(%s, new int[]{0x%s, 0x%s, 0x%s, 0x%s, 0x%s});", charPos, wrongOrder.substring(8, 10), wrongOrder.substring(6, 8), wrongOrder.substring(4, 6), wrongOrder.substring(2, 4), wrongOrder.substring(0, 2));
                System.out.println(theArray);
            }
        } catch (IOException ex) {
            System.err.println("Error when reading file: " + ex.getMessage());
        }
    }
}
