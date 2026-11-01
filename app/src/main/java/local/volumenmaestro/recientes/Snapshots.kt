package local.volumenmaestro.recientes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

/**
 * Las vistas previas del panel: una captura chica de cada app, en la carpeta privada de la app
 * (`Android/data/local.volumenmaestro/files/capturas`). No salen de la TV.
 *
 * La captura la saca la shell en formato crudo (`screencap` sin -p tarda ~0,5 s; en PNG, ~2 s) y
 * el motor la achica a JPEG. Las apps con protección (Netflix, Disney+) salen negras: esas no se
 * guardan y el panel muestra el banner de la app.
 */
object Snapshots {
    private const val WIDTH = 384
    private const val HEIGHT = 216
    private const val RAW = "captura.raw"
    private const val HEADER_BYTES = 16L // ancho, alto, formato y espacio de color (4 bytes cada uno)

    fun dir(context: Context): File? = context.getExternalFilesDir("capturas")

    fun file(context: Context, pkg: String): File? = dir(context)?.let { File(it, "$pkg.jpg") }

    /** Comando de shell que deja la pantalla en crudo; después va [store]. */
    fun captureCommand(context: Context): String? = dir(context)?.let { "screencap ${File(it, RAW).absolutePath}" }

    /** Achica la captura cruda y la guarda para [pkg]. Devuelve false si salió negra o no se pudo. */
    fun store(context: Context, pkg: String): Boolean {
        val raw = dir(context)?.let { File(it, RAW) } ?: return false
        try {
            val small = RandomAccessFile(raw, "r").use { f ->
                val w = f.readIntLe()
                val h = f.readIntLe()
                if (w <= 0 || h <= 0 || f.length() < HEADER_BYTES + w.toLong() * h * 4) return false
                val pixels = f.channel.map(FileChannel.MapMode.READ_ONLY, HEADER_BYTES, w.toLong() * h * 4)
                val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                full.copyPixelsFromBuffer(pixels)
                Bitmap.createScaledBitmap(full, WIDTH, HEIGHT, true).also { full.recycle() }
            }
            val out = file(context, pkg) ?: return false
            if (isBlack(small)) {
                out.delete()
                return false
            }
            FileOutputStream(out).use { small.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            small.recycle()
            return true
        } catch (e: Exception) {
            return false
        } finally {
            raw.delete()
        }
    }

    fun load(context: Context, pkg: String): Bitmap? =
        file(context, pkg)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }

    fun delete(context: Context, pkg: String) {
        file(context, pkg)?.delete()
    }

    /** Negra si ningún punto de una grilla de 10×10 tiene algo de brillo. */
    private fun isBlack(b: Bitmap): Boolean {
        for (y in 0 until 10) for (x in 0 until 10) {
            val c = b.getPixel(x * b.width / 10 + b.width / 20, y * b.height / 10 + b.height / 20)
            if ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF) > 60) return false
        }
        return true
    }

    private fun RandomAccessFile.readIntLe(): Int {
        val b = ByteArray(4)
        readFully(b)
        return (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8) or
            ((b[2].toInt() and 0xFF) shl 16) or ((b[3].toInt() and 0xFF) shl 24)
    }
}
