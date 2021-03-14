package org.ofdrw.converter.utils;

import org.apache.pdfbox.jbig2.JBIG2ImageReader;
import org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi;
import org.apache.pdfbox.jbig2.io.DefaultInputStreamFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class ImageUtils {

    public static byte[] toBytes(BufferedImage bufferedImage, String type) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean flag = ImageIO.write(bufferedImage, type, out);
        byte[] bytes = out.toByteArray();
        return bytes;
    }

    public static BufferedImage readJB2(InputStream inputStream) {

        int imageIndex = 0;
        JBIG2ImageReader imageReader = null;
        try {

            DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
            ImageInputStream imageInputStream = disf.getInputStream(inputStream);

            imageReader = new JBIG2ImageReader(new JBIG2ImageReaderSpi());
            imageReader.setInput(imageInputStream);
            BufferedImage bufferedImage = imageReader.read(imageIndex, imageReader.getDefaultReadParam());

            return bufferedImage;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static BufferedImage renderMask(BufferedImage image, BufferedImage mask) {
        if ((image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight())) {
            return image;
        }
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setColor(Color.WHITE);
        out = graphics.getDeviceConfiguration().createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = mask.getRGB(x, y);
                int r = 0xFF & rgb;
                int g = 0xFF00 & rgb;
                g >>= 8;
                int b = 0xFF0000 & rgb;
                b >>= 16;
                boolean sholdMask = (r + g + b) / 3 > 244;
                if (sholdMask) {
                    out.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        return out;
    }
}
