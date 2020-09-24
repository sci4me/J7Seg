package com.sci.seg7;

import java.text.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;

public final class SevenSegmentDisplay extends JComponent {
    private static BufferedImage[][] images = new BufferedImage[8][2];

    private static boolean init;

    private static void init() {
        if(!SevenSegmentDisplay.init) {
            SevenSegmentDisplay.init = true;
        } else {
            return;
        }

        try {
            images[0][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/A.png"));
            images[1][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/B.png"));
            images[2][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/C.png"));
            images[3][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/D.png"));
            images[4][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/E.png"));
            images[5][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/F.png"));
            images[6][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/G.png"));
            images[7][0] = ImageIO.read(SevenSegmentDisplay.class.getResourceAsStream("/assets/DP.png"));
        
            for(int i = 0; i < images.length; i++) {
                final BufferedImage off = images[i][0];
                final BufferedImage on = new BufferedImage(off.getWidth(), off.getHeight(), BufferedImage.TYPE_INT_ARGB);
                images[i][1] = on;

                for(int x = 0; x < off.getWidth(); x++) {
                    for(int y = 0; y < off.getHeight(); y++) {
                        final int original = off.getRGB(x, y);
                        final int a = (original >> 24) & 0xFF;
                        if(a > 0) {
                            on.setRGB(x, y, (a << 24) | (255 << 16));
                        }
                    }
                }
            }
        } catch(final Throwable t) {
            t.printStackTrace();
        }
    }

    private static BufferedImage getImage(final int part, final boolean state) {
        return images[part][state ? 1 : 0];
    }

    private static BufferedImage getScaledImage(final BufferedImage image, final int width, final int height) {
        final int imageWidth  = image.getWidth();
        final int imageHeight = image.getHeight();

        final double scaleX = (double) width / imageWidth;
        final double scaleY = (double) height / imageHeight;
        final AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        final AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        return bilinearScaleOp.filter(image, new BufferedImage(width, height, image.getType()));
    }

    private static int charToInt(final char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }

    private static final boolean[][] digitTable = {
        { true, true, true, true, true, true, false, false },
        { false, true, true, false, false, false, false, false },
        { true, true, false, true, true, false, true, false },
        { true, true, true, true, false, false, true, false },
        { false, true, true, false, false, true, true, false },
        { true, false, true, true, false, true, true, false },
        { true, false, true, true, true, true, true, false },
        { true, true, true, false, false, false, false, false },
        { true, true, true, true, true, true, true, false },
        { true, true, true, false, false, true, true, false },
        { true, true, true, false, true, true, true, false },
        { false, false, true, true, true, true, true, false },
        { true, false, false, true, true, true, false, false },
        { false, true, true, true, true, false, true, false },
        { true, false, false, true, true, true, true, false },
        { true, false, false, false, true, true, true, false }
    };

    public final DisplayMode displayMode;

    public final int digits;
    public final int digitsAfterDecimal;
    private final int totalDigits;

    private Color backgroundColor;

    private BufferedImage[][] scaledImageCache;
    private int lastWidth;
    private int lastHeight;

    private NumberFormat format;
    
    private double value;

    private int[] digitValues;
    private int[] digitAfterDecimalValues;

    public SevenSegmentDisplay(final int digits) {
        this(digits, DisplayMode.DECIMAL);
    }

    public SevenSegmentDisplay(final int digits, final DisplayMode mode) {
        this(digits, 0, mode);
    }

    public SevenSegmentDisplay(final int digits, final int digitsAfterDecimal) {
        this(digits, digitsAfterDecimal, DisplayMode.DECIMAL);
    }

    private SevenSegmentDisplay(final int digits, final int digitsAfterDecimal, final DisplayMode mode) {
        SevenSegmentDisplay.init();

        if(digits < 1) {
            throw new IllegalArgumentException("Seven segment display must have at least 1 digit");
        }
        
        this.digits = digits;
        this.digitsAfterDecimal = digitsAfterDecimal;
        this.displayMode = mode;
        this.totalDigits = this.digits + this.digitsAfterDecimal;

        this.digitValues = new int[this.digits];
        
        if(this.digitsAfterDecimal > 0) {
            this.digitAfterDecimalValues = new int[this.digitsAfterDecimal];
        }

        this.backgroundColor = new Color(30, 30, 30);

        this.setSize(188 * this.totalDigits, 300);

        if(this.displayMode == DisplayMode.DECIMAL) {
            String format = "#";
            for(int i = 0; i < this.digits; i++) {
                format += "0";
            }

            if(this.digitsAfterDecimal > 0) {
                format += ".";
                for(int i = 0; i < this.digitsAfterDecimal; i++) {
                    format += "0";
                }
            }

            this.format = new DecimalFormat(format);
        }
    }

    public void setValue(final double value) {
        this.value = value;

        final String formattedNumber;
        if(this.displayMode == DisplayMode.DECIMAL) {
            formattedNumber = this.format.format(this.value);
        } else {
            formattedNumber = String.format("%0" + this.digits + "X", (long) this.value);
        }

        final String sDigits;
        final String sDigitsAfterDecimal;

        if(formattedNumber.contains(".")) {
            sDigits = formattedNumber.substring(0, formattedNumber.indexOf("."));
            sDigitsAfterDecimal = formattedNumber.substring(formattedNumber.indexOf(".") + 1);
        } else {
            sDigits = formattedNumber;
            sDigitsAfterDecimal = "";
        }

        for(int i = this.digitValues.length - 1; i >= 0; i--) {
            if(i < sDigits.length()) {
                this.digitValues[i] = SevenSegmentDisplay.charToInt(sDigits.charAt(i));
            } else {
                this.digitValues[i] = 0;
            }
        }

        if(this.digitAfterDecimalValues != null) {
            for(int i = this.digitAfterDecimalValues.length - 1; i >= 0; i--) {
                if(i < sDigitsAfterDecimal.length()) {
                    this.digitAfterDecimalValues[i] = SevenSegmentDisplay.charToInt(sDigitsAfterDecimal.charAt(i));
                } else {
                    this.digitAfterDecimalValues[i] = 0;
                }
            }
        }
        
        this.repaint();
    }

    public double getValue() {
        return this.value;
    }
    
    public void setBackgroundColor(final Color color) {
        this.backgroundColor = color;
        this.repaint();
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    @Override
    public void paintComponent(final Graphics g) {
        final int width = this.getWidth();
        final int height = this.getHeight();

        if(width != this.lastWidth || height != this.lastHeight) {
            this.lastWidth = width;
            this.lastHeight = height;

            if(this.scaledImageCache != null) {
                for(int i = 0; i < this.scaledImageCache.length; i++) {
                    for(int j = 0; j < this.scaledImageCache[i].length; j++) {
                        this.scaledImageCache[i][j] = null;
                    }
                }
            }
        }

        g.setColor(this.backgroundColor);
        g.fillRect(0, 0, width, height);

        final int digitWidth = width / this.totalDigits;

        int x = 0;
        for(int digit = 0; digit < this.digits; digit++) {
            if(digit < this.digitValues.length) {
                this.drawDigit(g, x, 0, digitWidth, height, this.digitValues[digit], (digit == (this.digits - 1) && this.digitsAfterDecimal > 0));
            }
            x += digitWidth;
        }

        for(int digit = 0; digit < this.digitsAfterDecimal; digit++) {
            if(digit < this.digitAfterDecimalValues.length) {
                this.drawDigit(g, x, 0, digitWidth, height, this.digitAfterDecimalValues[digit], false);
            }
            x += digitWidth;
        }
    }

    private void drawDigit(final Graphics g, final int x, final int y, final int width, final int height, final int digit, final boolean drawDecimalPoint) {
        if(digit < 0 || digit > 15) {
            throw new IllegalArgumentException("Digits can only be 0 through 15");
        }

        final boolean[] parts = SevenSegmentDisplay.digitTable[digit];
        for(int i = 0; i < (drawDecimalPoint ? 8 : 7); i++) {
            final boolean s = i == 7 ? true : parts[i];
            final BufferedImage originalImage = SevenSegmentDisplay.getImage(i, s);
            
            if(width != originalImage.getWidth() || height != originalImage.getHeight()) {
                final BufferedImage scaledImage;
                
                final int j = s ? 1 : 0;
                if(this.scaledImageCache == null) {
                    this.scaledImageCache = new BufferedImage[8][2];
                    scaledImage = SevenSegmentDisplay.getScaledImage(originalImage, width, height);
                    this.scaledImageCache[i][j] = scaledImage;
                } else {
                    if(this.scaledImageCache[i][j] == null) {
                        scaledImage = SevenSegmentDisplay.getScaledImage(originalImage, width, height);
                        this.scaledImageCache[i][j] = scaledImage;
                    } else {
                        scaledImage = this.scaledImageCache[i][j];
                    }
                }        
                
                g.drawImage(scaledImage, x, y, null);
            } else {
                g.drawImage(originalImage, x, y, null);
            }
        }
    }

    public static enum DisplayMode {
        DECIMAL,
        HEX;
    }
}