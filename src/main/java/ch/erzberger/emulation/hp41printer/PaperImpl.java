package ch.erzberger.emulation.hp41printer;

import lombok.extern.java.Log;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;

@Log
public class PaperImpl implements Paper {
    private static final String TEXTFILE = "Hp8224-Text.txt";
    private static final String PNGFILE = "Hp8224-Image.png";
    private static final int PADLEFT = 22;
    private static final int PADRIGHT = 19;
    private static final int PADTOPBOTTOM = 10;
    private final StringBuilder textCache = new StringBuilder();
    // As long as the Paper instance is running it keeps appending to the same image buffer. On ever append
    // the buffer will also be written to disk. When the instance restarts then the buffer will be empty.
    private BufferedImage imageCache = null;

    public void printLine(String line) {
        // Append the text buffer
        textCache.append(line);
        textCache.append('\n');
        if (log.isLoggable(Level.INFO)) {
            if (line.isEmpty()) {
                log.log(Level.FINE, "Line feed");
            } else {
                log.log(Level.INFO, "Printing: {0}", line);
            }
        }
        // Delete and re-create the output file
        Path textFile = Paths.get(TEXTFILE);
        try {
            Files.deleteIfExists(textFile);
            Files.createFile(textFile);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot create the text file", ex);
        }
        // Write the buffer into the file
        try {
            Files.writeString(textFile, textCache);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot write to the text file", ex);
        }
    }

    public void printGraphic(boolean[][] bitmap) {
        // Produce the new image which gets appended to the existing image
        BufferedImage newImageAtBottom = arrayToBMP(bitmap);
        // Append the image
        imageCache = combineImages(imageCache, newImageAtBottom);
        if (imageCache == null) {
            log.log(Level.SEVERE, "Combining the images failed");
            return;
        }
        // Pad the image for output
        BufferedImage paddedImage = padImage(imageCache, false, PADLEFT, PADRIGHT, PADTOPBOTTOM);
        try {
            saveImage(new File(PNGFILE), paddedImage);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot write the image file", ex);
        }
    }

    /**
     * Convert the internal pseudo-bitmap into a BufferedImage
     *
     * @param pixelData Pseudo-Btmap
     * @return Converted image
     */
    private BufferedImage arrayToBMP(boolean[][] pixelData) {
        BufferedImage img = new BufferedImage(pixelData.length, pixelData[0].length, BufferedImage.TYPE_BYTE_BINARY);
        for (int row = 0; row < pixelData.length; row++) {
            boolean[] theRow = pixelData[row];
            for (int column = 0; column < theRow.length; column++) {
                int value = theRow[column] ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                img.setRGB(row, column, value);
            }
        }
        return img;
    }

    /**
     * Combine two images of equal width.
     *
     * @param top    The image that goes at the top
     * @param bottom The image that goes at the bottom
     * @return The combined image
     */
    private BufferedImage combineImages(BufferedImage top, BufferedImage bottom) {
        if (top == null) {
            // Simply return the bottom image, there is nothing to combine
            return bottom;
        }
        // Width of the images must be the same
        if (top.getWidth() != bottom.getWidth()) {
            log.log(Level.SEVERE, "New image has wrong number of columns. Expected: {0}, actual: {1}", new Object[]{top.getWidth(), bottom.getWidth()});
            return top;
        }
        // Make a new BufferedImage with the combined size
        BufferedImage totalImage = new BufferedImage(top.getWidth(), top.getHeight() + bottom.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = totalImage.createGraphics();
        g2.drawImage(top, null, 0, 0);
        g2.drawImage(bottom, null, 0, top.getHeight());
        return totalImage;
    }

    /**
     * Pad the image with white space in order to simulate a paper that is wider than the print area.
     * Also draw a one pixel wide black frame around the outer edges.
     *
     * @param unpaddedImage The input image
     * @return The image padded with whitespace
     */
    @SuppressWarnings("SameParameterValue")
    private BufferedImage padImage(BufferedImage unpaddedImage, boolean addBorder, int padLeft, int padRight, int padTopBottom) {
        int width = unpaddedImage.getWidth() + padLeft + padRight + (addBorder ? 2 : 0);
        int height = unpaddedImage.getHeight() + padTopBottom + padTopBottom + (addBorder ? 2 : 0);
        BufferedImage paddedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = paddedImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        if (addBorder) {
            g2.setColor(Color.BLACK);
            g2.drawRect(0, 0, width - 1, height - 1);
        }
        g2.drawImage(unpaddedImage, null, padLeft + (addBorder ? 1 : 0), padTopBottom + (addBorder ? 1 : 0));
        return paddedImage;
    }

    private void saveImage(File output, BufferedImage image) throws IOException {
        // Search for the "png" image writer with editable metadata (apparently there is more than one png writer)
        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext(); ) {
            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                continue;
            }
            // Found it; Set the dpi value
            setDPI(metadata);
            // Write the file
            try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
            }
            // Done, simply return (don't continue with the loop)
            return;
        }
    }

    private void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {
        // PMG files need dots per millimeter, convert the previously measured values
        double horizDotsPerMilli = 1.0 * 92 / 10 / 2.54; // Horizontal resolution is 92 dpi
        double vertiDotsPerMilli = 1.0 * 76 / 10 / 2.54; // Vertical is 76

        // Metadata for horizontal pixels
        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(horizDotsPerMilli));
        // Metadata for vertical pixels
        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(vertiDotsPerMilli));
        // Make a "dimension" node and add the two dimensions
        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);
        // Make the root node and append the domension
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);
        // Fix internal state of the metadata
        metadata.mergeTree("javax_imageio_1.0", root);
    }
}
