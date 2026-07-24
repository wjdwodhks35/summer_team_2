package com.example.whentoleave.ui.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.whentoleave.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 약속방 지도 — 각 참여자의 실제 대중교통 경로를 Leaflet.js로 표시
 *
 * Intent extras:
 *   "roomName"  String
 *   "destLat"   Double
 *   "destLon"   Double
 *   "destName"  String
 *   "members"   String  JSON array: [{name, lat, lon, isHost}]
 */
class RoomMapActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RoomMapActivity"
        private const val TMAP_KEY  = "rVhf1vi1M79depsoEfcTa6qw8vfB1yPxcWqQHtTj"
        private const val ODSAY_KEY = "flAySXBVOkP+HdOWTcDH/I9LRvbn4Wp5i28piQBuvPo"

        private val MEMBER_COLORS = listOf(
            "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6",
            "#EC4899", "#06B6D4", "#F97316", "#14B8A6"
        )
        private val SUBWAY_COLORS = mapOf(
            1 to "#0052A4", 2 to "#009246", 3 to "#EF7C1C",
            4 to "#00A5DE", 5 to "#996CAC", 6 to "#CD7C2F",
            7 to "#747F00", 8 to "#E6186C", 9 to "#BDB092"
        )
    }

    private var webView: WebView? = null
    private var destLat = 0.0
    private var destLon = 0.0
    private var destName = "목적지"
    private var membersJson = "[]"

    data class Member(val name: String, val lat: Double, val lon: Double, val isHost: Boolean)
    data class RouteSegment(
        val type: Int,
        val pts: List<Pair<Double, Double>>,
        val color: String,
        val label: String
    )

    inner class MapBridge {
        @JavascriptInterface
        fun onMapReady() {
            Log.d(TAG, "지도 준비 완료 → 경로 로딩 시작")
            runOnUiThread { startDrawingRoutes() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_map)

        val roomName = intent.getStringExtra("roomName") ?: "약속방"
        destLat      = intent.getDoubleExtra("destLat", 0.0)
        destLon      = intent.getDoubleExtra("destLon", 0.0)
        destName     = intent.getStringExtra("destName") ?: "목적지"
        membersJson  = intent.getStringExtra("members") ?: "[]"

        supportActionBar?.title = "🗺 $roomName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val centerLat = if (destLat != 0.0) destLat else 37.5665
        val centerLon = if (destLon != 0.0) destLon else 126.9780

        webView = findViewById<WebView>(R.id.webview_map).apply {
            settings.apply {
                javaScriptEnabled    = true
                domStorageEnabled    = true
                useWideViewPort      = true
                loadWithOverviewMode = true
                cacheMode            = WebSettings.LOAD_DEFAULT
                mixedContentMode     = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            addJavascriptInterface(MapBridge(), "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    Log.d("$TAG/JS", "[${msg.messageLevel()}] ${msg.message()}")
                    return true
                }
            }
            webViewClient = WebViewClient()
            loadDataWithBaseURL(
                "https://unpkg.com",
                buildMapHtml(centerLat, centerLon),
                "text/html", "UTF-8", null
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        webView?.destroy(); webView = null
        super.onDestroy()
    }

    // ── 경로 그리기 시작 ──────────────────────────────────────────────────
    private fun startDrawingRoutes() {
        val members = parseMembers()
        if (members.isEmpty()) {
            jsEval("showStatus('출발지를 설정한 참여자가 없습니다')")
            return
        }

        if (destLat != 0.0 && destLon != 0.0) {
            jsEval("addDestMarker($destLat, $destLon, '${escape(destName)}')")
        }

        val legendArr = JSONArray()
        members.forEachIndexed { i, m ->
            val color = MEMBER_COLORS[i % MEMBER_COLORS.size]
            legendArr.put(JSONObject().put("label", if (m.isHost) "${m.name} (방장)" else m.name).put("color", color))
        }
        jsEval("drawMemberLegend('${escape(legendArr.toString())}')")

        lifecycleScope.launch {
            members.forEachIndexed { i, member ->
                val color = MEMBER_COLORS[i % MEMBER_COLORS.size]
                val label = if (member.isHost) "${member.name} (방장)" else member.name

                withContext(Dispatchers.Main) {
                    jsEval("showStatus('${escape(label)} 경로 로딩 중... (${i + 1}/${members.size})')")
                    jsEval("addMemberMarker(${member.lat}, ${member.lon}, '${escape(label)}', '$color')")
                }

                if (destLat != 0.0 && destLon != 0.0) {
                    val segments = fetchTransitRoute(member.lat, member.lon, destLat, destLon)
                    if (segments.isNotEmpty()) {
                        drawMemberRoute(segments)
                    } else {
                        withContext(Dispatchers.Main) {
                            jsEval("drawFallbackLine(${member.lat}, ${member.lon}, $destLat, $destLon, '$color')")
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                jsEval("hideStatus()")
                jsEval("fitAllBounds()")
            }
        }
    }

    // ── 멤버 JSON 파싱 ────────────────────────────────────────────────────
    private fun parseMembers(): List<Member> {
        val list = mutableListOf<Member>()
        try {
            val arr = JSONArray(membersJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lat = obj.optDouble("lat", 0.0)
                val lon = obj.optDouble("lon", 0.0)
                if (lat == 0.0 || lon == 0.0) continue
                list.add(Member(
                    name   = obj.optString("name", "참여자"),
                    lat    = lat,
                    lon    = lon,
                    isHost = obj.optBoolean("isHost", false)
                ))
            }
        } catch (e: Exception) { Log.e(TAG, "멤버 파싱 오류: ${e.message}") }
        return list
    }

    // ── ODSAY 대중교통 경로 조회 ──────────────────────────────────────────
    private suspend fun fetchTransitRoute(
        startLat: Double, startLon: Double,
        endLat: Double,   endLon: Double
    ): List<RouteSegment> = withContext(Dispatchers.IO) {
        try {
            val encodedKey = URLEncoder.encode(ODSAY_KEY, "UTF-8")
            val url = "https://api.odsay.com/v1/api/searchPubTransPathT" +
                    "?SX=$startLon&SY=$startLat&EX=$endLon&EY=$endLat&apiKey=$encodedKey"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000; conn.readTimeout = 12000

            if (conn.responseCode != 200) {
                Log.e(TAG, "ODSAY HTTP ${conn.responseCode}")
                return@withContext emptyList()
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val subPaths = JSONObject(body)
                .optJSONObject("result")
                ?.optJSONArray("path")?.optJSONObject(0)
                ?.optJSONArray("subPath")
                ?: return@withContext emptyList()

            val list = mutableListOf<RouteSegment>()
            for (i in 0 until subPaths.length()) {
                val sp   = subPaths.getJSONObject(i)
                val type = sp.optInt("trafficType", 3)

                val laneObj    = sp.optJSONArray("lane")?.optJSONObject(0)
                val subwayCode = laneObj?.optInt("subwayCode", 0) ?: 0
                val laneName   = laneObj?.optString("name", "") ?: ""
                val busNo      = laneObj?.optString("busNo", "") ?: ""

                val lineNum = subwayCode.takeIf { it > 0 }
                    ?: Regex("(\\d+)호선").find(laneName)?.groupValues?.get(1)?.toIntOrNull()
                    ?: 0

                val color = when (type) {
                    1 -> SUBWAY_COLORS[lineNum] ?: "#1e64ff"
                    2 -> "#ff8200"
                    else -> "#909090"
                }
                val label = when (type) {
                    1 -> if (lineNum > 0) "${lineNum}호선" else laneName.ifEmpty { "지하철" }
                    2 -> if (busNo.isNotEmpty()) "${busNo}번" else "버스"
                    else -> "도보"
                }

                val pts = mutableListOf<Pair<Double, Double>>()
                val ls  = sp.optJSONObject("passShape")?.optString("linestring") ?: ""
                if (ls.isNotEmpty()) {
                    for (tok in ls.trim().split(" ")) {
                        val c = tok.split(",")
                        if (c.size >= 2) {
                            val lo = c[0].toDoubleOrNull() ?: continue
                            val la = c[1].toDoubleOrNull() ?: continue
                            pts.add(Pair(la, lo))
                        }
                    }
                } else {
                    val stations = sp.optJSONObject("passStopList")?.optJSONArray("stations")
                    if (stations != null) {
                        for (j in 0 until stations.length()) {
                            val st = stations.getJSONObject(j)
                            val lo = st.optString("x").toDoubleOrNull() ?: continue
                            val la = st.optString("y").toDoubleOrNull() ?: continue
                            pts.add(Pair(la, lo))
                        }
                    } else {
                        val lo1 = sp.optString("startX").toDoubleOrNull()
                        val la1 = sp.optString("startY").toDoubleOrNull()
                        val lo2 = sp.optString("endX").toDoubleOrNull()
                        val la2 = sp.optString("endY").toDoubleOrNull()
                        if (lo1 != null && la1 != null) pts.add(Pair(la1, lo1))
                        if (lo2 != null && la2 != null) pts.add(Pair(la2, lo2))
                    }
                }
                if (pts.size >= 2) list.add(RouteSegment(type, pts, color, label))
            }
            list
        } catch (e: Exception) { Log.e(TAG, "ODSAY 오류: ${e.message}"); emptyList() }
    }

    // ── 세그먼트를 JS로 전달 ──────────────────────────────────────────────
    private suspend fun drawMemberRoute(segments: List<RouteSegment>) {
        val segsArr     = JSONArray()
        val boardingArr = JSONArray()

        for (seg in segments) {
            val ptArr = JSONArray()
            for ((la, lo) in seg.pts) ptArr.put(JSONArray().put(la).put(lo))
            segsArr.put(
                JSONObject()
                    .put("points", ptArr)
                    .put("color", seg.color)
                    .put("width", if (seg.type == 3) 3 else 6)
            )
            if (seg.type != 3 && seg.pts.isNotEmpty()) {
                val (la, lo) = seg.pts.first()
                boardingArr.put(
                    JSONObject()
                        .put("lat", la).put("lon", lo)
                        .put("label", seg.label)
                        .put("color", seg.color)
                        .put("tooltip", when (seg.type) {
                            1 -> "${seg.label} 탑승"
                            2 -> "${seg.label}번 버스 탑승"
                            else -> seg.label
                        })
                )
            }
        }

        withContext(Dispatchers.Main) {
            jsEval("drawSegments('${escape(segsArr.toString())}')")
            if (boardingArr.length() > 0) {
                jsEval("drawBoardingMarkers('${escape(boardingArr.toString())}')")
            }
        }
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────
    private fun escape(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")

    private fun jsEval(script: String) {
        webView?.post { webView?.evaluateJavascript(script, null) }
    }

    // ── HTML ─────────────────────────────────────────────────────────────
    private fun buildMapHtml(centerLat: Double, centerLon: Double): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
html, body, #map { width:100%; height:100%; }
#status-bar {
    position:absolute; top:10px; left:50%; transform:translateX(-50%);
    z-index:1001; background:rgba(0,0,0,0.72); color:#fff;
    padding:8px 18px; border-radius:20px; font-size:13px;
    font-family:sans-serif; display:none; white-space:nowrap;
}
#member-legend {
    position:absolute; bottom:20px; left:10px; z-index:1000;
    background:rgba(255,255,255,0.95); border-radius:10px;
    padding:10px 14px; box-shadow:0 2px 8px rgba(0,0,0,0.18);
    font-family:sans-serif; font-size:12px; min-width:110px; display:none;
}
#member-legend-title { font-size:11px; font-weight:bold; color:#555; margin-bottom:6px; }
#transit-legend {
    position:absolute; bottom:20px; right:10px; z-index:1000;
    background:rgba(255,255,255,0.95); border-radius:10px;
    padding:10px 14px; box-shadow:0 2px 8px rgba(0,0,0,0.18);
    font-family:sans-serif; font-size:12px; display:none;
}
</style>
</head>
<body>
<div id="map"></div>
<div id="status-bar"></div>
<div id="member-legend"><div id="member-legend-title">참여자</div></div>
<div id="transit-legend"></div>
<script>
var map = L.map('map', {zoomControl:true}).setView([$centerLat, $centerLon], 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution:'© OpenStreetMap contributors', maxZoom:19
}).addTo(map);

var allBounds = [];
var drawnTransitLabels = {};

map.whenReady(function() { Android.onMapReady(); });

function addDestMarker(lat, lon, name) {
    var icon = L.divIcon({
        html: '<div style="background:#E53E3E;width:22px;height:22px;border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;color:white;font-weight:bold;font-size:10px;">목</div>',
        iconSize:[22,22], iconAnchor:[11,11], className:''
    });
    L.marker([lat, lon], {icon:icon}).addTo(map)
        .bindPopup('<b>🏁 ' + name + '</b><br>목적지', {maxWidth:200});
    allBounds.push([lat, lon]);
}

function addMemberMarker(lat, lon, name, color) {
    var icon = L.divIcon({
        html: '<div style="background:' + color + ';width:18px;height:18px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.4);"></div>',
        iconSize:[18,18], iconAnchor:[9,9], className:''
    });
    L.marker([lat, lon], {icon:icon}).addTo(map)
        .bindPopup('<b>' + name + '</b><br>출발지', {maxWidth:200});
    allBounds.push([lat, lon]);
}

function drawSegments(segmentsJson) {
    try {
        var segs = JSON.parse(segmentsJson);
        segs.forEach(function(seg) {
            L.polyline(seg.points, {
                color:seg.color, weight:seg.width, opacity:0.85,
                lineJoin:'round', lineCap:'round'
            }).addTo(map);
            seg.points.forEach(function(p) { allBounds.push(p); });
        });
    } catch(e) { console.error('drawSegments: ' + e.message); }
}

function drawFallbackLine(sLat, sLon, eLat, eLon, color) {
    L.polyline([[sLat, sLon],[eLat, eLon]], {
        color:color, weight:3, opacity:0.55, dashArray:'8,6'
    }).addTo(map);
    allBounds.push([sLat, sLon]);
}

function drawBoardingMarkers(markersJson) {
    try {
        JSON.parse(markersJson).forEach(function(m) {
            var icon = L.divIcon({
                html: '<div style="background:' + m.color + ';color:white;font-size:10px;font-weight:bold;padding:3px 7px;border-radius:12px;white-space:nowrap;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.4);">' + m.label + '</div>',
                iconSize:null, className:''
            });
            L.marker([m.lat, m.lon], {icon:icon}).addTo(map)
                .bindTooltip(m.tooltip || m.label, {direction:'top', offset:[0,-8]});

            if (!drawnTransitLabels[m.label]) {
                drawnTransitLabels[m.label] = true;
                var tl = document.getElementById('transit-legend');
                tl.style.display = 'block';
                var row = document.createElement('div');
                row.style.cssText = 'display:flex;align-items:center;margin:4px 0';
                row.innerHTML = '<div style="width:24px;height:5px;background:' + m.color + ';border-radius:3px;margin-right:8px;flex-shrink:0;"></div><span>' + m.label + '</span>';
                tl.appendChild(row);
            }
        });
    } catch(e) { console.error('drawBoardingMarkers: ' + e.message); }
}

function drawMemberLegend(membersJson) {
    try {
        var members = JSON.parse(membersJson);
        var legend = document.getElementById('member-legend');
        var title  = document.getElementById('member-legend-title');
        legend.innerHTML = '';
        legend.appendChild(title);
        members.forEach(function(m) {
            var row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:center;margin:4px 0';
            row.innerHTML = '<div style="width:12px;height:12px;border-radius:50%;background:' + m.color + ';margin-right:8px;flex-shrink:0;border:2px solid white;box-shadow:0 1px 3px rgba(0,0,0,0.3);"></div><span>' + m.label + '</span>';
            legend.appendChild(row);
        });
        if (members.length > 0) legend.style.display = 'block';
    } catch(e) {}
}

function showStatus(msg) {
    var el = document.getElementById('status-bar');
    el.textContent = msg; el.style.display = 'block';
}
function hideStatus() {
    document.getElementById('status-bar').style.display = 'none';
}
function fitAllBounds() {
    if (allBounds.length > 0) {
        try { map.fitBounds(allBounds, {padding:[40,40]}); } catch(e) {}
    }
}
</script>
</body>
</html>
""".trimIndent()
}
