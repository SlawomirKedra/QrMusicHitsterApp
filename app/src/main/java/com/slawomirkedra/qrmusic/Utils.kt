
package com.slawomirkedra.qrmusic

data class Parsed(val type:String, val subtype:String?=null, val id:String?=null, val raw:String)

fun parseLink(raw:String): Parsed {
    val t = raw.trim()
    return try {
        if (t.startsWith("spotify:")) {
            val parts = t.split(":")
            if (parts.size>=3) return Parsed("spotify", parts[1], parts[2], raw=t)
        }
        if (t.startsWith("http")) {
            val url = java.net.URL(t)
            val host = url.host
            if (host.contains("spotify.com")) {
                val seg = url.path.trim('/').split("/")
                if (seg.size>=2) return Parsed("spotify", seg[0], seg[1], raw=t)
            }
        }
        Parsed("unknown", raw=t)
    } catch (e:Throwable) {
        Parsed("unknown", raw=t)
    }
}
