package com.lvngx.gamespace

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        MacroRunner.stop()
    }

    override fun onDestroy() {
        MacroRunner.stop()
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun performTap(x: Float, y: Float, durationMs: Long = 45L, onDone: (() -> Unit)? = null) {
        val safeX = x.coerceAtLeast(1f)
        val safeY = y.coerceAtLeast(1f)
        val path = Path().apply {
            moveTo(safeX, safeY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(20L, 1000L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onDone?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onDone?.invoke()
            }
        }, null)

        if (!dispatched) {
            onDone?.invoke()
        }
    }
}

object MacroRunner {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    var isRunning: Boolean = false
        private set

    var onStateChanged: ((Boolean, String) -> Unit)? = null

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        onStateChanged?.invoke(false, "Macro berhenti")
    }

    fun play(
        context: Context,
        steps: List<MacroStep>,
        repeatCount: Int = 1,
        infiniteLoop: Boolean = false
    ): Boolean {
        stop()
        val service = MacroAccessibilityService.instance ?: return false
        if (steps.isEmpty()) return false

        isRunning = true
        onStateChanged?.invoke(true, if (infiniteLoop) "Looping..." else "Running (${repeatCount}x)")

        var currentRound = 0

        fun executeStep(stepIndex: Int) {
            if (!isRunning) return
            if (stepIndex >= steps.size) {
                currentRound++
                if (infiniteLoop || currentRound < repeatCount) {
                    handler.postDelayed({ executeStep(0) }, 60L)
                } else {
                    isRunning = false
                    onStateChanged?.invoke(false, "Selesai (${currentRound}x)")
                }
                return
            }

            val step = steps[stepIndex]
            service.performTap(step.x, step.y, step.holdMs) {
                if (isRunning) {
                    handler.postDelayed({ executeStep(stepIndex + 1) }, step.delayMs.coerceAtLeast(15L))
                }
            }
        }

        executeStep(0)
        return true
    }
}
