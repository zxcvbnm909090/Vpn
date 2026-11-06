package com.myvpn.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class V2RayVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.myvpn.app.CONNECT"
        const val ACTION_DISCONNECT = "com.myvpn.app.DISCONNECT"
        const val EXTRA_SERVER_JSON = "server_config"

        @Volatile var isRunning = false
            private set
        @Volatile var connectedAt: Long = 0L
            private set
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var controller: CoreController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_SERVER_JSON) ?: return START_NOT_STICKY
                startForeground(1, buildNotification("متصل"))
                connect(config)
                START_STICKY
            }
            ACTION_DISCONNECT -> { disconnect(); START_NOT_STICKY }
            else -> START_NOT_STICKY
        }
    }

    private fun connect(configJson: String) {
        scope.launch {
            try {
                val tun = Builder()
                    .setSession("MyVPN")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 30)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .establish() ?: throw IllegalStateException("تعذّر إنشاء واجهة VPN")
                tunInterface = tun

                val handler = object : CoreCallbackHandler {
                    override fun startup(): Long = 0L
                    override fun shutdown(): Long = 0L
                    override fun onEmitStatus(p0: Long, p1: String?): Long = 0L
                }
                controller = Libv2ray.newCoreController(handler).also {
                    // تمرير الـ fd الخاص بالـ TUN إلى المحرك
                    it.startLoop(configJson, tun.fd)
                }

                isRunning = true
                connectedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect()
            }
        }
    }

    private fun disconnect() {
        try { controller?.stopLoop() } catch (_: Exception) {}
        controller = null
        tunInterface?.close()
        tunInterface = null
        isRunning = false
        connectedAt = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() = disconnect()
    override fun onDestroy() { disconnect(); super.onDestroy() }

    private fun buildNotification(text: String): Notification {
        val channelId = "vpn_status"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "حالة VPN", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyVPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }
}
