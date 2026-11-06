package com.myvpn.app

import android.content.Context
import org.json.JSONObject

data class Server(
    val name: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val wsPath: String,
    val sni: String,
    val host: String
)

object ServerRepository {
    fun load(context: Context): List<Server> {
        val text = context.assets.open("servers.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("servers")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Server(
                name = o.getString("name"),
                address = o.getString("address"),
                port = o.optInt("port", 443),
                uuid = o.getString("uuid"),
                wsPath = o.optString("wsPath", "/"),
                sni = o.optString("sni", o.getString("address")),
                host = o.optString("host", o.getString("address"))
            )
        }
    }
}
