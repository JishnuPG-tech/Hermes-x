package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.ModelTelemetryItemDto
import com.example.hermes.data.TraceTelemetryItemDto
import com.example.hermes.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OmniRouteScreen(
    onBack: () -> Unit,
    viewModel: OmniRouteViewModel = viewModel()
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "OmniRoute HUD",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )

                IconButton(onClick = { viewModel.fetchTelemetry() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner: Upstream Status & Active Gateway
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4E9F6E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ROUTER ${telemetry.status.uppercase()}",
                                    style = HermesTypography.labelSmall.copy(
                                        color = Color(0xFF4E9F6E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CanvasNearBlack)
                                    .border(1.dp, BorderSubtle, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Auto-Failover ON",
                                    style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Upstream Gateway:",
                            style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = telemetry.active_upstream.ifBlank { "https://jishnupg-opencode-cli.hf.space/v1" },
                            style = HermesTypography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = BrandCoral,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Active Primary Model: ${telemetry.active_model.ifBlank { "antigravity/gemini-2.5-flash" }}",
                            style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm, fontSize = 14.sp)
                        )
                    }
                }

                // 3 Quick Metrics Gauges
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Avg Latency",
                            value = "${telemetry.avg_latency_ms} ms",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Cache Hit",
                            value = telemetry.cache_hit_rate,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total Routes",
                            value = "${telemetry.total_requests}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section: Provider Fleet
                item {
                    Text(
                        text = "ROUTED PROVIDER FLEET",
                        style = HermesTypography.labelSmall.copy(
                            color = TextSubtle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(telemetry.models) { modelItem ->
                    ModelFleetCard(modelItem)
                }

                // Section: Real-time Trace Log
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LIVE INFERENCE TRACES",
                        style = HermesTypography.labelSmall.copy(
                            color = TextSubtle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(telemetry.recent_traces) { trace ->
                    TraceItemRow(trace)
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F1E1C))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 11.5.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = HermesTypography.titleLarge.copy(
                color = TextPrimaryWarm,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ModelFleetCard(item: ModelTelemetryItemDto) {
    val isActive = item.status == "active"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F1E1C))
            .border(
                1.dp,
                if (isActive) BrandCoral.copy(alpha = 0.5f) else BorderSubtle,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFF4E9F6E) else TextSubtle)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.provider,
                    style = HermesTypography.bodySmall.copy(
                        color = if (isActive) BrandCoral else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.id,
                style = HermesTypography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimaryWarm,
                    fontSize = 13.5.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Uptime: ${item.uptime_pct}% • ${item.cost_per_1m}/1M",
                style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 11.5.sp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CanvasNearBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${item.latency_ms}ms",
                style = HermesTypography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimaryWarm,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun TraceItemRow(trace: TraceTelemetryItemDto) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = sdf.format(Date(trace.timestamp * 1000))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF191816))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeStr,
                    style = HermesTypography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSubtle,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trace.model.substringAfterLast('/'),
                    style = HermesTypography.bodyMedium.copy(
                        color = TextPrimaryWarm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${trace.tokens_in} in / ${trace.tokens_out} out • ${trace.latency_ms}ms",
                style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 11.5.sp)
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF1E3A29))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${trace.status} OK",
                style = HermesTypography.labelSmall.copy(
                    color = Color(0xFF4E9F6E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
