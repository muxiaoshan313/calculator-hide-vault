package calculator.hide.vault.launcher.repository

import android.graphics.drawable.Drawable
import android.util.LruCache
import calculator.hide.vault.data.launcher.AppRepository
import calculator.hide.vault.data.launcher.LaunchableApp

class IconCacheRepository(private val appRepository: AppRepository) {

    private val cache = LruCache<String, Drawable>(CACHE_SIZE)

    fun loadIcon(app: LaunchableApp): Drawable? {
        cache.get(app.key)?.let { return it }
        val loaded = appRepository.getIcon(app) ?: return null
        cache.put(app.key, loaded)
        return loaded
    }

    fun evict(packageName: String, className: String) {
        cache.remove("$packageName/$className")
    }

    fun evictAll() {
        cache.evictAll()
    }

    companion object {
        private const val CACHE_SIZE = 120
    }
}
