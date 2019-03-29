
/* Ordered dithering algorithm */
/* Mostly based off of algorithms from Bisqwit */

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

    val R: Int = 7; val G: Int = 9; val B: Int = 4; // 7*9*4 palette (252 colours)
    val paletteGamma: Double = 1.5;
    val ditherGamma: Double = 2.0 / paletteGamma;
    var colourConvert = arrayOfNulls<Int>(3 * 256 * 256);
    var dither8x8 = arrayOfNulls<Int>(8 * 8);
    val ditherBits = 6; // Dither strength

    public fun invoke() {
        println("Invoked");

        // Initialise bayer 8x8 dither matrix
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                dither8x8[x + y * 8] =
                ((x      ) and 4) / 4 + ((x      ) and 2) * 2 + ((x      ) and 1) * 16
              + ((x xor y) and 4) / 2 + ((x xor y) and 2) * 4 + ((x xor y) and 1) * 32;
            }
        }
        // Gamma-corrected look-up tables for dithering
        var dtab: Array<Float?> = arrayOfNulls<Float>(256);
        var ptab: Array<Float?> = arrayOfNulls<Float>(256);
        for (n in 0 until 256) {
            dtab[n] = ((255.0f/256.0f) - Math.pow(n / 256.0, 1.0 / ditherGamma)).toFloat();
            ptab[n] = Math.pow(n / 255.0, 1.0 / paletteGamma).toFloat();
        }
        for (n in 0 until 256) {
            for (d in 0 until 256) {
                colourConvert[0 + n * 256 + d * 256] =     Math.min(B - 1, (ptab[n]!! * (B - 1) + dtab[d]!!).toInt());
                colourConvert[1 + n * 256 + d * 256] =   B*Math.min(G - 1, (ptab[n]!! * (G - 1) + dtab[d]!!).toInt());
                colourConvert[2 + n * 256 + d * 256] = G*B*Math.min(R - 1, (ptab[n]!! * (R - 1) + dtab[d]!!).toInt());
            }
        }

        val img: BufferedImage = ImageIO.read(File(pathInput));
        val w = img.width.toInt();
        val h = img.height.toInt();
        computeImage(SwingFXUtils.toFXImage(img, null), w, h);
    }

    private fun computeImage(img: WritableImage, w: Int, h: Int) {
        val pReader = img.pixelReader;
        val pWriter = img.pixelWriter;
        
        //val d = Array(w, { arrayOfNulls<Colour>(h) });

        for (y in 0 until h) {
            for (x in 0 until w) {
                var d: Int = dither8x8[(y and 7) + ((x and 7) * 8)]!!;
                d = d and (0x3F - (0x3F shr ditherBits));
                d *= 4; // No temporal dithering.

                val col = pReader.getColor(x, y);
                val intR: Int = (col.getRed() * 255).toInt();
                val intG: Int = (col.getGreen() * 255).toInt();
                val intB: Int = (col.getBlue() * 255).toInt();

                val r0 = Math.pow((((intR/(B*G))%R) * 1.0 / (R-1)).toDouble(), paletteGamma).toInt() * 63;
                val g0 = Math.pow((((intG/    B)%R) * 1.0 / (G-1)).toDouble(), paletteGamma).toInt() * 63;
                val b0 = Math.pow((((intB      )%B) * 1.0 / (B-1)).toDouble(), paletteGamma).toInt() * 63;

                // The 'and 0xFF' may not be required since the values wont ever pass 255 anyway.
                var newR: Int = colourConvert[0 + ((r0 * 256) and 0xFF) + d * 256]!!;
                var newG: Int = colourConvert[1 + ((g0 * 256) and 0xFF) + d * 256]!!;
                var newB: Int = colourConvert[2 + ((b0 * 256) and 0xFF) + d * 256]!!;
                
                val result = Color.rgb(newR, newG, newB);
                
                pWriter.setColor(x, y, result);
            }
        }
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File(pathOutput));
    }
}