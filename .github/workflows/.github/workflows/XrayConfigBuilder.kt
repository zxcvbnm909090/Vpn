package com.myvpn.app

import org.json.JSONArray
import org.json.JSONObject

/** يبني إعدادات Xray (VLESS + WebSocket + TLS) للسيرفر المختار */
object XrayConfigBuilder {
    fun build(server: Server): String {
        val user = JSONObject()
            .put("id", server.uuid)
            .put("encryption", "none")

        val vnext = JSONObject()
            .put("address", server.address)
            .put("port", server.port)
            .put("users", JSONArray().put(user))

        val outbound = JSONObject()
            .put("tag", "proxy")
            .put("protocol", "vless")
            .put("settings", JSONObject().put("vnext", JSONArray().put(vnext)))
            .put("streamSettings", JSONObject()
                .put("network", "ws")
                .put("security", "tls")
                .put("tlsSettings", JSONObject()
                    .put("serverName", server.sni)
                    .put("allowInsecure", false))
                .put("wsSettings", JSONObject()
                    .put("path", server.wsPath)
                    .put("headers", JSONObject().put("Host", server.host))))

        val direct = JSONObject().put("tag", "direct").put("protocol", "freedom")

        return JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("dns", JSONObject().put("servers", JSONArray().put("1.1.1.1")))
            .put("outbounds", JSONArray().put(outbound).put(direct))
            .put("routing", JSONObject()
                .put("domainStrategy", "IPIfNonMatch")
                .put("rules", JSONArray().put(JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray().put("geoip:private"))
                    .put("outboundTag", "direct"))))
            .toString(2)
    }
}
