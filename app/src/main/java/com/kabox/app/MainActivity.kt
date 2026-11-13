package com.kabox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.kabox.app.data.LocalStore
import com.kabox.app.usage.AppUsage
import com.kabox.app.usage.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val Ink = Color(0xFF0B0D18)
private val Panel = Color(0xFF151827)
private val PanelRaised = Color(0xFF1C2032)
private val Gold = Color(0xFFD8B56D)
private val GoldLight = Color(0xFFF0D99D)
private val Muted = Color(0xFFAAA9B5)
private val Lavender = Color(0xFF9C8BD7)
private val Mint = Color(0xFF8FC7A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 13, 24)
        window.navigationBarColor = android.graphics.Color.rgb(11, 13, 24)
        setContent { KaboxTheme { KaboxApp() } }
    }
}

@Composable
private fun KaboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Ink,
            secondary = Lavender,
            background = Ink,
            surface = Panel,
            onSurface = Color(0xFFF5F0E6),
            onBackground = Color(0xFFF5F0E6)
        ),
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
        ),
        content = content
    )
}

private enum class Tab(val title: String) {
    HOME("Home"), FOCUS("Focus"), PROGRESS("Progress"), QUESTS("Quests"), MORE("More")
}

@Composable
private fun KaboxApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var duration by remember { mutableIntStateOf(store.selectedMinutes()) }
    var running by remember { mutableStateOf(store.isTimerRunning()) }
    var remaining by remember { mutableLongStateOf(store.remainingSeconds()) }
    var refreshUsage by remember { mutableIntStateOf(0) }

    LaunchedEffect(running, refreshUsage) {
        while (running) {
            remaining = store.remainingSeconds()
            if (remaining <= 0L) {
                store.finishIfDue()
                running = false
                remaining = 0L
                break
            }
            delay(250L)
        }
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101321), contentColor = Gold) {
                val entries = listOf(
                    Tab.HOME to Icons.Outlined.Home,
                    Tab.FOCUS to Icons.Outlined.HourglassTop,
                    Tab.PROGRESS to Icons.Outlined.BarChart,
                    Tab.QUESTS to Icons.Outlined.MenuBook,
                    Tab.MORE to Icons.Outlined.AutoAwesome
                )
                entries.forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item; if (item == Tab.PROGRESS) refreshUsage++ },
                        icon = { Icon(icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF111526), Ink, Color(0xFF090B13))))
                .padding(innerPadding)
        ) {
            AnimatedContent(targetState = tab, label = "tab_content") { current ->
                when (current) {
                    Tab.HOME -> HomeScreen(store, remaining, running, onFocus = { tab = Tab.FOCUS }, onQuests = { tab = Tab.QUESTS })
                    Tab.FOCUS -> FocusScreen(
                        duration = duration,
                        remaining = remaining,
                        running = running,
                        onDuration = { minutes -> duration = minutes; store.setSelectedMinutes(minutes); store.resetTimer(); remaining = store.remainingSeconds(); running = false },
                        onStart = { if (remaining <= 0L || (!running && remaining == duration * 60L)) { store.startNew(duration) } else { store.resume() }; running = true; remaining = store.remainingSeconds() },
                        onPause = { store.pause(); running = false; remaining = store.remainingSeconds() },
                        onReset = { store.resetTimer(); running = false; remaining = store.remainingSeconds() }
                    )
                    Tab.PROGRESS -> ProgressScreen(store, refreshUsage)
                    Tab.QUESTS -> QuestsScreen(store)
                    Tab.MORE -> MoreScreen(onProgress = { tab = Tab.PROGRESS })
                }
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun BrandHeader(eyebrow: String = "THE ACADEMY OF FOCUS") {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("KABOX", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text(eyebrow, color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun HomeScreen(store: LocalStore, remaining: Long, running: Boolean, onFocus: () -> Unit, onQuests: () -> Unit) {
    PageColumn {
        BrandHeader()
        Spacer(Modifier.height(4.dp))
        Text("Your next chapter\nstarts now.", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFF4EEDD), lineHeight = 38.sp)
        Text("A quieter mind. A stronger routine. One focused session at a time.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f))
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = Gold, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("TODAY'S FOCUS", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text("Make this hour count", color = Color(0xFFF4EEDD), style = MaterialTheme.typography.titleLarge)
                    }
                    Text("✧", color = GoldLight, fontSize = 28.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniMetric("${store.dailyFocusMinutes()}", "FOCUS MIN", Modifier.weight(1f))
                    MiniMetric("${store.completedSessions()}", "SESSIONS", Modifier.weight(1f))
                    MiniMetric("${store.streakDays()} days", "STREAK", Modifier.weight(1f))
                }
                Button(onClick = onFocus, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = Ink)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Return to focus · ${formatTime(remaining)}" else "Enter focus chamber", color = Ink, fontWeight = FontWeight.Bold)
                }
            }
        }

        SectionTitle("Today's small victories", "Consistency is its own kind of magic.")
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuestRow("Read or revise for 25 minutes", store.isQuestDone(0), onQuests)
                Divider(color = Color.White.copy(alpha = 0.08f))
                QuestRow("Complete one practice set", store.isQuestDone(1), onQuests)
                Divider(color = Color.White.copy(alpha = 0.08f))
                QuestRow("Take a mindful screen break", store.isQuestDone(2), onQuests)
                Text("Edit and complete quests in the Quests tab.", color = Muted, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onQuests))
            }
        }

        SectionTitle("Explore the academy", "More rooms are being prepared.")
        FeatureTile("The Archive", "Study plans and learning rooms", "📚", "IN DEVELOPMENT")
        FeatureTile("The Shield", "Distraction and app blocking", "🛡", "IN DEVELOPMENT")
    }
}

