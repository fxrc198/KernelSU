package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFFF4F5F7)
private val Panel = Color(0xFFFFFFFF)
private val Panel2 = Color(0xFFEDEFF2)
private val Border = Color(0xFFE3E6EA)
private val TextColor = Color(0xFF1A1C1E)
private val Muted = Color(0xFF6B7280)
private val Accent = Color(0xFF1F2937)
private val WarnColor = Color(0xFFD97706)
private val OkColor = Color(0xFF16A34A)
private val BadColor = Color(0xFFDC2626)

@Composable
fun HomePagerRune(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val sys = state.systemInfo
    val installed = state.ksuVersion != null
    val rootOk = state.isRootAvailable

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomInnerPadding + 24.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Text("RuneSU", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Spacer(Modifier.height(2.dp))
        Text(
            "${sys.deviceModel} · ${sys.managerVersion}",
            fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))
        StatusCard(installed, rootOk, state, actions)

        Spacer(Modifier.height(8.dp))
        PerfCard(state)

        Spacer(Modifier.height(16.dp))
        Text("设备", fontSize = 11.sp, color = Muted,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        DeviceCard(sys)

        Spacer(Modifier.height(16.dp))
        Text("安全", fontSize = 11.sp, color = Muted,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        PlainCard {
            InfoRow(Icons.Outlined.Security, "SELinux", sys.selinuxStatus.ifBlank { "未知" })
            HorizontalDivider(thickness = 0.7.dp, color = Border)
            InfoRow(Icons.Outlined.Tune, "Seccomp", seccompText(sys.seccompStatus))
        }
    }
}

@Composable
private fun StatusCard(
    installed: Boolean,
    rootOk: Boolean,
    state: HomeUiState,
    actions: HomeActions,
) {
    val color = when {
        !installed -> Muted
        rootOk -> OkColor
        else -> BadColor
    }
    val title = when {
        !installed -> "未安装"
        state.isLateLoadMode -> "临时加载模式"
        state.isSafeMode -> "安全模式"
        rootOk -> "Root 已连接"
        else -> "Root 不可用"
    }
    val sub = when {
        !installed -> "点击安装 KernelSU"
        else -> "KernelSU ${state.ksuVersion}"
    }

    Surface(
        color = Panel,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !installed) { actions.onInstallClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor)
                Spacer(Modifier.height(2.dp))
                Text(sub, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PerfCard(state: HomeUiState) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 14.dp)) {
            PerfCol("内核", state.kernelVersion.toString())
            PerfCol("KSU", state.ksuVersion?.toString() ?: "—")
            PerfCol("UAPI", state.kernelUAPIVersion?.toString() ?: "—")
        }
    }
}

@Composable
private fun RowScope.PerfCol(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun DeviceCard(sys: SystemInfo) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row {
                GridCell(Icons.Outlined.Memory, "内核", sys.kernelVersion, Modifier.weight(1f))
                GridCell(Icons.Outlined.Code, "管理器", sys.managerVersion, Modifier.weight(1f))
            }
            HorizontalDivider(thickness = 0.7.dp, color = Border)
            Row {
                GridCell(Icons.Outlined.PhoneAndroid, "设备", sys.deviceModel, Modifier.weight(1f))
                GridCell(Icons.Outlined.Bolt, "版本", sys.fingerprint, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GridCell(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Muted, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 9.5.sp, color = Muted)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value.ifBlank { "—" }, fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
            color = TextColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlainCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 2.dp), content = content)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Muted, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextColor)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 11.sp, color = Muted, lineHeight = 16.sp)
        }
    }
}

private fun seccompText(v: Int): String = when (v) {
    0 -> "关闭"
    1 -> "严格模式"
    2 -> "过滤模式"
    else -> "未知"
}
