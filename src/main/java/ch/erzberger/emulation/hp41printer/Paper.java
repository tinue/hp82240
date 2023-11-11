package ch.erzberger.emulation.hp41printer;

public interface Paper {
    /**
     * Print the text part of the output to Paper.
     * @param line The line to append to the paper.
     */
    void printLine(String line);
    /**
     * Print a pseudo-bitmap to Paper graphically.
     * @param bitmap The pseudo-bitmap.
     */
    void printGraphic(boolean[][] bitmap);
}