@Composable
private fun MiniMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(PanelRaised).padding(vertical = 11.dp, horizontal = 8.dp)) {
        Text(value, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Color(0xFFF2EBDD), style = MaterialTheme.typography.titleLarge)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun QuestRow(title: String, done: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = if (done) Mint else Muted.copy(alpha = 0.65f))
        Spacer(Modifier.width(10.dp))
        Text(title, color = if (done) Muted else Color(0xFFEDE8DE), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(if (done) "DONE" else "VIEW", color = if (done) Mint else Gold, fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun FeatureTile(title: String, description: String, symbol: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 25.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFFF2EBDD), fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 11.sp)
            }
            Text(status, color = Gold, fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun FocusScreen(
    duration: Int,
    remaining: Long,
    running: Boolean,
    onDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    PageColumn {
        BrandHeader("THE FOCUS CHAMBER")
        Text("Stillness is\nyour superpower.", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFF4EEDD), lineHeight = 38.sp)
        Text("Choose a session, close the noise, and begin.", color = Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 45, 60).forEach { minutes ->
                FilterChip(
                    selected = duration == minutes,
                    onClick = { onDuration(minutes) },
                    label = { Text("$minutes min") },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold,
                        selectedLabelColor = Ink,
                        labelColor = Color(0xFFECE5D6)
                    )
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                    val progress by animateFloatAsState(
                        targetValue = (remaining.toFloat() / (duration * 60f)).coerceIn(0f, 1f),
                        animationSpec = tween(450, easing = FastOutSlowInEasing), label = "timer_progress"
                    )
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = Gold, trackColor = Color.White.copy(alpha = 0.08f), strokeWidth = 7.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✦", color = Gold, fontSize = 20.sp)
                        Text(formatTime(remaining), color = Color(0xFFF5EBD5), fontSize = 42.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Serif)
                        Text(if (running) "FOCUS IN PROGRESS" else "READY WHEN YOU ARE", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
                Text("One task. One session. No rush.", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = if (running) onPause else onStart, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold), shape = RoundedCornerShape(14.dp)) {
                        Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null, tint = Ink)
                        Spacer(Modifier.width(6.dp))
                        Text(if (running) "Pause" else if (remaining < duration * 60L) "Resume" else "Start focus", color = Ink, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.size(50.dp), contentPadding = PaddingValues(0.dp), border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f)), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer", tint = Gold)
                    }
                }
            }
        }
        Text("Your session is saved on this device. The timer restores its remaining time when you reopen Kabox. Background completion notifications are not enabled in this first build.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun ProgressScreen(store: LocalStore, refreshTrigger: Int) {
    val context = LocalContext.current
    var permission by remember { mutableStateOf(UsageRepository.hasPermission(context)) }
    var usage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var usageRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger, usageRefresh) {
        permission = UsageRepository.hasPermission(context)
        if (permission) {
            loading = true
            usage = withContext(Dispatchers.IO) { UsageRepository.loadLast24Hours(context) }
            loading = false
        } else {
            usage = emptyList()
        }
    }

    PageColumn {
        BrandHeader("YOUR PROGRESS")
        Text("Proof of your\nquiet discipline.", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFF4EEDD), lineHeight = 38.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("${store.dailyFocusMinutes()}", "TODAY · MIN", Modifier.weight(1f))
            SummaryCard("${store.totalFocusMinutes()}", "TOTAL · MIN", Modifier.weight(1f))
            SummaryCard("${store.completedSessions()}", "SESSIONS", Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SCREEN-TIME LEDGER", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("App usage · Last 24 hours", color = Color(0xFFF2EBDD), style = MaterialTheme.typography.titleLarge)
                Text("Kabox reads Android's Usage Access data only after you grant permission. Usage stays on this device in this build.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                if (!permission) {
                    Button(onClick = { UsageRepository.openPermissionSettings(context) }, colors = ButtonDefaults.buttonColors(containerColor = Gold), shape = RoundedCornerShape(12.dp)) {
                        Text("Grant Usage Access", color = Ink, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(onClick = { usageRefresh++ }, border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))) {
                        Text("Refresh usage", color = Gold)
                    }
                    if (loading) Text("Loading app usage…", color = Muted, fontSize = 12.sp)
                    else if (usage.isEmpty()) Text("No usage records were returned for this period.", color = Muted, fontSize = 12.sp)
                    usage.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.label, color = Color(0xFFEDE8DE), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${item.minutes} min", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Divider(color = Color.White.copy(alpha = 0.07f))
                    }
                }
            }
        }
        Text("Usage totals are supplied by Android and may be delayed or rounded by the operating system.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, color = GoldLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
private fun QuestsScreen(store: LocalStore) {
    var done0 by remember { mutableStateOf(store.isQuestDone(0)) }
    var done1 by remember { mutableStateOf(store.isQuestDone(1)) }
    var done2 by remember { mutableStateOf(store.isQuestDone(2)) }
    PageColumn {
        BrandHeader("THE DAILY GRIMOIRE")
        Text("Small quests.\nLasting change.", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFF4EEDD), lineHeight = 38.sp)
        Text("Mark each quest when you complete it. Your checklist stays on this device.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        QuestCard("01", "Read or revise for 25 minutes", "Give one topic your full attention.", done0) { done0 = it; store.setQuestDone(0, it) }
        QuestCard("02", "Complete one practice set", "Turn what you know into something you can use.", done1) { done1 = it; store.setQuestDone(1, it) }
        QuestCard("03", "Take a mindful screen break", "Step away from the scroll for a little while.", done2) { done2 = it; store.setQuestDone(2, it) }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("A NOTE FROM THE ACADEMY", color = Gold, fontSize = 10.sp, letterSpacing = 1.7.sp)
                Spacer(Modifier.height(6.dp))
                Text("You do not need a perfect day. You need a next good decision.", color = Color(0xFFF1E9D9), fontFamily = FontFamily.Serif, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun QuestCard(number: String, title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (checked) Mint.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(number, color = Gold, fontFamily = FontFamily.Serif, fontSize = 24.sp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color(0xFFF1EBDD), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(description, color = Muted, fontSize = 10.sp)
            }
            Checkbox(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun MoreScreen(onProgress: () -> Unit) {
    PageColumn {
        BrandHeader("THE ACADEMY")
        Text("Every great routine\nneeds a little wonder.", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFF4EEDD), lineHeight = 38.sp)
        FeatureTile("Usage ledger", "Review app usage with Android permission", "◷", "AVAILABLE")
        FeatureTile("App shield", "Choose apps to pause during focus", "🛡", "PLANNED")
        FeatureTile("Forbidden sites", "Domain-level website blocking", "⌁", "PLANNED")
        FeatureTile("Common rooms", "Live group study sessions", "✧", "PLANNED")
        FeatureTile("House Cup", "Fair points and friendly rankings", "🏆", "PLANNED")
        FeatureTile("Enchanted ambience", "Focus sounds and soundscapes", "♫", "PLANNED")
        Button(onClick = onProgress, colors = ButtonDefaults.buttonColors(containerColor = Gold), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Ink)
            Spacer(Modifier.width(8.dp))
            Text("Open progress & usage", color = Ink, fontWeight = FontWeight.Bold)
        }
        Text("Kabox v0.1.0 · Original wizard-academy inspired identity", color = Muted, fontSize = 10.sp)
    }
}


private fun formatTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60L
    val secs = safe % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}
