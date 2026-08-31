package com.remembrall.app.ui.ring

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remembrall.app.data.repo.DatabaseProvider
import com.remembrall.app.notify.AlarmService
import com.remembrall.app.notify.ReminderNotifier
import com.remembrall.app.ui.theme.RemembrallTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Full-screen ringing screen shown when an alarm-style reminder fires. */
class RingActivity : ComponentActivity() {

    private var dismissed = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == com.remembrall.app.engine.AlarmReceiver.ACTION_DISMISSED) {
                finishQuietly()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShow()

        ContextCompat.registerReceiver(
            this, dismissReceiver,
            IntentFilter(com.remembrall.app.engine.AlarmReceiver.ACTION_DISMISSED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val reminderId = intent.getLongExtra(EXTRA_ID, -1)

        setContent {
            RemembrallTheme {
                RingScreen(
                    reminderId = reminderId,
                    onDismiss = { dismiss(reminderId) }
                )
            }
        }
    }

    private fun turnScreenOnAndShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismiss(reminderId: Long) {
        dismissed = true
        ReminderNotifier.cancel(this, reminderId)
        runCatching { startService(AlarmService.stopIntent(this, reminderId)) }
        sendBroadcast(
            Intent(com.remembrall.app.engine.AlarmReceiver.ACTION_DISMISSED)
                .setPackage(packageName)
        )
        finish()
    }

    private fun finishQuietly() {
        if (!isFinishing) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(dismissReceiver) }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ID = "reminder_id"
        fun intent(context: Context, reminderId: Long): Intent =
            Intent(context, RingActivity::class.java).apply {
                putExtra(EXTRA_ID, reminderId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}

@Composable
fun RingScreen(reminderId: Long, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("记忆球提醒") }
    var note by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reminderId) {
        scope.launch {
            val r = DatabaseProvider.reminderDao(context).getById(reminderId)
            if (r != null) {
                title = r.title
                note = r.note
            }
        }
    }

    val now = remember { java.util.Calendar.getInstance() }
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val c = java.util.Calendar.getInstance()
            clock = "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
            delay(1000)
        }
    }

    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Layered diagonal gradient (indigo -> violet -> warm) instead of flat purple.
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6E5BFF),
                        Color(0xFF5B6CFF),
                        Color(0xFF4E7BF0),
                        Color(0xFF7A5AF0)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1200f, 2200f)
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow behind the content for depth.
        Box(
            Modifier
                .size(360.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.18f),
                            1f to Color.Transparent
                        )
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                clock,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .size((104 * scale).dp)
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(36.dp))
            Text(
                title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    note,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(72.dp))
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("我知道了", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

