package com.myvpn.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var servers: List<Server>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        servers = ServerRepository.load(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { HomeScreen(servers) }
            }
        }
    }
    private var pendingServer: Server? = null
    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) pendingServer?.let { startVpn(it) }
    }
    fun requestConnect(server: Server) {
        pendingServer = server
        val prepare = VpnService.prepare(this)
        if (prepare != null) vpnPermission.launch(prepare) else startVpn(server)
    }
    private fun startVpn(server: Server) {
        val intent = Intent(this, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_SERVER_JSON, XrayConfigBuilder.build(server))
        }
        startForegroundService(intent)
    }
    fun requestDisconnect() {
        startService(Intent(this, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_DISCONNECT
        })
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun HomeScreen(servers: List<Server>) {
        var selected by remember { mutableStateOf(servers.firstOrNull()) }
        var showPicker by remember { mutableStateOf(false) }
        var connected by remember { mutableStateOf(V2RayVpnService.isRunning) }
        var elapsed by remember { mutableStateOf(0L) }
        // تحديث حالة الاتصال والعداد
        LaunchedEffect(Unit) {
            while (true) {
                connected = V2RayVpnService.isRunning
                elapsed = if (connected && V2RayVpnService.connectedAt > 0)
                    (System.currentTimeMillis() - V2RayVpnService.connectedAt) / 1000 else 0L
                delay(1000)
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (connected) "متصل" else "غير متصل",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            if (connected) {
                Text(
                    text = "%02d:%02d:%02d".format(elapsed / 3600, (elapsed / 60) % 60, elapsed % 60),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(32.dp))
            } else {
                Spacer(Modifier.height(56.dp))
            }
            Button(
                onClick = {
                    if (connected) requestDisconnect()
                    else selected?.let { requestConnect(it) }
                },
                modifier = Modifier.size(160.dp)
            ) {
                Text(if (connected) "قطع" else "اتصال")
            }
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = { showPicker = true }) {
                Text("السيرفر: ${selected?.name ?: "-"}")
            }
        }
        if (showPicker) {
            ModalBottomSheet(onDismissRequest = { showPicker = false }) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(servers) { s ->
                        ListItem(
                            headlineContent = { Text(s.name) },
                            supportingContent = { Text(s.address) },
                            modifier = Modifier.clickable {
                                selected = s
                                showPicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}
