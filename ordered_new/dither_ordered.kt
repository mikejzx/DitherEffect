
/* Ordered dithering algorithm */
/* algorithms courtesy of Bisqwit */

package io.mikejzx.github.ditheringtests;

import javafx.scene.image.Image;
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

    var palette = ArrayList<Int>();

    public fun invoke() {
        println("Invoked");

        // Default RGB
        /*
        for (r0 in 0 until 257 step 64) {
            val r = clamp(r0, 0, 255);
            for (g0 in 0 until 257 step 64) {
                val g = clamp(g0, 0, 255);
                for (b0 in 0 until 257 step 64) {
                    val b = clamp(b0, 0, 255);
                    val c: Int = ((r shl 16) + (g shl 8) + b) and 0xFFFFFF;
                    palette.add(c);
                }
            }
        }*/
        // Greyscale
        for (idx in 0 until 257 step 64) {
            val i = clamp(idx, 0, 255);
            val c: Int = ((i shl 16) + (i shl 8) + i) and 0xFFFFFF;
            palette.add(c);
        }

        //for (i in palette) { println("Palette: R:" + ((i shr 16) and 0xFF) + " G:" + ((i shr 8) and 0xFF) + " B:" + (i and 0xFF));}

        val img: BufferedImage = ImageIO.read(File(pathInput));
        val w = img.width.toInt();
        val h = img.height.toInt();
        computeImage(SwingFXUtils.toFXImage(img, null), w, h);
    }

    private fun computeImage(img: WritableImage, w: Int, h: Int) {
        val pReader = img.pixelReader;
        val pWriter = img.pixelWriter;

        val threshold = arrayOf(64, 64, 64); // 256/4
        for (y in 0 until h) {
            for (x in 0 until w) {
                val factor = calcDitherThreshold((x and 7) + ((y and 7) shl 3));
                val c = pReader.getColor(x, y);
                val r = clamp(((c.getRed() * 255.0).toInt() + factor * threshold[0]).toInt(), 0, 255);
                val g = clamp(((c.getGreen() * 255.0).toInt() + factor * threshold[1]).toInt(), 0, 255);
                val b = clamp(((c.getBlue() * 255.0).toInt() + factor * threshold[2]).toInt(), 0, 255);
                
                val result = getPaletteColour(r, g, b);
                pWriter.setColor(x, y, result);
            }
        }
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File(pathOutput));
    }

    var pal = arrayOf (
        /* // Default RGB palette:
        0x080000,0x201A0B,0x432817,0x492910,  
        0x234309,0x5D4F1E,0x9C6B20,0xA9220F,
        0x2B347C,0x2B7409,0xD0CA40,0xE8A077,
        0x6A94AB,0xD5C4B3,0xFCE76E,0xFCFAE2
        */

        // Experimental greyscale palettE:
        0x000000, 0x111111, 0x222222, 0x333333,
        0x444444, 0x555555, 0x666666, 0x777777,
        0x888888, 0x999999, 0xaaaaaa, 0xbbbbbb,
        0xcccccc, 0xdddddd, 0xeeeeee, 0xffffff
    );

    private fun getPaletteColour(r0: Int, g0: Int, b0: Int) : Color {
        var close = Color.rgb(
            (palette[0] shr 16) and 0xFF, 
            (palette[0] shr 8) and 0xFF, 
             palette[0] and 0xFF);

        var target = Color.rgb(r0, g0, b0);
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

    private fun clamp(v: Int, min: Int, max: Int) : Int {
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