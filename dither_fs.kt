
/*
    Floyd-Steinberg dithering implementation attempt.
 */

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

class Utils {

    // TODO: RESEARCH HOW THIS WORKS
    companion object {
        public fun clamp(x: Int, min: Int, max: Int): Int {
            if (x < min) { return min; }
            if (x > max) { return max; }
            return x;
        } 
    }
}

class Colour(val x: Color) {

    public var r: Int = 0;
    public var g: Int = 0;
    public var b: Int = 0;

    init {
        r = Math.round(x.getRed() * 255.0).toInt();
        g = Math.round(x.getGreen() * 255.0).toInt();
        b = Math.round(x.getBlue() * 255.0).toInt();
    }

    companion object {
        public fun subtract(ac: Colour, bc: Colour): Colour {
            return Colour(Color.rgb(
                Utils.clamp(ac.r - bc.r, 0, 255),
                Utils.clamp(ac.g - bc.g, 0, 255),
                Utils.clamp(ac.b - bc.b, 0, 255)
            ));
        } 
    }

    public fun build(): Color {
        return Color.rgb(r, g, b);
    }

    public fun add(a: Colour) : Colour {
        r += a.r; g += a.g; b += a.b;
        clamp();
        return this;
    }

    public fun sub(a: Colour) : Colour {
        r -= a.r; g -= a.g; b -= a.b;
        clamp();
        return this;
    }

    public fun mul(x: Int) : Colour {
        r *= x; g *= x; b *= x;
        clamp();
        return this;
    }

    public fun div(x: Int): Colour {
        r /= x; g /= x; b /= x;
        clamp();
        return this;
    }

    public fun grey() : Colour {
        val common: Int = (r + g + b) / 3;
        r = common; g = common; b = common;
        clamp();
        return this;
    }

    public fun clamp() {
        r = Utils.clamp(r, 0, 255);
        g = Utils.clamp(g, 0, 255);
        b = Utils.clamp(b, 0, 255);
    }
}

class MainClass {

    val pathInput: String = "res/lenna.png";
    val pathOutput: String = "res/result.png";

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
        
        val d = Array(w, { arrayOfNulls<Colour>(h) });
        for (y in 0 until h) {
            for (x in 0 until w) {
                d[x][y] = Colour(pReader.getColor(x, y));
            }
        }

        for (y in 1 until (h - 1)) {
            for (x in 1 until (w - 1)) {
                val oldpixel: Colour = Colour(pReader.getColor(x, y)).grey(); //d[x][y]!!;//.grey();
                val newpixel: Colour = findClosestPaletteColour(oldpixel);
                val quantErr: Colour = Colour.subtract(oldpixel, newpixel);
                pWriter.setColor(x, y, oldpixel.build());

                val p0 = pReader.getColor(x + 1, y);
                val p0c = Color.rgb(
                    Utils.clamp(((p0.getRed().toFloat() * 255.0f) + (quantErr.r.toFloat() * 255.0f * (7.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p0.getGreen().toFloat() * 255.0f) + (quantErr.g.toFloat() * 255.0f * (7.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p0.getBlue().toFloat() * 255.0f) + (quantErr.b.toFloat() * 255.0f * (7.0f / 16.0f))).toInt(), 0, 255)
                );
                pWriter.setColor(x + 1, y, p0c)

                val p1 = pReader.getColor(x - 1, y + 1);
                val p1c = Color.rgb(
                    Utils.clamp(((p1.getRed().toFloat() * 255.0f) + (quantErr.r.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p1.getGreen().toFloat() * 255.0f) + (quantErr.g.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p1.getBlue().toFloat() * 255.0f) + (quantErr.b.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255)
                );
                pWriter.setColor(x - 1, y + 1, p1c)

                val p2 = pReader.getColor(x, y + 1);
                val p2c = Color.rgb(
                    Utils.clamp(((p2.getRed().toFloat() * 255.0f) + (quantErr.r.toFloat() * 255.0f * (5.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p2.getGreen().toFloat() * 255.0f) + (quantErr.g.toFloat() * 255.0f * (5.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p2.getBlue().toFloat() * 255.0f) + (quantErr.b.toFloat() * 255.0f * (5.0f / 16.0f))).toInt(), 0, 255)
                );
                pWriter.setColor(x, y + 1, p2c)

                val p3 = pReader.getColor(x + 1, y + 1);
                val p3c = Color.rgb(
                    Utils.clamp(((p3.getRed().toFloat() * 255.0f) + (quantErr.r.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p3.getGreen().toFloat() * 255.0f) + (quantErr.g.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255),
                    Utils.clamp(((p3.getBlue().toFloat() * 255.0f) + (quantErr.b.toFloat() * 255.0f * (3.0f / 16.0f))).toInt(), 0, 255)
                );
                pWriter.setColor(x + 1, y + 1, p3c)
            }
        }
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File(pathOutput));
    }

    // Simple rounding palette.
    private fun findClosestPaletteColour(x: Colour): Colour {
        val factor: Float = (255.0f / 1.0f);
        val r = Utils.clamp((Math.round(x.r.toFloat() / factor).toInt()) * factor.toInt(), 0, 255);
        val g = Utils.clamp((Math.round(x.g.toFloat() / factor).toInt()) * factor.toInt(), 0, 255);
        val b = Utils.clamp((Math.round(x.b.toFloat() / factor).toInt()) * factor.toInt(), 0, 255);
        return Colour(Color.rgb(r, g, b));
    }
}