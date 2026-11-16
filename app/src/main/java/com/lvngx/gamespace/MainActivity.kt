package com.lvngx.gamespace

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// LVNG -X Signature Palette (Deep Navy Blue + Crimson Red)
private val NavyBg = Color(0xFF060E20)
private val NavySurface = Color(0xFF0D1B38)
private val NavyCard = Color(0xFF12254A)
private val Crimson = Color(0xFFE91E5A)
private val CrimsonGlow = Color(0xFFFF2A6D)
private val CyanAccent = Color(0xFF38BDF8)
private val TextMuted = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PerformanceBooster.applyMaxRefreshRate(window, true)
        setContent {
            LvngXApp()
        }
    }
}

@Composable
fun LvngXApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var savedGames by remember { mutableStateOf(GameSpaceStore.resolveSavedGames(context)) }
    var selectedGamePkg by remember { mutableStateOf(savedGames.firstOrNull()?.packageName) }
    var showAppPicker by remember { mutableStateOf(false) }

    var perfMode by remember { mutableStateOf(GameSpaceStore.getPerfMode(context)) }
    var audioFx by remember { mutableStateOf(GameSpaceStore.isAudioBoost(context)) }
    var crosshair by remember { mutableStateOf(GameSpaceStore.isCrosshairEnabled(context)) }
    var currentFps by remember { mutableIntStateOf(60) }
    var ramStats by remember { mutableStateOf(PerformanceBooster.getRamStats(context)) }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityGranted by remember { mutableStateOf(MacroAccessibilityService.isEnabled()) }

    // Real-time FPS & permission observer
    DisposableEffect(Unit) {
        val tracker = RealTimeFpsTracker { fps -> currentFps = fps }
        tracker.start()
        onDispose { tracker.stop() }
    }

    LaunchedEffect(selectedTab) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityGranted = MacroAccessibilityService.isEnabled()
            ramStats = PerformanceBooster.getRamStats(context)
            delay(1200L)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Crimson,
            secondary = CyanAccent,
            background = NavyBg,
            surface = NavySurface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = NavyBg,
            bottomBar = {
                NavigationBar(
                    containerColor = NavySurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                        label = { Text("Mulai Game", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonGlow,
                            indicatorColor = Crimson
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.TouchApp, contentDescription = null) },
                        label = { Text("Macro", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonGlow,
                            indicatorColor = Crimson
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        label = { Text("Fitur Menu", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonGlow,
                            indicatorColor = Crimson
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Security, contentDescription = null) },
                        label = { Text("Setup", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonGlow,
                            indicatorColor = Crimson
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1C3D), NavyBg)
                        )
                    )
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        savedGames = savedGames,
                        selectedGamePkg = selectedGamePkg,
                        onSelectGame = { selectedGamePkg = it },
                        onOpenPicker = { showAppPicker = true },
                        onRemoveGame = { pkg ->
                            GameSpaceStore.removeGamePackage(context, pkg)
                            savedGames = GameSpaceStore.resolveSavedGames(context)
                            if (selectedGamePkg == pkg) {
                                selectedGamePkg = savedGames.firstOrNull()?.packageName
                            }
                        },
                        currentFps = currentFps,
                        ramStats = ramStats,
                        overlayGranted = overlayGranted,
                        accessibilityGranted = accessibilityGranted,
                        onGoSetup = { selectedTab = 3 },
                        onLaunchGame = {
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "Izinkan Jendela Mengambang (Overlay) dulu!", Toast.LENGTH_SHORT).show()
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            } else {
                                // 1. Boost RAM & Lock Max Refresh Rate
                                val res = PerformanceBooster.boostMemory(context)
                                (context as? Activity)?.window?.let {
                                    PerformanceBooster.applyMaxRefreshRate(it, perfMode != PerfMode.ECO)
                                }

                                // 2. Start Floating Panel Service
                                val svcIntent = Intent(context, FloatingPanelService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(svcIntent)
                                } else {
                                    context.startService(svcIntent)
                                }

                                Toast.makeText(
                                    context,
                                    "LVNG -X Boost +${res.freedMb}MB • Floating Panel Aktif!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 3. Launch Selected Game if available
                                val targetPkg = selectedGamePkg
                                if (!targetPkg.isNullOrBlank()) {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    }
                                }
                            }
                        }
                    )

                    1 -> MacroEditorTab()

                    2 -> FeaturesMenuTab(
                        perfMode = perfMode,
                        onChangePerfMode = { mode ->
                            perfMode = mode
                            GameSpaceStore.setPerfMode(context, mode)
                            (context as? Activity)?.window?.let {
                                PerformanceBooster.applyMaxRefreshRate(it, mode != PerfMode.ECO)
                            }
                            if (mode == PerfMode.ULTRA) {
                                val res = PerformanceBooster.boostMemory(context)
                                Toast.makeText(context, "Ultra Boost: +${res.freedMb} MB RAM dibebaskan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        audioFx = audioFx,
                        onToggleAudio = {
                            audioFx = it
                            GameSpaceStore.setAudioBoost(context, it)
                            PerformanceBooster.toggleAudioFx(context, it)
                        },
                        crosshair = crosshair,
                        onToggleCrosshair = {
                            crosshair = it
                            GameSpaceStore.setCrosshairEnabled(context, it)
                        },
                        currentFps = currentFps,
                        ramStats = ramStats
                    )

                    3 -> SetupTab(
                        overlayGranted = overlayGranted,
                        accessibilityGranted = accessibilityGranted
                    )
                }

                if (showAppPicker) {
                    GamePickerDialog(
                        onDismiss = { showAppPicker = false },
                        onPick = { pkg ->
                            GameSpaceStore.addGamePackage(context, pkg)
                            savedGames = GameSpaceStore.resolveSavedGames(context)
                            selectedGamePkg = pkg
                            showAppPicker = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LvngXLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF081226))
            .border(2.dp, Crimson.copy(alpha = 0.7f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val w = size.width
            val h = size.height
            val upperWing = Path().apply {
                moveTo(w * 0.28f, h * 0.14f)
                lineTo(w * 0.54f, h * 0.06f)
                lineTo(w * 0.44f, h * 0.66f)
                lineTo(w * 0.14f, h * 0.84f)
                close()
            }
            val lowerWing = Path().apply {
                moveTo(w * 0.18f, h * 0.88f)
                lineTo(w * 0.48f, h * 0.60f)
                lineTo(w * 0.92f, h * 0.72f)
                lineTo(w * 0.80f, h * 0.90f)
                close()
            }
            drawPath(upperWing, color = Crimson)
            drawPath(lowerWing, color = CrimsonGlow)
        }
    }
}

@Composable
private fun DashboardTab(
    savedGames: List<GameItem>,
    selectedGamePkg: String?,
    onSelectGame: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onRemoveGame: (String) -> Unit,
    currentFps: Int,
    ramStats: Pair<Long, Long>,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    onGoSetup: () -> Unit,
    onLaunchGame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Status Strip (Shizuku / Permissions banner)
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, Crimson.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGoSetup() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LVNG -X",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(10.dp))
                val ready = overlayGranted && accessibilityGranted
                Text(
                    if (ready) "● Sistem Siap (Overlay & Macro Aktif)"
                    else "⚠ Belum diaktifkan. Ketuk untuk izin Overlay/Macro",
                    color = if (ready) CyanAccent else Color(0xFFFDA4AF),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ChevronRight, null, tint = Crimson, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Main Split Card matching Reference Image (Game Grid on Left + Big LVNG-X Centerpiece on Right)
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.5.dp, Crimson),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Installed / Added Games + Add (+) Button
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "GAME LIBRARY",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(savedGames) { game ->
                            val isSelected = game.packageName == selectedGamePkg
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Crimson.copy(alpha = 0.25f) else NavyCard)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Crimson else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectGame(game.packageName) }
                                    .padding(6.dp)
                            ) {
                                AppIconImage(game.icon, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    game.appName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // "+" Add Game Tile
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NavyCard)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { onOpenPicker() }
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah Game", tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("Tambah", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Right Column: Big LVNG-X Emblem + Live Telemetry
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LvngXLogoBadge(modifier = Modifier.size(116.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "LVNG -X",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "GAME SPACE",
                        color = CrimsonGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    // Telemetry Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip("FPS", "$currentFps", CyanAccent)
                        StatChip("RAM", "${ramStats.first / 1024f}".take(3) + "G", CrimsonGlow)
                    }

                    val activeName = savedGames.find { it.packageName == selectedGamePkg }?.appName ?: "Mode Overlay"
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Target: $activeName",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Main "Mulai Game" Button
        Button(
            onClick = onLaunchGame,
            colors = ButtonDefaults.buttonColors(containerColor = Crimson),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                "Mulai Game & Aktifkan Floating Panel",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NavyCard)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp)
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun MacroEditorTab() {
    val context = LocalContext.current
    var steps by remember { mutableStateOf(GameSpaceStore.getMacroSteps(context)) }
    var repeatCount by remember { mutableStateOf(GameSpaceStore.getMacroRepeat(context).toString()) }
    var infiniteLoop by remember { mutableStateOf(GameSpaceStore.isMacroLoop(context)) }
    var labelInput by remember { mutableStateOf("Tap Skill") }
    var xInput by remember { mutableStateOf("850") }
    var yInput by remember { mutableStateOf("520") }
    var delayInput by remember { mutableStateOf("120") }
    var statusText by remember { mutableStateOf(if (MacroRunner.isRunning) "Macro sedang berjalan" else "Siap") }

    DisposableEffect(Unit) {
        MacroRunner.onStateChanged = { _, msg -> statusText = msg }
        onDispose { MacroRunner.onStateChanged = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("MACRO ENGINE", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Auto-Tap & Combo Gesture", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))

        // Add Coordinate Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavyCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Nama Tombol") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = delayInput,
                        onValueChange = { delayInput = it },
                        label = { Text("Jeda (ms)") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xInput,
                        onValueChange = { xInput = it },
                        label = { Text("Koordinat X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = yInput,
                        onValueChange = { yInput = it },
                        label = { Text("Koordinat Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = repeatCount,
                        onValueChange = {
                            repeatCount = it
                            it.toIntOrNull()?.let { n -> GameSpaceStore.setMacroRepeat(context, n) }
                        },
                        label = { Text("Ulangi (x)") },
                        modifier = Modifier.weight(1f),
                        enabled = !infiniteLoop,
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = infiniteLoop,
                            onCheckedChange = {
                                infiniteLoop = it
                                GameSpaceStore.setMacroLoop(context, it)
                            }
                        )
                        Text("Loop Terus", color = Color.White, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = {
                        val sx = xInput.toFloatOrNull()
                        val sy = yInput.toFloatOrNull()
                        val sd = delayInput.toLongOrNull()
                        if (sx != null && sy != null && sd != null) {
                            val updated = steps + MacroStep(
                                label = labelInput.ifBlank { "Tap" },
                                x = sx,
                                y = sy,
                                delayMs = sd.coerceAtLeast(20L)
                            )
                            steps = updated
                            GameSpaceStore.saveMacroSteps(context, updated)
                            statusText = "Titik ditambahkan (${sx.toInt()}, ${sy.toInt()})"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddLocationAlt, null, tint = CyanAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Langkah Macro", color = CyanAccent)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Daftar Urutan (${steps.size} Step) • Tips: Gunakan '🎯 SET TITIK' di Floating Panel saat dalam game!", color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(steps, key = { it.id }) { step ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("X: ${step.x.toInt()}  |  Y: ${step.y.toInt()}  |  Delay: ${step.delayMs} ms", color = CyanAccent, fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            val updated = steps.filterNot { it.id == step.id }
                            steps = updated
                            GameSpaceStore.saveMacroSteps(context, updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Crimson)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (!MacroAccessibilityService.isEnabled()) {
                        Toast.makeText(context, "Aktifkan Layanan Aksesibilitas LVNG -X terlebih dahulu!", Toast.LENGTH_LONG).show()
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else {
                        val rep = repeatCount.toIntOrNull() ?: 1
                        MacroRunner.play(context, steps, rep, infiniteLoop)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text("Tes Macro")
            }
            OutlinedButton(
                onClick = { MacroRunner.stop() },
                border = BorderStroke(1.dp, Crimson)
            ) {
                Text("Stop", color = CrimsonGlow)
            }
        }
        Text("Status: $statusText", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun FeaturesMenuTab(
    perfMode: PerfMode,
    onChangePerfMode: (PerfMode) -> Unit,
    audioFx: Boolean,
    onToggleAudio: (Boolean) -> Unit,
    crosshair: Boolean,
    onToggleCrosshair: (Boolean) -> Unit,
    currentFps: Int,
    ramStats: Pair<Long, Long>
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FITUR MENU", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Performance, Audio FX & FPS Boost", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        // Performance Mode Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, Crimson.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null, tint = Crimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Performance & FPS Boost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("3 Modes — Eco, Balanced, Ultra Boost (Max Refresh Rate)", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerfMode.entries.forEach { mode ->
                        val active = perfMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Crimson else NavyCard)
                                .clickable { onChangePerfMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "FPS Layar Saat Ini: $currentFps FPS  •  RAM Terpakai: ${ramStats.first}MB / ${ramStats.second}MB",
                    color = CyanAccent,
                    fontSize = 11.sp
                )
            }
        }

        // Audio FX Toggle
        FeatureSwitchCard(
            icon = Icons.Default.GraphicEq,
            title = "Audio FX (EQ & Bass Boost)",
            subtitle = "Tingkatkan kejernihan suara langkah kaki (footstep) di game",
            checked = audioFx,
            onCheckedChange = onToggleAudio
        )

        // Crosshair Toggle
        FeatureSwitchCard(
            icon = Icons.Default.CenterFocusStrong,
            title = "Virtual Crosshair",
            subtitle = "Tampilkan titik bidik presisi di tengah layar saat game berjalan",
            checked = crosshair,
            onCheckedChange = onToggleCrosshair
        )

        // Instant Memory Cleaner Button
        Button(
            onClick = {
                val res = PerformanceBooster.boostMemory(context)
                Toast.makeText(context, "RAM Dibersihkan: +${res.freedMb} MB (${res.killedCount} proses latar)", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
            border = BorderStroke(1.dp, CyanAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CleaningServices, null, tint = CyanAccent)
            Spacer(Modifier.width(8.dp))
            Text("Bersihkan RAM & Cache Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Crimson, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Crimson)
            )
        }
    }
}

@Composable
private fun SetupTab(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("APK INFO & SETUP", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Izin Sistem LVNG -X", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        // Step 1: Overlay Permission
        StepActionCard(
            step = "Step 1: Izin Jendela Mengambang",
            desc = "Wajib agar panel FPS, Crosshair, & tombol Macro muncul di atas game",
            isDone = overlayGranted,
            btnLabel = if (overlayGranted) "Aktif" else "Grant",
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }
        )

        // Step 2: Accessibility Service for Macro
        StepActionCard(
            step = "Step 2: Layanan Aksesibilitas Macro",
            desc = "Wajib agar fitur Auto-Tap Macro dapat menyentuh layar secara otomatis",
            isDone = accessibilityGranted,
            btnLabel = if (accessibilityGranted) "Aktif" else "Start",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        Spacer(Modifier.weight(1f))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, Crimson),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LvngXLogoBadge(modifier = Modifier.size(64.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("LVNG -X", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        "Game Space by LVNG-X: Boost, FPS Unlock, Crosshair & Macro dalam panel mengambang minimalis.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepActionCard(
    step: String,
    desc: String,
    isDone: Boolean,
    btnLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDone) CyanAccent.copy(alpha = 0.2f) else Crimson.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDone) Icons.Default.CheckCircle else Icons.Default.Pending,
                    contentDescription = null,
                    tint = if (isDone) CyanAccent else Crimson
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDone) NavyCard else Crimson),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(btnLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GamePickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val allApps = remember { GameSpaceStore.loadInstalledLaunchableApps(context) }
    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text("Pilih Game / Aplikasi", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari nama game...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyCard)
                                .clickable { onPick(app.packageName) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(app.icon, Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(10.dp))
                            Text(app.appName, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = CrimsonGlow)
            }
        }
    )
}

@Composable
private fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Box(modifier.background(NavyCard), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SportsEsports, null, tint = Crimson)
        }
        return
    }
    val bitmap = remember(drawable) {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = modifier)
}
