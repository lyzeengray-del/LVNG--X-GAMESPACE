package com.lvngx.gamespace

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class MacroStep(
    val id: Long = System.currentTimeMillis(),
    val label: String = "Tap",
    val x: Float,
    val y: Float,
    val delayMs: Long = 150L,
    val holdMs: Long = 45L
)

data class GameItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

enum class PerfMode(val title: String, val desc: String) {
    ECO("Eco", "Hemat baterai & suhu dingin"),
    BALANCED("Balanced", "Seimbang untuk harian"),
    ULTRA("Ultra Boost", "Bersihkan RAM & kunci prioritas")
}

object GameSpaceStore {
    private const val PREF_NAME = "lvngx_store"
    private const val KEY_GAMES = "saved_games"
    private const val KEY_MACRO = "macro_steps"
    private const val KEY_MACRO_LOOP = "macro_loop"
    private const val KEY_MACRO_REPEAT = "macro_repeat"
    private const val KEY_PERF_MODE = "perf_mode"
    private const val KEY_CROSSHAIR = "crosshair_enabled"
    private const val KEY_CROSSHAIR_SIZE = "crosshair_size"
    private const val KEY_AUDIO_BOOST = "audio_boost"

    private val gson = Gson()

    fun getSavedGamePackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GAMES, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    }

    fun saveGamePackages(context: Context, packages: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GAMES, gson.toJson(packages.distinct())).apply()
    }

    fun addGamePackage(context: Context, pkg: String) {
        val current = getSavedGamePackages(context).toMutableList()
        if (!current.contains(pkg)) {
            current.add(pkg)
            saveGamePackages(context, current)
        }
    }

    fun removeGamePackage(context: Context, pkg: String) {
        val current = getSavedGamePackages(context).toMutableList()
        current.remove(pkg)
        saveGamePackages(context, current)
    }

    fun getMacroSteps(context: Context): List<MacroStep> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MACRO, null)
        if (raw.isNullOrBlank()) {
            return listOf(
                MacroStep(1L, "Skill 1", 820f, 540f, 120L, 40L),
                MacroStep(2L, "Attack", 940f, 620f, 100L, 40L)
            )
        }
        return runCatching {
            gson.fromJson<List<MacroStep>>(raw, object : TypeToken<List<MacroStep>>() {}.type)
        }.getOrDefault(emptyList())
    }

    fun saveMacroSteps(context: Context, steps: List<MacroStep>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_MACRO, gson.toJson(steps)).apply()
    }

    fun getMacroRepeat(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_MACRO_REPEAT, 1)

    fun setMacroRepeat(context: Context, count: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MACRO_REPEAT, count.coerceIn(1, 999)).apply()
    }

    fun isMacroLoop(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_MACRO_LOOP, false)

    fun setMacroLoop(context: Context, loop: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MACRO_LOOP, loop).apply()
    }

    fun getPerfMode(context: Context): PerfMode {
        val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PERF_MODE, PerfMode.ULTRA.name) ?: PerfMode.ULTRA.name
        return runCatching { PerfMode.valueOf(name) }.getOrDefault(PerfMode.ULTRA)
    }

    fun setPerfMode(context: Context, mode: PerfMode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PERF_MODE, mode.name).apply()
    }

    fun isCrosshairEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_CROSSHAIR, false)

    fun setCrosshairEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CROSSHAIR, enabled).apply()
    }

    fun isAudioBoost(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AUDIO_BOOST, false)

    fun setAudioBoost(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUDIO_BOOST, enabled).apply()
    }

    fun loadInstalledLaunchableApps(context: Context): List<GameItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString() ?: pkg
                val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                GameItem(pkg, label, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun resolveSavedGames(context: Context): List<GameItem> {
        val pm = context.packageManager
        return getSavedGamePackages(context).mapNotNull { pkg ->
            runCatching {
                val appInfo: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
                GameItem(
                    packageName = pkg,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }.getOrNull()
        }
    }
}
