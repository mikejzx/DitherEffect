
/* Floyd-Steinberg dithering algorithm */
// As found on the wiki page
// https://en.wikipedia.org/wiki/Floyd-Steinberg_dithering

package io.mikejzx.github.ditheringtests;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javafx.scene.paint.Color;
import java.lang.Math;

public fun main(args: Array<String>) {
    MainClass().invoke();
}

class MainClass {

    val pathInput: String = "../res/lenna.png";
    val pathOutput: String = "../res/result.png";

    public fun invoke() {
        println("Invoked");

        val img: BufferedImage = ImageIO.read(File(pathInput));
        val w = img.width.toInt();
        val h = img.height.toInt();
        computeImage(SwingFXUtils.toFXImage(img, null), w, h);
    }

    private fun computeImage(img: WritableImage, w: Int, h: Int) {
        val pReader = img.pixelReader;
        val pWriter = img.pixelWriter;

        val threshold = arrayOf(64, 64, 64); // 256/4
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val oldpixel = pReader.getColor(x, y);
                val newpixel = getPaletteColour(oldpixel);
                pWriter.setColor(x, y, newpixel);
                val err: Color = Color.rgb(
                    clamp(255.0f * (oldpixel.getRed().toFloat()   - newpixel.getRed().toFloat())  , 0.0f, 255.0f).toInt(),
                    clamp(255.0f * (oldpixel.getGreen().toFloat() - newpixel.getGreen().toFloat()), 0.0f, 255.0f).toInt(),
                    clamp(255.0f * (oldpixel.getBlue().toFloat()  - newpixel.getBlue().toFloat()) , 0.0f, 255.0f).toInt()
                );
                giveQuantErr(x + 1, y    , 7.0f / 16.0f, err, pWriter, pReader);
                giveQuantErr(x - 1, y + 1, 3.0f / 16.0f, err, pWriter, pReader);
                giveQuantErr(x    , y + 1, 5.0f / 16.0f, err, pWriter, pReader);
                giveQuantErr(x + 1, y + 1, 1.0f / 16.0f, err, pWriter, pReader);
            }
        }
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File(pathOutput));
    }

    private fun giveQuantErr(x: Int, y: Int, z: Float, err: Color, pWriter: PixelWriter, pReader: PixelReader) {
        val c = pReader.getColor(x, y);
        val col = Color.rgb(
            clamp(c.getRed().toFloat()   * 255.0f + (err.getRed().toFloat()   * 255.0f * z), 0.0f, 255.0f).toInt(),
            clamp(c.getGreen().toFloat() * 255.0f + (err.getGreen().toFloat() * 255.0f * z), 0.0f, 255.0f).toInt(),
            clamp(c.getBlue().toFloat()  * 255.0f + (err.getBlue().toFloat()  * 255.0f * z), 0.0f, 255.0f).toInt()
        );
        pWriter.setColor(x, y, col);
    }

    var palette = arrayOf (
        0x000000, 0xFFFFFF

        // Default RGB palette:
        /*0x080000,0x201A0B,0x432817,0x492910,  
        0x234309,0x5D4F1E,0x9C6B20,0xA9220F,
        0x2B347C,0x2B7409,0xD0CA40,0xE8A077,
        0x6A94AB,0xD5C4B3,0xFCE76E,0xFCFAE2*/

        // Experimental greyscale palettE:
        /*0x000000, 0x111111, 0x222222, 0x333333,
        0x444444, 0x555555, 0x666666, 0x777777,
        0x888888, 0x999999, 0xaaaaaa, 0xbbbbbb,
        0xcccccc, 0xdddddd, 0xeeeeee, 0xffffff*/
    );

    private fun getPaletteColour(target: Color) : Color {
        var close = Color.rgb(
            (palette[0] shr 16) and 0xFF, 
            (palette[0] shr 8) and 0xFF, 
             palette[0] and 0xFF);

        for (i in palette) {
            val c = Color.rgb(
                (i shr 16) and 0xFF, 
                (i shr 8) and 0xFF, 
                i and 0xFF);
            if (colourDiff(c, target) < colourDiff(close, target)) {
                close = c;
            }
        }
        return close;
    }

    private fun colourDiff(a: Color, b: Color) : Int {
        val diffR = ((a.getRed() * 255.0) - (b.getRed() * 255.0)).toInt();
        val diffG = ((a.getGreen() * 255.0) - (b.getGreen() * 255.0)).toInt();
        val diffB = ((a.getBlue() * 255.0) - (b.getBlue() * 255.0)).toInt();
        return diffR * diffR + diffG * diffG + diffB * diffB;
    }
    /*
    private fun clamp(v: Int, min: Int, max: Int) : Int {
        if (v < min) { return min; }
        if (v > max) { return max; }
        return v;
    }
    */
    private fun clamp(v: Float, min: Float, max: Float) : Float {
        if (v < min) { return min; }
        if (v > max) { return max; }
        return v;
    }
    
    private fun calcDitherThreshold (p: Int) : Float {
        val q = p xor (p shr 3);
        return (((p and 4) shr 2) or ((q and 4) shr 1)
             or ((p and 2) shl 1) or ((q and 2) shl 2)
             or ((p and 1) shl 4) or ((q and 1) shl 5)).toFloat() / 64.0f;
    }
}