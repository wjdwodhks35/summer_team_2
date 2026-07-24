package com.example.whentoleave.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MapFragment : Fragment() {

    companion object {
        private const val TAG = "MapFragment"
        private const val TMAP_KEY = "rVhf1vi1M79depsoEfcTa6qw8vfB1yPxcWqQHtTj"
        private const val ODSAY_KEY = "flAySXBVOkP+HdOWTcDH/I9LRvbn4Wp5i28piQBuvPo"

        // 서울 지하철 표준 색상 (호선 번호 → hex)
        private val SUBWAY_COLORS = mapOf(
            1 to "#0052A4", 2 to "#009246", 3 to "#EF7C1C",
            4 to "#00A5DE", 5 to "#996CAC", 6 to "#CD7C2F",
            7 to "#747F00", 8 to "#E6186C", 9 to "#BDB092"
        )
    }

    private var webView: WebView? = null

    inner class MapBridge {
        @JavascriptInterface
        fun onMapReady() {
            Log.d(TAG, "Leaflet 지도 준비 완료")
            activity?.runOnUiThread { startRouting() }
        }
    }

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        webView = WebView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            with(settings) {
                javaScriptEnabled    = true
                domStorageEnabled    = true
                useWideViewPort      = true
                loadWithOverviewMode = true
                cacheMode            = WebSettings.LOAD_DEFAULT
            }
            addJavascriptInterface(MapBridge(), "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    Log.d("$TAG/JS", "[${msg.messageLevel()}] ${msg.message()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?, errorCode: Int, description: String?, failingUrl: String?
                ) { Log.e(TAG, "WebView 오류 $errorCode: $description") }
            }
            loadDataWithBaseURL(
                "https://unpkg.com",
                buildMapHtml(),
                "text/html", "UTF-8", null
            )
        }
        return webView!!
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    // ──────────────────────────────────────────────
    // HTML (Leaflet.js)
    // ──────────────────────────────────────────────

    private fun buildMapHtml(): String = """
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
#legend {
    position:absolute; bottom:24px; left:12px; z-index:1000;
    background:rgba(255,255,255,0.93); border-radius:10px;
    padding:10px 14px; box-shadow:0 2px 8px rgba(0,0,0,0.18);
    display:none; min-width:110px;
}
#legend-title {
    font-size:11px; font-weight:bold; color:#555; margin-bottom:6px;
}
</style>
</head>
<body>
<div id="map"></div>
<div id="legend">
    <div id="legend-title">경로 안내</div>
</div>
<script>
var map = L.map('map', { zoomControl: true }).setView([37.5666, 126.9782], 13);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 19
}).addTo(map);

// 지도 준비 완료 → Android 콜백
map.whenReady(function() {
    console.log("Leaflet 지도 준비 완료");
    Android.onMapReady();
});

function initMap(startLat, startLon, endLat, endLon) {
    // 출발 마커 (파랑)
    L.circleMarker([startLat, startLon], {
        radius: 8, color: '#fff', weight: 2,
        fillColor: '#1e64ff', fillOpacity: 1
    }).addTo(map).bindPopup('출발');

    // 도착 마커 (빨강)
    L.circleMarker([endLat, endLon], {
        radius: 8, color: '#fff', weight: 2,
        fillColor: '#ff4040', fillOpacity: 1
    }).addTo(map).bindPopup('도착');

    // 출발·도착이 모두 보이도록 fitBounds
    map.fitBounds([[startLat, startLon], [endLat, endLon]], { padding: [50, 50] });
}

function drawSegments(segmentsJson) {
    try {
        var segs = JSON.parse(segmentsJson);
        segs.forEach(function(seg) {
            L.polyline(seg.points, {
                color  : seg.color,
                weight : seg.width,
                opacity: 0.85,
                lineJoin: 'round',
                lineCap : 'round'
            }).addTo(map);
        });
        console.log("drawSegments 완료: " + segs.length + "개");
    } catch(e) {
        console.error("drawSegments 오류: " + e.message);
    }
}

function drawLegend(legendJson) {
    try {
        var items = JSON.parse(legendJson);
        var legend = document.getElementById('legend');
        var title = document.getElementById('legend-title');
        legend.innerHTML = '';
        legend.appendChild(title);
        items.forEach(function(item) {
            var row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:center;margin:4px 0';
            var bar = document.createElement('div');
            bar.style.cssText = 'width:26px;height:5px;background:' + item.color
                + ';border-radius:3px;margin-right:8px;flex-shrink:0';
            var txt = document.createElement('span');
            txt.textContent = item.label;
            txt.style.cssText = 'font-size:12px;color:#222';
            row.appendChild(bar); row.appendChild(txt);
            legend.appendChild(row);
        });
        legend.style.display = items.length > 0 ? 'block' : 'none';
    } catch(e) {
        console.error("drawLegend 오류: " + e.message);
    }
}

// 탑승 지점 마커 (버스 번호 / 지하철 호선 표시)
function drawBoardingMarkers(markersJson) {
    try {
        var markers = JSON.parse(markersJson);
        markers.forEach(function(m) {
            var icon = L.divIcon({
                html: '<div style="'
                    + 'background:' + m.color + ';'
                    + 'color:white;font-size:10px;font-weight:bold;'
                    + 'padding:3px 7px;border-radius:12px;'
                    + 'white-space:nowrap;border:2px solid white;'
                    + 'box-shadow:0 2px 5px rgba(0,0,0,0.4);'
                    + 'cursor:default'
                    + '">' + m.label + '</div>',
                iconSize: null,
                className: ''
            });
            L.marker([m.lat, m.lon], { icon: icon })
                .addTo(map)
                .bindTooltip(m.tooltip || m.label, { direction: 'top', offset: [0, -8] });
        });
    } catch(e) {
        console.error("drawBoardingMarkers 오류: " + e.message);
    }
}
</script>
</body>
</html>
""".trimIndent()

    // ──────────────────────────────────────────────
    // 경로 시작
    // ──────────────────────────────────────────────

    private fun startRouting() {
        if (!isAdded || view == null) return

        val transportMode = arguments?.getString("transportMode") ?: "transit"
        val startAddr     = arguments?.getString("startAddress") ?: ""
        val endAddr       = arguments?.getString("endAddress") ?: ""
        val argStartLat   = arguments?.getDouble("startLat", 0.0) ?: 0.0
        val argStartLon   = arguments?.getDouble("startLon", 0.0) ?: 0.0
        val argEndLat     = arguments?.getDouble("endLat", 0.0) ?: 0.0
        val argEndLon     = arguments?.getDouble("endLon", 0.0) ?: 0.0

        viewLifecycleOwner.lifecycleScope.launch {
            val (startLat, startLon) =
                if (argStartLat != 0.0 && argStartLon != 0.0) Pair(argStartLat, argStartLon)
                else geocodeAddress(startAddr) ?: run { Log.e(TAG, "출발지 geocoding 실패"); return@launch }

            val (endLat, endLon) =
                if (argEndLat != 0.0 && argEndLon != 0.0) Pair(argEndLat, argEndLon)
                else geocodeAddress(endAddr) ?: run { Log.e(TAG, "도착지 geocoding 실패"); return@launch }

            Log.d(TAG, "출발: $startLat,$startLon  도착: $endLat,$endLon")

            withContext(Dispatchers.Main) {
                jsEval("initMap($startLat, $startLon, $endLat, $endLon)")
            }

            when (transportMode) {
                "transit" -> drawTransitRoute(startLat, startLon, endLat, endLon)
                else      -> drawCarWalkRoute(startLat, startLon, endLat, endLon, transportMode)
            }
        }
    }

    // ──────────────────────────────────────────────
    // 경로 fetch
    // ──────────────────────────────────────────────

    private suspend fun drawCarWalkRoute(
        startLat: Double, startLon: Double,
        endLat: Double,   endLon: Double,
        mode: String
    ) {
        val pts: List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
            try {
                val apiUrl = if (mode == "walk")
                    "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1"
                else
                    "https://apis.openapi.sk.com/tmap/routes?version=1"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                conn.setRequestProperty("appKey", TMAP_KEY)
                conn.setRequestProperty("Content-Type", "application/json")
                val body = """{"startX":"$startLon","startY":"$startLat","endX":"$endLon","endY":"$endLat","reqCoordType":"WGS84GEO","resCoordType":"WGS84GEO","startName":"출발지","endName":"도착지"}"""
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode != 200) { Log.e(TAG, "T맵 HTTP ${conn.responseCode}"); return@withContext emptyList() }
                val features = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                    .optJSONArray("features") ?: return@withContext emptyList()
                val list = mutableListOf<Pair<Double, Double>>()
                for (k in 0 until features.length()) {
                    val geom = features.getJSONObject(k).optJSONObject("geometry") ?: continue
                    if (geom.optString("type") != "LineString") continue
                    val coords = geom.optJSONArray("coordinates") ?: continue
                    for (m in 0 until coords.length()) {
                        val pt = coords.getJSONArray(m)
                        list.add(Pair(pt.optDouble(1), pt.optDouble(0)))
                    }
                }
                list
            } catch (e: Exception) { Log.e(TAG, "경로 오류: ${e.message}"); emptyList() }
        }
        if (pts.size < 2) return
        val color = if (mode == "walk") "#00a000" else "#1e64ff"
        val arr = JSONArray().put(buildSegmentJson(pts, color, 5))
        withContext(Dispatchers.Main) { jsEval("drawSegments('${escape(arr.toString())}')")  }
    }

    private suspend fun drawTransitRoute(
        startLat: Double, startLon: Double,
        endLat: Double,   endLon: Double
    ) {
        data class Seg(val type: Int, val pts: List<Pair<Double, Double>>,
                       val color: String, val label: String)

        val segments: List<Seg> = withContext(Dispatchers.IO) {
            try {
                val encodedKey = URLEncoder.encode(ODSAY_KEY, "UTF-8")
                val url = "https://api.odsay.com/v1/api/searchPubTransPathT" +
                        "?SX=$startLon&SY=$startLat&EX=$endLon&EY=$endLat&apiKey=$encodedKey"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000; conn.readTimeout = 10000
                if (conn.responseCode != 200) { Log.e(TAG, "ODsay HTTP ${conn.responseCode}"); return@withContext emptyList() }
                val body     = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                val subPaths = JSONObject(body)
                    .optJSONObject("result")
                    ?.optJSONArray("path")?.optJSONObject(0)
                    ?.optJSONArray("subPath")
                    ?: run { Log.e(TAG, "ODsay subPath 없음"); return@withContext emptyList() }

                val list = mutableListOf<Seg>()
                for (i in 0 until subPaths.length()) {
                    val sp   = subPaths.getJSONObject(i)
                    val type = sp.optInt("trafficType", 3)

                    // ── 호선/버스 정보 파싱 ──
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
                    Log.d(TAG, "구간[$i] type=$type lineNum=$lineNum busNo=$busNo color=$color")

                    // ── 좌표 파싱 ──
                    val pts  = mutableListOf<Pair<Double, Double>>()
                    val ls   = sp.optJSONObject("passShape")?.optString("linestring") ?: ""
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
                            if (lo1 != null && la1 != null && lo2 != null && la2 != null) {
                                pts.add(Pair(la1, lo1)); pts.add(Pair(la2, lo2))
                            }
                        }
                    }
                    if (pts.size >= 2) list.add(Seg(type, pts, color, label))
                }
                list
            } catch (e: Exception) { Log.e(TAG, "ODsay 오류: ${e.message}"); emptyList() }
        }
        if (segments.isEmpty()) return

        // 폴리라인 배열
        val arr = JSONArray()
        for (seg in segments) {
            val width = if (seg.type == 3) 3 else 6
            arr.put(buildSegmentJson(seg.pts, seg.color, width))
        }

        // 범례 배열 (도보 제외, 중복 제외)
        val legendArr = JSONArray()
        val seen = mutableSetOf<String>()
        for (seg in segments) {
            if (seg.type == 3) continue
            if (seen.contains(seg.label)) continue
            seen.add(seg.label)
            legendArr.put(JSONObject().put("label", seg.label).put("color", seg.color))
        }

        // 탑승 지점 마커 (버스/지하철 노선 번호를 지도에 표시)
        val boardingArr = JSONArray()
        for (seg in segments) {
            if (seg.type == 3) continue   // 도보 제외
            if (seg.pts.isEmpty()) continue
            val (lat, lon) = seg.pts.first()
            val tooltip = when (seg.type) {
                1 -> "${seg.label} 탑승"
                2 -> "${seg.label}번 버스 탑승"
                else -> seg.label
            }
            boardingArr.put(
                JSONObject()
                    .put("lat", lat).put("lon", lon)
                    .put("label", seg.label)
                    .put("color", seg.color)
                    .put("tooltip", tooltip)
            )
        }

        withContext(Dispatchers.Main) {
            jsEval("drawSegments('${escape(arr.toString())}')")
            jsEval("drawLegend('${escape(legendArr.toString())}')")
            if (boardingArr.length() > 0) {
                jsEval("drawBoardingMarkers('${escape(boardingArr.toString())}')")
            }
        }
    }

    // ──────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────

    private fun buildSegmentJson(pts: List<Pair<Double, Double>>, color: String, width: Int): JSONObject {
        val ptArr = JSONArray()
        for ((la, lo) in pts) ptArr.put(JSONArray().put(la).put(lo))
        return JSONObject().put("points", ptArr).put("color", color).put("width", width)
    }

    /** JS 문자열 안에 넣을 때 작은따옴표 이스케이프 */
    private fun escape(s: String) = s.replace("'", "\\'")

    private fun jsEval(script: String) {
        webView?.post { webView?.evaluateJavascript(script, null) }
    }

    // ──────────────────────────────────────────────
    // Geocoding (T맵 POI REST API)
    // ──────────────────────────────────────────────

    private suspend fun geocodeAddress(address: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(address, "UTF-8")
                val poiConn = URL("https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword=$encoded&count=1")
                    .openConnection() as HttpURLConnection
                poiConn.setRequestProperty("appKey", TMAP_KEY)
                poiConn.connectTimeout = 5000; poiConn.readTimeout = 5000
                if (poiConn.responseCode == 200) {
                    val body = poiConn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    val pois = JSONObject(body).optJSONObject("searchPoiInfo")
                        ?.optJSONObject("pois")?.optJSONArray("poi")
                    if (pois != null && pois.length() > 0) {
                        val lat = pois.getJSONObject(0).optString("noorLat").toDoubleOrNull()
                        val lon = pois.getJSONObject(0).optString("noorLon").toDoubleOrNull()
                        if (lat != null && lon != null && lat != 0.0) return@withContext Pair(lat, lon)
                    }
                }
                val geoConn = URL("https://apis.openapi.sk.com/tmap/geo/fullAddrGeo?version=1&format=json&fullAddr=$encoded")
                    .openConnection() as HttpURLConnection
                geoConn.setRequestProperty("appKey", TMAP_KEY)
                geoConn.connectTimeout = 5000; geoConn.readTimeout = 5000
                if (geoConn.responseCode == 200) {
                    val body = geoConn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    val info = JSONObject(body).optJSONObject("coordinateInfo")
                    val lat  = info?.optString("lat")?.toDoubleOrNull()
                    val lon  = info?.optString("lon")?.toDoubleOrNull()
                    if (lat != null && lon != null && lat != 0.0) return@withContext Pair(lat, lon)
                }
                null
            } catch (e: Exception) { Log.e(TAG, "geocoding 오류: ${e.message}"); null }
        }
}