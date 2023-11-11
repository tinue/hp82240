package ch.erzberger.emulation.common;

import lombok.Data;

/**
 * One element of printable data. It can be either an graphic sequence, a printable string,
 * or a line feed.
 */
@Data public class Hp82240PrintData {
    private String text;
    private String graphic;
    private String linefeed;
    private String esc;
    private String hex;
}
