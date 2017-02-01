package org.orangepalantir;

import sun.security.util.ByteArrayTagOrder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * A short example of using ImageIO to create an svg file with an image embedded in the file itself.
 *
 * Created on 01/02/2017.
 */
public class SvgEmbedPng {
    static final String DOCTYPE = "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \n" +
            "  \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n";
    static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
    static final String SVG_TAG = "<svg width=\"%.2fpx\" height=\"%.2fpx\" viewBox=\"0 0 %s %s\"\n"+
            "    xmlns=\"http://www.w3.org/2000/svg\"\n"+
            "xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"+
            " version=\"1.1\">\n";


    private BufferedImage img;

    public SvgEmbedPng(BufferedImage img){
        this.img = img;
    }

    /**
     *  Encodes the image into a Base64 string and puts the data into an image tag.
     *
     * @return a format string that needs the position and dimensions included.
     *         &lt;image y="%s" x="%s" id="%s" xlink:href="data:image/png;base64,xxx" height="%s" width="%s"/&gt;
     */
    public String getImageTag() throws IOException {
        ByteArrayOutputStream encoder = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", encoder);
        String encoded = Base64.getEncoder().encodeToString(encoder.toByteArray());

        return "<image y=\"%s\"\n x=\"%s\"\n id=\"%s\"\n xlink:href=\"data:image/png;base64," +
                encoded +
                "\"\n height=\"%s\"\n width=\"%s\"\n/>";
    }

    /**
     * Utility for sizing and positioning.
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param id
     * @return
     * @throws IOException
     */
    public String getImageTag(String x, String y, String width, String height, String id) throws IOException {
        return String.format(getImageTag(), y, x, id, height, width);
    }

    public void saveSvg(Path p) throws IOException {
        int w = img.getWidth(null);
        int h = img.getHeight(null);

        BufferedWriter writes = Files.newBufferedWriter(p, StandardCharsets.UTF_8);

        writes.write(XML);
        writes.write(DOCTYPE);
        writes.write(String.format(SVG_TAG, w*1.0, h*1.0, w, h));
        writes.write(getImageTag("0", "0", "" + w, "" + h, "image1"));

        writes.write("</svg>");
        writes.close();
    }

    public static void main(String[] args){
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = img.getGraphics();
        g.setColor(Color.PINK);
        g.fillOval(0, 0, 400, 400);
        g.dispose();
        try {
            new SvgEmbedPng(img).saveSvg(Paths.get("sample.svg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
