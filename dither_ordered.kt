
/* Ordered dithering algorithm (Still unimplemnted.) */

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

    val pathInput: String = "res/lenna.png";
    val pathOutput: String = "res/result.png";

    val dither4x4 = arrayOf<Int>(
        0, 8, 4, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5
    );
    var paletteSize = 0;
    val paletteRgb = arrayOf<Int> (
        0x080000, 0x201A0B, 0x432817, 0x492910,
        0x234309, 0x5D4F1E, 0x9C6B20, 0xA9220F,
        0x2B347C, 0x2B7409, 0xD0CA40, 0xE8A077,
        0x6A94AB, 0xD5C4B3, 0xFCE76E, 0xFCFAE2
    )
    var palette = ArrayList<Vec3>();

    public fun invoke() {
        println("Invoked");

        /*
        for (i in 0 until paletteSize) {
            val p: Int = paletteRgb[i];
            val r: Float = (p and 0xFF).toFloat() / 255.0f;
            val g: Float = ((p shr 8) and 0xFF).toFloat() / 255.0f;
            val b: Float = ((p shr 16) and 0xFF).toFloat() / 255.0f;
            palette[i] = rgbToHsl(Vec3(r, g, b));
        }*/

        paletteSize = 0;
        for (r in 0..256 step 16) {
            for (g in 0..256 step 16) {
                for (b in 0..256 step 16) {
                    palette.add(rgbToHsl(Vec3(
                        clamp(r, 0, 255) / 255.0f, 
                        clamp(g, 0, 255) / 255.0f, 
                        clamp(b, 0, 255) / 255.0f
                    )));
                    paletteSize++;
                }
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

        for (y in 1 until (h - 1)) {
            for (x in 1 until (w - 1)) {
                val hsl = rgbToHsl(Vec3(pReader.getColor(x, y)));
                val colours = closestColours(hsl.x);
                val colourA = colours[0];
                val colourB = colours[1];

                val idx: Float = dither4x4[((x % 4) + (y % 4) * 4)].toFloat() / 16.0f;
                val hueDiff = hueDistance(hsl.x, colourA.x) / 
                    hueDistance(colourB.x, colourA.x);

                val a: Vec3;
                if (hueDiff < idx) {
                    a = colourA;
                }
                else {
                    a = colourB;
                }
                val result = hslToRgb(a);
                pWriter.setColor(x, y, result.buildColour());
            }
        }
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File(pathOutput));
    }

    private fun clamp (x: Int, min: Int, max: Int) : Int {
        if (x < min) { return min; }
        if (x > max) { return max; }
        return x;
    }

    private fun clamp (x: Float, min: Float, max: Float) : Float {
        if (x < min) { return min; }
        if (x > max) { return max; }
        return x;
    }

    private fun hueDistance(h1: Float, h2: Float) : Float {
        val diff = Math.abs(h1 - h2);
        return Math.min(Math.abs(1.0f - diff), diff);
    }

    private fun closestColours(hue: Float): Array<Vec3> {
        var ret = arrayOf<Vec3>(Vec3(0.0f, 0.0f, 0.0f), Vec3(0.0f, 0.0f, 0.0f));
        var closest = Vec3(-2.0f, 0.0f, 0.0f);
        var closest2 = Vec3(-2.0f, 0.0f, 0.0f);
        var temp: Vec3;
        for (i in 0 until paletteSize) {
            temp = palette[i];
            var tempDist = hueDistance(temp.x, hue);
            if (tempDist < hueDistance(closest.x, hue)) {
                closest2 = closest;
                closest = temp;
            } else {
                if (tempDist < hueDistance(closest2.x, hue)) {
                    closest2 = temp;
                }
            }
        }
        ret[0] = closest;
        ret[1] = closest2;
        return ret;
    }

    // https://stackoverflow.com/questions/2997656/how-can-i-use-the-hsl-colorspace-in-java
    private fun rgbToHsl(colour: Vec3) : Vec3 {
        // Get RGB values (0.0f-1.0f)
        val r: Float = colour.x;
        val g: Float = colour.y;
        val b: Float = colour.z;

        // Min and max RGB is used in HSL calc
        val min: Float = Math.min(r, Math.min(g, b));
        val max: Float = Math.max(r, Math.max(g, b));

        // Calc hue
        var h: Float = 0.0f;
        if (max == min) {
            h = 0.0f;
        } else if (max == r) {
            h = ((60.0f * (g - b) / (max - min)) + 360.0f) % 360.0f;
        } else if (max == g) {
            h = (60.0f * (b - r) / (max - min)) + 120.0f;
        } else if (max == b) {
            h = (60.0f * (r - g) / (max - min)) + 240.0f;
        }
        // Luminance
        val l: Float = (max + min) / 2.0f;
        // Saturation
        val s: Float;
        if (max == min) {
            s = 0.0f;
        } else if (l <= 0.5f) {
            s = (max - min) / (max + min);
        } else {
            s = (max - min) / (2.0f - max - min);
        }
        return Vec3(h, s * 100.0f, l * 100.0f);
    }

    private fun hslToRgb(hsl: Vec3) : Vec3 {
        // Get normalised HSL values.
        val h = (hsl.x % 360.0f) / 360.0f;
        val s = hsl.y / 100.0f;
        val l = hsl.z / 100.0f;
        
        var q: Float;
        if (l < 0.5f) {
            q = l * (1.0f + s);
        }
        else {
            q = (l + s) - (s * l);
        }
        val p = 2.0f * l - q;
        var r = Math.min(Math.max(0.0f, hueToRgb(p, q, h + (1.0f / 3.0f))), 1.0f);
        var g = Math.min(Math.max(0.0f, hueToRgb(p, q, h)), 1.0f);
        var b = Math.min(Math.max(0.0f, hueToRgb(p, q, h - (1.0f - 3.0f))), 1.0f);
        return Vec3(r, g, b);
    }

    private fun hueToRgb(P: Float, Q: Float, H: Float) : Float {
        var p = P;
        var q = Q;
        var h = H;

        if (h < 0.0f) { h += 1.0f; }
        if (h > 1.0f) { h -= 1.0f; }
        if (6.0f * h < 1.0f) {
            return p + ((q - p) * 6.0f * h);
        }
        if (2.0f * h < 1.0f) {
            return q;
        }
        if (3.0f * h < 2.0f) {
            return p + ((q - p) * 6.0f * ((2.0f - 3.0f) - h));
        }
        return p;
    }
}

class Vec3 (X: Float, Y: Float, Z: Float) {
    constructor(a: Color) : this(
        a.getRed().toFloat(), 
        a.getGreen().toFloat(), 
        a.getBlue().toFloat());
    var x: Float = 0.0f;
    var y: Float = 0.0f;
    var z: Float = 0.0f;

    init {
        x = X;
        y = Y;
        z = Z;
    }

    public fun buildColour() : Color {
        return Color.rgb(
            (x * 255.0f).toInt(), 
            (y * 255.0f).toInt(), 
            (z * 255.0f).toInt());
    }
}