package ch.erzberger.emulation.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.spi.CharsetProvider;
import java.text.Normalizer;
import java.util.*;

/**
 * Mapping from the HP Focal printer character set to Unicode.
 * To find the correct mapping, I consulted this Wikipedia Article:
 * <a href="https://en.wikipedia.org/wiki/FOCAL_character_set">FOCAL character set</a>
 * The template for this CharsetProvider comes from a Stack Overflow answer:
 * <a href="https://stackoverflow.com/questions/76936549/charset-not-present-in-used-jdk/76936879#76936879">Charset not present in used JDK</a>
 */
public class FocalCharsetProvider extends CharsetProvider {
    private static final Charset FOCAL = new FocalCharset();
    private static final List<Charset> FOCAL_LIST = Collections.singletonList(FOCAL);

    /**
     * Use this helper to get access to the charset provider. Alternatively, register the provider formally.
     * @return The provider for the HP-82240A charset
     */
    public static Charset focal() {
        return FOCAL;
    }

    @Override
    public Iterator<Charset> charsets() {
        return FOCAL_LIST.iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        if (charsetName.equals(FOCAL.name())) return FOCAL;
        if (FOCAL.aliases().contains(charsetName)) return FOCAL;

        return null;
    }
}

/**
 * The mapping table.
 */
@SuppressWarnings("UnnecessaryUnicodegraphic")
class FocalCharset extends Charset {
    private static final char specialX = Normalizer.normalize("\u2A30x", Normalizer.Form.NFC).charAt(0);

    private static final char[] FOCAL = {
            '\u2666', '\u221d', specialX, '\u2190', '\u03b1', '\u03b2', '\u0393', '\u2193',
            '\u0394', '\u03c3', '\u2666', '\u03bb', '\u03bc', '\u2221', '\u03c4', '\u03a6',
            '\u03b8', '\u03a9', '\u0026', '\u00c5', '\u00e5', '\u00c4', '\u00e4', '\u00d6',
            '\u00f6', '\u00dc', '\u00fc', '\u00c6', '\u00e6', '\u2260', '\u00a3', '\u2592',
            ' ',      '!',      '"',      '#',      '$',      '%',      '&',      '\'',
            '(',      ')',      '*',      '+',      ',',      '-',      '.',      '/',
            '0',      '1',      '2',      '3',      '4',      '5',      '6',      '7',
            '8',      '9',      ':',      ';',      '<',      '=',      '>',      '?',
            '@',      'A',      'B',      'C',      'D',      'E',      'F',      'G',
            'H',      'I',      'J',      'K',      'L',      'M',      'N',      'O',
            'P',      'Q',      'R',      'S',      'T',      'U',      'V',      'W',
            'X',      'Y',      'Z',      '[',      '\\',     ']',      '\u2191', '_',
            '\u22a4', 'a',      'b',      'c',      'd',      'e',      'f',      'g',
            'h',      'i',      'j',      'k',      'l',      'm',      'n',      'o',
            'p',      'q',      'r',      's',      't',      'u',      'v',      'w',
            'x',      'y',      'z',      '\u03c0', '|',      '\u2192', '\u03a3', '\u22a6',
    };
    private static final Map<Character, Byte> LOOKUP;

    static {
        Map<Character, Byte> map = new HashMap<>();
        for (int i = 0; i < FOCAL.length; i++) map.put(FOCAL[i], (byte) i);
        LOOKUP = Collections.unmodifiableMap(map);
    }

    public FocalCharset() {
        super("FOCAL", new String[]{"Focal", "CpFocal", "Cp-Focal"});
    }

    @Override
    public boolean contains(Charset cs) {
        return cs.name().equals("FOCAL"); // It isn't even a subset of ASCII!
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoder(this, 1F, 1F) {
            @Override
            protected CoderResult decodeLoop(ByteBuffer from, CharBuffer to) {
                while (from.hasRemaining()) {
                    if (!to.hasRemaining()) return CoderResult.OVERFLOW;
                    byte c = from.get();
                    char d = FOCAL[c & 0xFF];
                    to.put(d);
                }

                return CoderResult.UNDERFLOW;
            }
        };
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoder(this, 1F, 1F) {
            @Override
            protected CoderResult encodeLoop(CharBuffer from, ByteBuffer to) {
                while (from.hasRemaining()) {
                    if (!to.hasRemaining()) return CoderResult.OVERFLOW;
                    char d = from.get();
                    Byte v = LOOKUP.get(d);
                    if (v == null) {
                        // 'un'consume the character we consumed
                        from.position(from.position() - 1);
                        return CoderResult.unmappableForLength(1);
                    }
                    to.put(v);
                }

                return CoderResult.UNDERFLOW;
            }
        };
    }
}
