package calculator.hide.vaultpro.data.launcher

import android.graphics.drawable.Drawable
import android.util.LruCache

class LauncherIconLoader(private val appRepository: AppRepository) {

    private val cache = LruCache<String, Drawable>(ICON_CACHE_SIZE)

    fun loadIcon(app: LaunchableApp): Drawable? {
        cache.get(app.key)?.let { return it }
        val loaded = appRepository.getIcon(app) ?: return null
        cache.put(app.key, loaded)
        return loaded
    }

    fun clear() {
        cache.evictAll()
    }

    companion object {
        private const val ICON_CACHE_SIZE = 80
    }
}
