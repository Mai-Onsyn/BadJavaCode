package mai_onsyn.code_flattor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AXImage {

    private final int[] pixels;
    int width, height;

    public AXImage(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    public AXImage(int[] data, int width, int height) {
        this.width = width;
        this.height = height;
        pixels = data;
    }

    public AXImage(BufferedImage bi) {
        this.width = bi.getWidth();
        this.height = bi.getHeight();
        pixels = new int[width * height];
        bi.getRGB(0, 0, width, height, pixels, 0, width);
    }

    public AXImage(File file) {
        BufferedImage bi;
        try {
            bi = ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.width = bi.getWidth();
        this.height = bi.getHeight();
        pixels = new int[width * height];
        bi.getRGB(0, 0, width, height, pixels, 0, width);
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getPixel(int x, int y) {
        return new Color(pixels[x + y * width]);
    }

    public void setPixel(int x, int y, Color color) {
        pixels[x + y * width] = color.getRGB();
    }

    public void write(File file) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, width, height, pixels, 0, width);
        try {
            ImageIO.write(bi, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AXImage charlize(double charRatio, int horizontalCharCount) {
        int verticalCharCount = (int) Math.round(height * charRatio * horizontalCharCount / width);

        int[] resized = resize(pixels, width, height, horizontalCharCount, verticalCharCount);
        AXImage axImage = new AXImage(resized, horizontalCharCount, verticalCharCount);

        for (int y = 0; y < verticalCharCount; y++) {
            for (int x = 0; x < horizontalCharCount; x++) {
                Color color = axImage.getPixel(x, y);
                if (color.getRed() + color.getGreen() + color.getBlue() > 382) {
                    axImage.setPixel(x, y, Color.BLACK);
                } else {
                    axImage.setPixel(x, y, Color.WHITE);
                }
            }
        }
        return axImage;
    }

    private int[] resize(int[] src, int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        int[] dst = new int[dstWidth * dstHeight];
        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                int srcX = (int) (x * ((double) srcWidth / dstWidth));
                int srcY = (int) (y * ((double) srcHeight / dstHeight));
                dst[y * dstWidth + x] = src[srcY * srcWidth + srcX];
            }
        }
        return dst;
    }

}
