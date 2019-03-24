
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javafx.scene.paint.Color;

fun main(args: Array<String>) {
    Main().run();
}

class Main {
    /*private val palette: Array<Color> = emptyArray<T>(); = arrayOf(
        Color.rgb(  0,   0,   0), // Black
        Color.rgb(  0,   0, 255), // Green
        Color.rgb(  0, 255,   0), // Blue
        Color.rgb(  0, 255, 255), // Cyan
        Color.rgb(255,   0,   0), // Red
        Color.rgb(255,   0, 255), // Purple
        Color.rgb(255, 255,   0), // Yellow
        Color.rgb(255, 255, 255) // White
    );*/

    private val palette = ArrayList<Color>();

    public fun run() {
        println("Hello, world");

        //if (args.size < 2) { 
        //    println("Usage: <image_original_new> <image_new_path>");
        //    return; 
        //}

        /*
        for (r in 0..256 step 16) {
            for (g in 0..256 step 16) {
                for (b in 0..256 step 16) {
                    palette.add(Color.rgb(
                        clamp(r, 0, 255), 
                        clamp(g, 0, 255), 
                        clamp(b, 0, 255)
                    ));
                }
            }
        }
        */

        var img : BufferedImage = ImageIO.read(File("res/bands.png")); // args[0]
        val w = img.width.toInt();
        val h = img.height.toInt();
        compute(SwingFXUtils.toFXImage(img, null), w, h);
    }

    private fun compute(img: WritableImage, w: Int, h: Int) {
        val pReader = img.pixelReader;
        val pWriter = img.pixelWriter;

        val d = Array2D<Color>(w, h);
        for (x in 0 until w) {
            for (y in 0 until h) {
                d.set(x, y,  pReader.getColor(x, y));
            }
        }

        // Iterate through pixels, set them to colour / 2
        for (y in 0 until h) {
            for (x in 0 until w) {
                val oldPixel: Color = d.get(x, y)!!;
                val newPixel = findClosestPaletteColour(oldPixel);
                pWriter.setColor(x, y, newPixel);
                val quantErr = colourSub(oldPixel, newPixel);
                if (x + 1 < w) { 
                    val v = colAdd(pReader.getColor(x + 1, y), colMul(quantErr, 7.0f / 16.0f));
                    pWriter.setColor(x + 1, y, clampColour(v)); 
                }
                if (x - 1 > -1 && y + 1 < h) {
                    val v = colAdd(pReader.getColor(x - 1, y + 1), colMul(quantErr, 3.0f / 16.0f));
                    pWriter.setColor(x - 1, y + 1, clampColour(v));
                }
                if (y + 1 < h) {
                    val v = colAdd(pReader.getColor(x, y + 1), colMul(quantErr, 5.0f / 16.0f));
                    pWriter.setColor(x, y + 1, clampColour(v));
                }
                if (x + 1 < w && y + 1 < h) {
                    val v = colAdd(pReader.getColor(x + 1, y + 1), colMul(quantErr, 1.0f / 16.0f));
                    pWriter.setColor(x + 1, y + 1, clampColour(v));
                }


                //val c = pReader.getColor(x, y);
                //pWriter.setColor(x, y, c.desaturate());
            }
        }

        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", File("res/result.png"));
    }

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
            clamp((a.getRed() * x * 255.0).toInt(), 0, 255),
            clamp((a.getGreen() * x * 255.0).toInt(), 0, 255),
            clamp((a.getBlue() * x * 255.0).toInt(), 0, 255)
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