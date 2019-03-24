
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javafx.scene.paint.Color;

/*
    This is an attempted implementation of
    an 'arbitrary-palette positional dithering algorithm',

        Algorithm developed by:
    the legendary Joel Yliluoma (a.k.a. Bisqwit)
    https://bisqwit.iki.fi/story/howto/dither/jy/

        Implementation by:
            Michael
*/

fun main(args: Array<String>) {
    Main().run();
}

// To be implemented for ease of access.
class Colour255 {

}

class MixingPlan {
    public var colourA: Int = 0;
    public var colourB: Int = 0;
    public var ratio: Float = 0.0f;
}

class Main {

    private val palette = ArrayList<Color>();
    private val map = arrayOf (
        0, 48, 12, 60, 3, 51, 15, 63,
        32, 16, 44, 28, 35, 19, 47, 31,
        8, 56, 4, 52, 11, 59, 7, 55,
        40, 24, 36, 20, 43, 27, 39, 23,
        2, 50, 14, 62, 1, 49, 13, 61,
        34, 18, 46, 30, 33, 17, 45, 29,
        10, 58, 6, 54, 9, 57, 5, 53,
        42, 26, 38, 22, 41, 25, 37, 21
    );

    public fun run() {
        println("Hello, world");
        var pal = arrayOf(
            0x080000, 0x201A0B, 0x432817, 0x492910,
            0x234309, 0x5D4F1E, 0x9C6B20, 0xA9220F,
            0x2B347C, 0x2B7409, 0xD0CA40, 0xE8A077,
            0x6A94AB, 0xD5C4B3, 0xFCE76E, 0xFCFAE2
        );
        for (p: Int in pal) {
            val r = p and 0xFF;
            val g = (p shr 8) and 0xFF;
            val b = (p shr 16) and 0xFF;
            palette.add(Color.rgb(r, g, b));
        }

        var img : BufferedImage = ImageIO.read(File("res/lenna.png"));
        val w = img.width.toInt();
        val h = img.height.toInt();
        compute(SwingFXUtils.toFXImage(img, null), w, h);
    }

