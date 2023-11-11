package ch.erzberger.emulation.common;

import lombok.Getter;

@Getter
public enum Hp82240EscapeCodes {
    RESET("reset", (byte)0xFF),
    SELFTEST("selftest", (byte)0xFE),
    START_DOUBLEWIDE("doublewide", (byte)0xFD),
    STOP_DOUBLEWIDE("end_doublewide", (byte)0xFC),
    START_UNDERLINE("underline", (byte)0xFB),
    STOP_UNDERLINE("end_underline", (byte)0xFA),
    START_ISO8859("iso8859", (byte)0xF9),
    STOP_ISO8859("end_iso8859", (byte)0xF8),
    GRAPHICS_MODE("graphics", (byte)0x00);
    private final String textVersion;
    private final byte escCode;

    Hp82240EscapeCodes(String textVersion, byte escCode) {
        this.textVersion = textVersion;
        this.escCode = escCode;
    }
    public static Hp82240EscapeCodes getEscapeCodeByTextVersion(String textVersion) {
        for (Hp82240EscapeCodes value : values()) {
            if (value.getTextVersion().equalsIgnoreCase(textVersion)) {
                return value;
            }
        }
        return GRAPHICS_MODE;
    }

    public static Hp82240EscapeCodes getEscapeCodeByCode(int code) {
        for (Hp82240EscapeCodes value : values()) {
            if (value.escCode == (byte)code) {
                return value;
            }
        }
        return GRAPHICS_MODE;
    }
}
