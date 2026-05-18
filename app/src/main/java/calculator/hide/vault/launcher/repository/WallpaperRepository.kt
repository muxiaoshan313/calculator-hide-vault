package calculator.hide.vault.launcher.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

class WallpaperRepository(private val context: Context) {

    fun loadHomeWallpaper(): Drawable? {
        return try {
            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.drawable
            } else {
                @Suppress("DEPRECATION")
                wm.drawable
            }
        } catch (_: Exception) {
            null
        }
    }

    fun loadBlurredWallpaper(blurRadius: Float = 18f): Bitmap? {
        val drawable = loadHomeWallpaper() ?: return null
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> drawable.toBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            )
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)?.let { copy ->
                    android.graphics.RenderEffect.createBlurEffect(
                        blurRadius, blurRadius,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    copy
                }
            } catch (_: Exception) {
                bitmap
            }
        } else {
            bitmap
        }
    }
}
