package ch.erzberger.emulation.common;

import lombok.Data;

import java.util.List;

/**
 * A YAML formatted file is read into this class. It represents an input file that can be
 * converted into a series of bytes to be sent to an HP 82240A printer. It contains a mix of
 * Text string in UTF-8 format, graphic Sequences and Line Feeds (Normal or HP).
 * These can appear in any order.
 */
@Data
public class Hp84440PrinterFile {
    private String title;
    private String purpose;
    private List<Hp82240PrintData> hp82240PrintData;
}
