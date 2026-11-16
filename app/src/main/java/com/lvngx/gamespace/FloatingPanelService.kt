package com.lvngx.gamespace

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingPanelService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var crosshairView: View? = null
    private var fpsBadgeView: TextView? = null
    private var targetDotView: View? = null

    private var isPanelOpen = false
    private var currentFps = 60
    private var fpsTracker: RealTimeFpsTracker? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    // Colors matching LVNG-X dark navy + crimson red aesthetic
    private val colNavyDark = Color.parseColor("#EE0B172E")
    private val colNavyCard = Color.parseColor("#FF112244")
    private val colCrimson = Color.parseColor("#FFE91E5A")
    private val colCrimsonSoft = Color.parseColor("#33E91E5A")
    private val colCyan = Color.parseColor("#FF38BDF8")

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        createFloatingBubble()

        fpsTracker = RealTimeFpsTracker { fps ->
            currentFps = fps
            fpsBadgeView?.text = "FPS $fps"
        }.also { it.start() }

        if (GameSpaceStore.isCrosshairEnabled(this)) {
            showCrosshairOverlay(true)
        }
    }

    private fun startForegroundNotification() {
        val channelId = "lvngx_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "LVNG -X In-Game Floating Panel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("LVNG -X Game Space")
                .setContentText("Jendela mengambang aktif • Ketuk ikon L di layar")
                .setSmallIcon(R.drawable.ic_lvngx_logo)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("LVNG -X Game Space")
                .setContentText("Jendela mengambang aktif")
                .setSmallIcon(R.drawable.ic_lvngx_logo)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notif)
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun roundedBg(fillColor: Int, strokeColor: Int = Color.TRANSPARENT, radiusDp: Float = 14f, strokeWidthPx: Int = 2): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = radiusDp * density
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        val container = FrameLayout(this).apply {
            background = roundedBg(colNavyDark, colCrimson, 16f, 3)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_lvngx_logo)
            layoutParams = FrameLayout.LayoutParams(dp(38), dp(38))
        }
        container.addView(logo)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(180)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    wm.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMainPanel()
                    true
                }
                else -> false
            }
        }

        bubbleView = container
        wm.addView(container, params)
    }

    private fun toggleMainPanel() {
        if (isPanelOpen) {
            closeMainPanel()
        } else {
            openMainPanel()
        }
    }

    private fun closeMainPanel() {
        panelView?.let { runCatching { wm.removeView(it) } }
        panelView = null
        isPanelOpen = false
    }

    @SuppressLint("SetTextI18n")
    private fun openMainPanel() {
        closeMainPanel()
        isPanelOpen = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(colNavyDark, colCrimson, 18f, 3)
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }

        // Top Bar: Logo + Title + FPS + Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val miniLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_lvngx_logo)
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
        }
        val title = TextView(this).apply {
            text = "  LVNG -X"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val fpsText = TextView(this).apply {
            text = "FPS $currentFps"
            setTextColor(colCyan)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(colNavyCard, colCyan, 8f, 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        fpsBadgeView = fpsText

        val closeBtn = TextView(this).apply {
            text = "  ✕ "
            setTextColor(colCrimson)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { closeMainPanel() }
        }

        header.addView(miniLogo)
        header.addView(title)
        header.addView(fpsText)
        header.addView(closeBtn)
        root.addView(header)

        // RAM & Performance Status Bar
        val (usedRam, totalRam) = PerformanceBooster.getRamStats(this)
        val statusInfo = TextView(this).apply {
            text = "RAM: ${usedRam}MB / ${totalRam}MB • Mode: ${GameSpaceStore.getPerfMode(this@FloatingPanelService).title}"
            setTextColor(Color.parseColor("#FF94A3B8"))
            textSize = 11f
            setPadding(0, dp(6), 0, dp(10))
        }
        root.addView(statusInfo)

        // Row 1: Quick Action Tiles (Performance Boost | Audio FX | Crosshair)
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnBoost = makeActionTile("⚡ BOOST", "Free RAM", true) {
            val res = PerformanceBooster.boostMemory(this)
            val (u, t) = PerformanceBooster.getRamStats(this)
            statusInfo.text = "Boosted +${res.freedMb}MB! RAM: ${u}MB / ${t}MB"
            Toast.makeText(this, "LVNG -X Boost: +${res.freedMb} MB dibersihkan!", Toast.LENGTH_SHORT).show()
        }

        var audioOn = GameSpaceStore.isAudioBoost(this)
        val btnAudio = makeActionTile("🔊 AUDIO", if (audioOn) "FX ON" else "FX OFF", audioOn) {
            audioOn = !audioOn
            GameSpaceStore.setAudioBoost(this, audioOn)
            PerformanceBooster.toggleAudioFx(this, audioOn)
            Toast.makeText(this, if (audioOn) "Audio FX & Footstep Boost ON" else "Audio FX OFF", Toast.LENGTH_SHORT).show()
            openMainPanel()
        }

        var crosshairOn = GameSpaceStore.isCrosshairEnabled(this)
        val btnCrosshair = makeActionTile("⊕ AIM", if (crosshairOn) "ON" else "OFF", crosshairOn) {
            crosshairOn = !crosshairOn
            GameSpaceStore.setCrosshairEnabled(this, crosshairOn)
            showCrosshairOverlay(crosshairOn)
            openMainPanel()
        }

        row1.addView(btnBoost)
        row1.addView(spaceView(dp(8)))
        row1.addView(btnAudio)
        row1.addView(spaceView(dp(8)))
        row1.addView(btnCrosshair)
        root.addView(row1)

        root.addView(spaceView(dp(10)))

        // Macro Sub-Card (Direct Start/Stop Macro In-Game!)
        val macroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(colNavyCard, Color.parseColor("#FF1E3A6E"), 12f, 2)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        val steps = GameSpaceStore.getMacroSteps(this)
        val macroHeader = TextView(this).apply {
            text = "MACRO ENGINE (${steps.size} Step • ${if (MacroAccessibilityService.isEnabled()) "SIAP" else "IZIN MATI"})"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        macroCard.addView(macroHeader)
        macroCard.addView(spaceView(dp(6)))

        val macroRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnPlayMacro = TextView(this).apply {
            text = if (MacroRunner.isRunning) "■ STOP MACRO" else "▶ JALANKAN MACRO"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBg(if (MacroRunner.isRunning) Color.parseColor("#FFDC2626") else colCrimson, Color.TRANSPARENT, 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f)
            setOnClickListener {
                if (MacroRunner.isRunning) {
                    MacroRunner.stop()
                    openMainPanel()
                } else {
                    if (!MacroAccessibilityService.isEnabled()) {
                        Toast.makeText(this@FloatingPanelService, "Aktifkan LVNG -X di menu Aksesibilitas dulu!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        val loadedSteps = GameSpaceStore.getMacroSteps(this@FloatingPanelService)
                        val rep = GameSpaceStore.getMacroRepeat(this@FloatingPanelService)
                        val loop = GameSpaceStore.isMacroLoop(this@FloatingPanelService)
                        // Hide panel briefly so macro taps hit the game underneath cleanly
                        closeMainPanel()
                        uiHandler.postDelayed({
                            MacroRunner.play(this@FloatingPanelService, loadedSteps, rep, loop)
                        }, 180L)
                    }
                }
            }
        }

        val btnTargetPicker = TextView(this).apply {
            text = "🎯 SET TITIK"
            setTextColor(colCyan)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#FF0B172E"), colCyan, 10f, 1)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                closeMainPanel()
                showCoordinatePickerOverlay()
            }
        }

        macroRow.addView(btnPlayMacro)
        macroRow.addView(spaceView(dp(8)))
        macroRow.addView(btnTargetPicker)
        macroCard.addView(macroRow)
        root.addView(macroCard)

        root.addView(spaceView(dp(10)))

        // Bottom Row: Open Full App | Exit Overlay
        val footerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val btnOpenApp = TextView(this).apply {
            text = "⚙ MENU LENGKAP"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundedBg(colNavyCard, Color.TRANSPARENT, 8f)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                closeMainPanel()
                startActivity(Intent(this@FloatingPanelService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val btnStopService = TextView(this).apply {
            text = "MATIKAN PANEL"
            setTextColor(Color.parseColor("#FFF87171"))
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundedBg(colCrimsonSoft, colCrimson, 8f, 1)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { stopSelf() }
        }
        footerRow.addView(btnOpenApp)
        footerRow.addView(spaceView(dp(8)))
        footerRow.addView(btnStopService)
        root.addView(footerRow)

        val params = WindowManager.LayoutParams(
            dp(290),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        panelView = root
        wm.addView(root, params)
    }

    private fun makeActionTile(title: String, sub: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(
                if (active) colCrimsonSoft else colNavyCard,
                if (active) colCrimson else Color.parseColor("#FF1E3A6E"),
                12f,
                2
            )
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }

            addView(TextView(this@FloatingPanelService).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
            addView(TextView(this@FloatingPanelService).apply {
                text = sub
                setTextColor(if (active) colCrimson else Color.parseColor("#FF94A3B8"))
                textSize = 10f
                gravity = Gravity.CENTER
            })
        }
    }

    private fun spaceView(sizePx: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
    }

    private fun showCrosshairOverlay(show: Boolean) {
        crosshairView?.let { runCatching { wm.removeView(it) } }
        crosshairView = null
        if (!show) return

        val view = object : View(this) {
            private val paintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colCrimson
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            private val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colCyan
                style = Paint.Style.FILL
            }
            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f
                val cy = height / 2f
                canvas.drawCircle(cx, cy, 18f, paintOuter)
                canvas.drawLine(cx - 28f, cy, cx - 8f, cy, paintOuter)
                canvas.drawLine(cx + 8f, cy, cx + 28f, cy, paintOuter)
                canvas.drawLine(cx, cy - 28f, cx, cy - 8f, paintOuter)
                canvas.drawLine(cx, cy + 8f, cx, cy + 28f, paintOuter)
                canvas.drawCircle(cx, cy, 4f, paintCenter)
            }
        }

        val params = WindowManager.LayoutParams(
            dp(64),
            dp(64),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = view
        wm.addView(view, params)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showCoordinatePickerOverlay() {
        targetDotView?.let { runCatching { wm.removeView(it) } }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(colNavyDark, colCrimson, 16f, 3)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val label = TextView(this).apply {
            text = "🎯 Geser ke tombol game
Lalu klik SIMPAN TITIK"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(140)
            y = dp(300)
        }

        val saveBtn = TextView(this).apply {
            text = "✔ SIMPAN TITIK"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(colCrimson, Color.TRANSPARENT, 8f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                val loc = IntArray(2)
                box.getLocationOnScreen(loc)
                val tapX = (loc[0] + box.width / 2f)
                val tapY = (loc[1] + box.height / 2f)

                val current = GameSpaceStore.getMacroSteps(this@FloatingPanelService).toMutableList()
                current.add(
                    MacroStep(
                        id = System.currentTimeMillis(),
                        label = "In-Game #${current.size + 1}",
                        x = tapX,
                        y = tapY,
                        delayMs = 120L,
                        holdMs = 45L
                    )
                )
                GameSpaceStore.saveMacroSteps(this@FloatingPanelService, current)
                Toast.makeText(
                    this@FloatingPanelService,
                    "Titik (${tapX.toInt()}, ${tapY.toInt()}) ditambahkan ke Macro!",
                    Toast.LENGTH_SHORT
                ).show()
                runCatching { wm.removeView(box) }
                targetDotView = null
                openMainPanel()
            }
        }

        box.addView(label)
        box.addView(spaceView(dp(6)))
        box.addView(saveBtn)

        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        box.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (ev.rawX - touchX).toInt()
                    params.y = initY + (ev.rawY - touchY).toInt()
                    wm.updateViewLayout(box, params)
                    true
                }
                else -> false
            }
        }

        targetDotView = box
        wm.addView(box, params)
    }

    override fun onDestroy() {
        fpsTracker?.stop()
        MacroRunner.stop()
        bubbleView?.let { runCatching { wm.removeView(it) } }
        panelView?.let { runCatching { wm.removeView(it) } }
        crosshairView?.let { runCatching { wm.removeView(it) } }
        targetDotView?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