    private fun compute(img: WritableImage, w: Int, h: Int) {
        val pReader = img.pixelReader;
        val pWriter = img.pixelWriter;

        val d = Array2D<Color>(w, h);
        for (y in 0 until h) {
            for (x in 0 until w) {
                d.set(x, y, pReader.getColor(x, y));
            }
        } 

        val dither8x8 = ArrayList<Float>();
        for (d0 in 0 until 64) {
            dither8x8.add(getMatrixVal(d0));
        }

        // Iterate through pixels, set them to colour / 2
       // val thresholds: Color = Color.rgb(256 / 4, 256 / 4, 256 / 4);
        for (y in 0 until h) {
            for (x in 0 until w) {
                /*
                val mapValue = dither8x8.get((x and 7) + ((y and 7) shl 3));
                val colour = d.get(x, y)!!;
                val r = clamp(((colour.getRed() * 255 + mapValue * thresholds.getRed() * 255)).toInt(), 0, 255);
                val g = clamp(((colour.getGreen() * 255 + mapValue * thresholds.getGreen() * 255)).toInt(), 0, 255);
                val b = clamp(((colour.getBlue() * 255 + mapValue * thresholds.getBlue() * 255)).toInt(), 0, 255);
                pWriter.setColor(x, y, Color.rgb(r, g, b));
                */

                val mapValue = map[(x and 7) + ((y and 7) shl 3)];
                val colour = d.get(x, y)!!;
                val plan: MixingPlan = deviseBestMixingPlan(colour);
                val c: Int;
                if (mapValue < plan.ratio) {
                    c = plan.colourB;
                }
                else {
                    c = plan.colourA;
                }
                val r: Int = c and 0xFF;
                val g: Int = (c shr 8) and 0xFF;
                val b: Int = (c shr 16) and 0xFF;
                val pixel = Color.rgb(r, g, b);
                pWriter.setColor(x, y, pixel);
            }
        }

        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File("res/result.png"));
    }

    private fun deviseBestMixingPlan(col: Color) : MixingPlan {
        var result: MixingPlan = MixingPlan();
        var leastPenalty: Float = (1e+99).toFloat();
        val r: Int = (col.getRed() * 255).toInt();
        val g: Int = (col.getGreen() * 255).toInt();
        val b: Int = (col.getBlue() * 255).toInt();

        for (index1 in 0 until 16) {
            for (index2 in index1 until 16) {
                for (ratio in 0 until 64) {
                    if (index1 == index2 && ratio != 0) {
                        break;
                    }
                    // Determine the two component colours
                    val c1 = palette.get(index1);
                    val c2 = palette.get(index2);
                    
                    val r1 = red(c1); val g1 = grn(c1); val b1 = blu(c1);
                    val r2 = red(c2); val g2 = grn(c2); val b2 = blu(c2);

                    // Determine what mixing them in this will produce...
                    val r0 = r1 + ratio * ((r2 - r1) / 64).toInt();
                    val g0 = g1 + ratio * ((g2 - g1) / 64).toInt();
                    val b0 = b1 + ratio * ((b2 - b1) / 64).toInt();

                    val penalty: Float = evaluateMixingErr(r, g, b, r0, g0, b0);
                    if (penalty < leastPenalty) {
                        // Keep result which has smallest error
                        leastPenalty = penalty;
                        result.colourA = index1;
                        result.colourB = index2;
                        result.ratio = (ratio / 64.0).toFloat();
                    }
                }
            }
        }
        return result;
    }

    private fun red(c: Color) : Int { return (c.getRed() * 255).toInt(); }
    private fun grn(c: Color) : Int { return (c.getGreen() * 255).toInt(); }
    private fun blu(c: Color) : Int { return (c.getBlue() * 255).toInt(); }

    private fun colourCompare(a: Color, b: Color) : Float {
        val diffR: Float = (a.getRed() - b.getRed()).toFloat();
        val diffG: Float = (a.getGreen() - b.getGreen()).toFloat();
        val diffB: Float = (a.getBlue() - b.getBlue()).toFloat();
        return ( 
            diffR * diffR + 
            diffG * diffG + 
            diffB * diffB).toFloat();
    }

    private fun evaluateMixingErr(r: Int, g: Int, b: Int, r0: Int, g0: Int, b0: Int) : Float {
        return colourCompare(Color.rgb(r, g, b), Color.rgb(r0, g0, b0));
    }

    private fun getMatrixVal(p: Int) : Float {
        val q: Int = p xor (p shr 3);
        return ((((p and 4) shr 2) or ((q and 4) shr 1)
            or ((p and 2) shl 1) or ((q and 2) shl 2)
            or ((p and 1) shl 4) or ((q and 1) shl 5)).toFloat() / 64.0).toFloat();
    }

    // TODO: Move these functions into Colour255 class.
    private fun clampColour(x: Color) : Color {
        return Color.rgb(
            clamp(x.getRed().toInt(), 0, 255), 
            clamp(x.getGreen().toInt(), 0, 255), 
            clamp(x.getBlue().toInt(), 0, 255)
        );
    }

    private fun clamp (x: Int, min: Int, max: Int) : Int {
        if (x < min) { return min; }
        if (x > max) { return max; }
        return x;
    }

    private fun colourDiff(a: Color, b: Color) : Float {
        val x: Float = (b.getRed() - a.getRed()).toFloat();
        val y: Float = (b.getGreen() - a.getGreen()).toFloat();
        val z: Float = (b.getBlue() - a.getBlue()).toFloat();
        return x * x + y * y + z * z;
    }

    private fun colourSub(a: Color, b: Color) : Color {
        return Color.rgb(
            clamp(((a.getRed() - b.getRed()) * 255).toInt(), 0, 255),
            clamp(((a.getGreen() - b.getGreen()) * 255).toInt(), 0, 255),
            clamp(((a.getBlue() - b.getBlue()) * 255).toInt(), 0, 255)
        );
    }

    private fun colAdd(a: Color, b: Color) : Color {
        return Color.rgb(
            clamp(((a.getRed() + b.getRed()) * 255).toInt(), 0, 255),
            clamp(((a.getGreen() + b.getGreen()) * 255).toInt(), 0, 255),
            clamp(((a.getBlue() + b.getBlue()) * 255).toInt(), 0, 255)
        );
    }

    private fun colMul(a: Color, x: Float) : Color {
        return Color.rgb(
            clamp((a.getRed() * x * 255).toInt(), 0, 255),
            clamp((a.getGreen() * x * 255).toInt(), 0, 255),
            clamp((a.getBlue() * x * 255).toInt(), 0, 255)
        );
    }
    

    private fun colDiv(a: Color, x: Int) : Color {
        return Color.rgb(
            ((a.getRed() * 255) / x).toInt(),
            ((a.getGreen() * 255) / x).toInt(),
            ((a.getBlue() * 255) / x).toInt()
        );
    }

    private fun findClosestPaletteColour(c: Color) : Color {
        var closest: Color = palette[0];
        for (n in palette) {
            if (colourDiff(n, c) < colourDiff(closest, c)) {
                closest = n;
            }
        }
        return closest;
    }
}

// 'Borrowed' from https://stackoverflow.com/questions/28548647/create-generic-2d-array-in-kotlin
class Array2D<T> (val xSize: Int, val ySize: Int, val array: Array<Array<T>>) {

    companion object {

        inline operator fun <reified T> invoke() = Array2D(0, 0, Array(0, { emptyArray<T>() }))

        inline operator fun <reified T> invoke(xWidth: Int, yWidth: Int) =
            Array2D(xWidth, yWidth, Array(xWidth, { arrayOfNulls<T>(yWidth) }))

        inline operator fun <reified T> invoke(xWidth: Int, yWidth: Int, operator: (Int, Int) -> (T)): Array2D<T> {
            val array = Array(xWidth, {
                val x = it
                Array(yWidth, {operator(x, it)})})
            return Array2D(xWidth, yWidth, array)
        }
    }

    operator fun get(x: Int, y: Int): T {
        return array[x][y]
    }

    operator fun set(x: Int, y: Int, t: T) {
        array[x][y] = t
    }

    inline fun forEach(operation: (T) -> Unit) {
        array.forEach { it.forEach { operation.invoke(it) } }
    }

    inline fun forEachIndexed(operation: (x: Int, y: Int, T) -> Unit) {
        array.forEachIndexed { x, p -> p.forEachIndexed { y, t -> operation.invoke(x, y, t) } }
    }
}