package com.example.whentoleave.ui.home

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.whentoleave.R
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.data.api.TripApiService
import com.example.whentoleave.data.model.TripRequest
import com.example.whentoleave.data.model.TripResponse
import com.example.whentoleave.databinding.FragmentResultBinding
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class ResultFragment : Fragment() {

    companion object {
        private const val TAG        = "ResultFragment"
        private const val TMAP_KEY   = "rVhf1vi1M79depsoEfcTa6qw8vfB1yPxcWqQHtTj"
        private const val ODSAY_KEY  = "flAySXBVOkP+HdOWTcDH/I9LRvbn4Wp5i28piQBuvPo"
        private const val PUBLIC_KEY = "2dc8847e3d266a050537367834b26158359b1f24b1b1fda048fe7f2424e63273"
        private const val SEOUL_KEY  = "524a6371487265653739616d4a5559"
    }

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val stackColors = listOf(
        "#2D6A6A", "#7C3AED", "#FF6B57", "#3DDC97", "#FFB627", "#FF8C42", "#1E40AF"
    )
    private val stackLabels = listOf(
        "기본 이동", "대기", "혼잡", "날씨", "교통", "목적", "개인"
    )

    // 경로 상 모든 지하철 구간 (호선번호, 탑승역)
    private val subwaySegments = mutableListOf<Pair<Int, String>>()

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startAddress = arguments?.getString("startAddress") ?: ""
        val endAddress   = arguments?.getString("endAddress")   ?: ""
        val targetDate   = arguments?.getString("targetDate")   ?: ""
        val targetTime   = arguments?.getString("targetTime")   ?: ""
        val purpose      = arguments?.getString("purpose")      ?: "GENERAL"
        val argStartLat  = arguments?.getDouble("startLat")     ?: 0.0
        val argStartLon  = arguments?.getDouble("startLon")     ?: 0.0
        val timeType     = arguments?.getString("timeType")     ?: "ARRIVAL"

        binding.tvRouteSummary.text = "$startAddress → $endAddress"

        // 버튼 (좌표 resolve 후 갱신)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnDetail.setOnClickListener { showCongestionDetail() }

        // 로딩 초기값
        binding.tvDepartTime.text    = "--:--"
        binding.tvArriveTime.text    = "--:--"
        binding.tvTransportInfo.text = "조회 중..."
        binding.tvSeatProb.text      = "-"
        binding.tvRainTime.text      = "-"
        binding.tvTravelTime.text    = "-"

        viewLifecycleOwner.lifecycleScope.launch {
            // ① 좌표 resolve (없으면 T맵으로 geocoding)
            val (startLat, startLon) =
                if (argStartLat != 0.0 && argStartLon != 0.0) Pair(argStartLat, argStartLon)
                else geocodeByAddress(startAddress) ?: Pair(0.0, 0.0)

            val (endLat, endLon) = geocodeByAddress(endAddress) ?: Pair(0.0, 0.0)

            Log.d(TAG, "출발좌표: $startLat,$startLon  도착좌표: $endLat,$endLon")

            // ② 지도 버튼 — resolve된 좌표로 세팅
            withContext(Dispatchers.Main) {
                binding.btnViewMap.setOnClickListener {
                    findNavController().navigate(R.id.action_result_to_map, Bundle().apply {
                        putString("transportMode", "transit")
                        putString("startAddress", startAddress)
                        putString("endAddress",   endAddress)
                        putDouble("startLat", startLat); putDouble("startLon", startLon)
                        putDouble("endLat",   endLat);   putDouble("endLon",   endLon)
                    })
                }
            }

            // ③ 병렬 API 호출
            val backendDef = async { fetchBackend(startAddress, endAddress, targetDate, targetTime, purpose) }
            val odsayDef   = async { fetchOdsay(startLat, startLon, endLat, endLon) }
            val weatherDef = async { fetchWeather(endLat, endLon, targetDate, targetTime) }

            val backend = backendDef.await()
            val odsay   = odsayDef.await()
            val weather = weatherDef.await()

            withContext(Dispatchers.Main) {
                applyUi(backend, odsay, weather, targetTime, targetDate, timeType)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ──────────────────────────────────────────────
    // UI 조합
    // ──────────────────────────────────────────────

    private fun applyUi(
        backend: TripResponse?,
        odsay: OdsayResult?,
        weather: WeatherResult,
        targetTime: String,
        targetDate: String,
        timeType: String = "ARRIVAL"
    ) {
        binding.tvTransportInfo.text = odsay?.routeLabel ?: "대중교통"
        binding.tvSeatProb.text = "${estimateSeatProb(targetTime, targetDate)}%"
        binding.tvRainTime.text = if (weather.hasRain) weather.description else "없음"

        when {
            backend != null -> {
                binding.tvDepartTime.text = backend.recommendedDepartureTime.substringAfter("T").take(5)
                binding.tvArriveTime.text = backend.expectedArrivalTime.substringAfter("T").take(5)
                val total = backend.baseTravelMinutes + backend.totalBufferMinutes
                binding.tvTravelTime.text = "${total}분"
                val vals = listOf(
                    backend.baseTravelMinutes, 0, 0,
                    if (weather.hasRain) 5 else 0, 0,
                    backend.purposeBufferMinutes, backend.personalBufferMinutes
                )
                drawStackBar(vals, total); drawLegend(vals)
                val parts = mutableListOf<String>()
                if (!backend.summary.isNullOrBlank()) parts.add(backend.summary)
                if (weather.hasRain) parts.add("출발 시간대에 ${weather.description} 예보가 있어 여유 시간을 추가했습니다.")
                binding.tvRecommendationSummary.text =
                    parts.joinToString("\n").ifBlank { "추천 시간에 출발하면 혼잡 구간을 피할 수 있습니다." }
            }
            odsay != null -> {
                val weatherBuf  = if (weather.hasRain) 5 else 0
                val purposeBuf  = 3
                val personalBuf = maxOf(0, 10 - weatherBuf - purposeBuf)
                val total = odsay.totalTime + weatherBuf + purposeBuf + personalBuf
                if (timeType == "DEPARTURE") {
                    binding.tvDepartTime.text = targetTime.take(5)
                    binding.tvArriveTime.text = calcArriveTime(targetTime, total)
                } else {
                    binding.tvDepartTime.text = calcDepartTime(targetTime, total)
                    binding.tvArriveTime.text = targetTime.take(5)
                }
                binding.tvTravelTime.text = "${total}분"   // 버퍼 포함 총 시간
                // vals 합 = total 이 되도록 구성 (0은 drawStackBar에서 제외됨)
                val vals = listOf(odsay.totalTime, 0, 0, weatherBuf, 0, purposeBuf, personalBuf)
                drawStackBar(vals, total); drawLegend(vals)
                binding.tvRecommendationSummary.text =
                    if (weather.hasRain) "출발 시간대에 ${weather.description} 예보가 있습니다. 여유 시간을 추가했습니다."
                    else "대중교통 이동시간 ${odsay.totalTime}분 + 여유 ${weatherBuf + purposeBuf + personalBuf}분을 반영했습니다."
            }
            else -> {
                if (timeType == "DEPARTURE") {
                    binding.tvDepartTime.text = targetTime.take(5)
                    binding.tvArriveTime.text = calcArriveTime(targetTime, 46)
                } else {
                    binding.tvDepartTime.text = calcDepartTime(targetTime, 46)
                    binding.tvArriveTime.text = targetTime.take(5)
                }
                binding.tvTravelTime.text = "46분"
                val vals = listOf(30, 5, 5, 0, 3, 2, 1)
                drawStackBar(vals, 46); drawLegend(vals)
                binding.tvRecommendationSummary.text = "서버/네트워크 연결 오류 — 기본값을 표시합니다."
            }
        }
    }

    // ──────────────────────────────────────────────
    // Geocoding (T맵 POI REST)
    // ──────────────────────────────────────────────

    private suspend fun geocodeByAddress(address: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            if (address.isBlank() || address == "현재 위치") return@withContext null
            try {
                val encoded = URLEncoder.encode(address, "UTF-8")
                // POI 검색
                val poiConn = URL("https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword=$encoded&count=1")
                    .openConnection() as HttpURLConnection
                poiConn.setRequestProperty("appKey", TMAP_KEY)
                poiConn.connectTimeout = 5000; poiConn.readTimeout = 5000
                if (poiConn.responseCode == 200) {
                    val pois = JSONObject(poiConn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                        .optJSONObject("searchPoiInfo")?.optJSONObject("pois")?.optJSONArray("poi")
                    if (pois != null && pois.length() > 0) {
                        val lat = pois.getJSONObject(0).optString("noorLat").toDoubleOrNull()
                        val lon = pois.getJSONObject(0).optString("noorLon").toDoubleOrNull()
                        if (lat != null && lon != null && lat != 0.0)
                            return@withContext Pair(lat, lon)
                    }
                }
                // 주소 geocoding fallback
                val geoConn = URL("https://apis.openapi.sk.com/tmap/geo/fullAddrGeo?version=1&format=json&fullAddr=$encoded")
                    .openConnection() as HttpURLConnection
                geoConn.setRequestProperty("appKey", TMAP_KEY)
                geoConn.connectTimeout = 5000; geoConn.readTimeout = 5000
                if (geoConn.responseCode == 200) {
                    val info = JSONObject(geoConn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                        .optJSONObject("coordinateInfo")
                    val lat = info?.optString("lat")?.toDoubleOrNull()
                    val lon = info?.optString("lon")?.toDoubleOrNull()
                    if (lat != null && lon != null && lat != 0.0)
                        return@withContext Pair(lat, lon)
                }
                null
            } catch (e: Exception) { Log.e(TAG, "geocoding ${address}: ${e.message}"); null }
        }

    // ──────────────────────────────────────────────
    // 백엔드
    // ──────────────────────────────────────────────

    private suspend fun fetchBackend(
        start: String, end: String,
        date: String, time: String, purpose: String
    ): TripResponse? = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.instance.create(TripApiService::class.java)
            val resp = api.calculateTrip(
                TripRequest(
                    startAddress       = start,
                    destinationAddress = end,
                    targetDateTime     = "${date}T${time}:00",
                    purposeCode        = purpose
                )
            )
            if (resp.isSuccessful) resp.body()
            else { Log.e(TAG, "백엔드 ${resp.code()}"); null }
        } catch (e: Exception) { Log.e(TAG, "백엔드 오류: ${e.message}"); null }
    }

    // ──────────────────────────────────────────────
    // ODsay
    // ──────────────────────────────────────────────

    data class OdsayResult(val routeLabel: String, val totalTime: Int,
                           val line: Int, val lineName: String, val station: String)

    private suspend fun fetchOdsay(
        sLat: Double, sLon: Double, eLat: Double, eLon: Double
    ): OdsayResult? = withContext(Dispatchers.IO) {
        if (sLat == 0.0 || sLon == 0.0 || eLat == 0.0 || eLon == 0.0) {
            Log.w(TAG, "ODsay: 좌표 없음 (sLat=$sLat eLat=$eLat)")
            return@withContext null
        }
        try {
            val encodedKey = URLEncoder.encode(ODSAY_KEY, "UTF-8")
            val conn = URL("https://api.odsay.com/v1/api/searchPubTransPathT?SX=$sLon&SY=$sLat&EX=$eLon&EY=$eLat&apiKey=$encodedKey")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            val code = conn.responseCode
            if (code != 200) { Log.e(TAG, "ODsay HTTP $code"); return@withContext null }

            val body   = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            Log.d(TAG, "ODsay 응답: ${body.take(200)}")
            val result = JSONObject(body).optJSONObject("result") ?: run {
                Log.e(TAG, "ODsay result 없음: $body"); return@withContext null
            }
            val path0  = result.optJSONArray("path")?.optJSONObject(0) ?: return@withContext null
            val total  = path0.optJSONObject("info")?.optInt("totalTime", 0) ?: 0
            val subs   = path0.optJSONArray("subPath") ?: return@withContext null

            val parts = mutableListOf<String>()
            var line = 0; var station = ""

            for (i in 0 until subs.length()) {
                val sp   = subs.getJSONObject(i)
                val type = sp.optInt("trafficType", 3)

                when (type) {
                    1 -> { // 지하철
                        val laneObj    = sp.optJSONArray("lane")?.optJSONObject(0)
                        val subwayCode = laneObj?.optInt("subwayCode", 0) ?: 0
                        val laneName   = laneObj?.optString("name", "") ?: ""
                        val lineNum = subwayCode.takeIf { it > 0 }
                            ?: Regex("(\\d+)호선").find(laneName)?.groupValues?.get(1)?.toIntOrNull()
                            ?: 0
                        val displayName = if (lineNum > 0) "${lineNum}호선" else laneName
                        if (displayName.isNotEmpty()) parts.add("🚇 $displayName")
                        if (line == 0) line = lineNum
                        if (lineNum > 0) {
                            val st = sp.optString("startName","").removeSuffix("역")
                            station = station.ifEmpty { st }
                            subwaySegments.add(Pair(lineNum, st))
                        }
                        Log.d(TAG, "지하철 subPath[$i]: subwayCode=$subwayCode, laneName=$laneName, lineNum=$lineNum, startName=${sp.optString("startName")}")
                    }
                    2 -> { // 버스
                        val lanes = sp.optJSONArray("lane") ?: continue
                        if (lanes.length() == 0) continue
                        val no = lanes.getJSONObject(0).optString("busNo","")
                        if (no.isNotEmpty()) parts.add("🚌 ${no}번")
                    }
                    3 -> { // 도보
                        val walkMin = sp.optInt("sectionTime", 0)
                        if (walkMin > 0) parts.add("🚶 ${walkMin}분")
                    }
                }
            }
            // subwaySegments는 위에서 이미 채워짐
            Log.d(TAG, "ODsay 파싱 결과: line=$line, station=$station, parts=$parts")
            val label    = parts.joinToString(" → ").ifBlank { "대중교통" }
            val lineName = if (line > 0) "${line}호선" else ""
            OdsayResult(label, total, line, lineName, station)
        } catch (e: Exception) { Log.e(TAG, "ODsay 오류: ${e.message}"); null }
    }

    // ──────────────────────────────────────────────
    // 앉을 확률 (시간대 기반)
    // ──────────────────────────────────────────────

    private fun estimateSeatProb(targetTime: String, targetDate: String): Int {
        val parts = targetTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val t = h * 60 + m
        val cal = Calendar.getInstance()
        try { cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(targetDate) ?: Date() }
        catch (_: Exception) {}
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return 78
        return when (t) {
            in 450..570   -> 22
            in 570..660   -> 48
            in 660..840   -> 68
            in 840..960   -> 60
            in 960..1080  -> 45
            in 1080..1200 -> 20
            in 1200..1320 -> 52
            else          -> 72
        }
    }

    // ──────────────────────────────────────────────
    // 기상청 단기예보
    // ──────────────────────────────────────────────

    data class WeatherResult(val hasRain: Boolean, val description: String)

    private suspend fun fetchWeather(
        lat: Double, lon: Double,
        targetDate: String, targetTime: String
    ): WeatherResult = withContext(Dispatchers.IO) {
        if (lat == 0.0 || lon == 0.0) return@withContext WeatherResult(false, "없음")
        try {
            val (nx, ny) = latLonToGrid(lat, lon)
            val nowCal   = Calendar.getInstance()
            val nowHHMM  = nowCal.get(Calendar.HOUR_OF_DAY) * 100 + nowCal.get(Calendar.MINUTE)
            val slots    = listOf(2300, 2000, 1700, 1400, 1100, 800, 500, 200)
            val baseInt  = slots.firstOrNull { (it + 10) <= nowHHMM } ?: run {
                nowCal.add(Calendar.DATE, -1); 2300
            }
            val baseDate = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(nowCal.time)
            val baseTime = "%04d".format(baseInt)
            val encKey   = URLEncoder.encode(PUBLIC_KEY, "UTF-8")
            val url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                    "?serviceKey=$encKey&pageNo=1&numOfRows=1000&dataType=JSON" +
                    "&base_date=$baseDate&base_time=$baseTime&nx=$nx&ny=$ny"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            if (conn.responseCode != 200) return@withContext WeatherResult(false, "없음")
            val items = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                .optJSONObject("response")?.optJSONObject("body")
                ?.optJSONObject("items")?.optJSONArray("item")
                ?: return@withContext WeatherResult(false, "없음")
            val fcstDate = targetDate.replace("-", "")
            val fcstTime = "${targetTime.take(2).padStart(2,'0')}00"
            var pty = 0; var pop = 0
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optString("fcstDate") == fcstDate && item.optString("fcstTime") == fcstTime) {
                    when (item.optString("category")) {
                        "PTY" -> pty = item.optString("fcstValue","0").toIntOrNull() ?: 0
                        "POP" -> pop = item.optString("fcstValue","0").toIntOrNull() ?: 0
                    }
                }
            }
            val hasRain = pty > 0 || pop >= 60
            val desc = when (pty) {
                1 -> "비 (${pop}%)"; 2 -> "비/눈 (${pop}%)"; 3 -> "눈 (${pop}%)"
                4 -> "소나기 (${pop}%)"
                else -> if (pop >= 60) "비 가능성 ${pop}%" else "없음"
            }
            WeatherResult(hasRain, desc)
        } catch (e: Exception) { Log.e(TAG, "기상청: ${e.message}"); WeatherResult(false, "없음") }
    }

    private fun latLonToGrid(lat: Double, lon: Double): Pair<Int, Int> {
        val re = 6371.00877 / 5.0
        val s1 = Math.toRadians(30.0); val s2 = Math.toRadians(60.0)
        val ol = Math.toRadians(126.0); val oa = Math.toRadians(38.0)
        val sn = ln(cos(s1)/cos(s2)) / ln(tan(PI/4+s2/2)/tan(PI/4+s1/2))
        val sf = (tan(PI/4+s1/2).pow(sn) * cos(s1)) / sn
        val ro = re * sf / tan(PI/4+oa/2).pow(sn)
        val ra = re * sf / tan(PI/4+Math.toRadians(lat)/2).pow(sn)
        var th = Math.toRadians(lon) - ol
        if (th > PI) th -= 2*PI; if (th < -PI) th += 2*PI; th *= sn
        return Pair((ra*sin(th)+43+0.5).toInt(), (ro-ra*cos(th)+136+0.5).toInt())
    }

    // ──────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────

    private fun calcDepartTime(targetTime: String, minutesBefore: Int): String {
        val p = targetTime.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        val t = (h*60 + m - minutesBefore).let { if (it < 0) it + 1440 else it }
        return "%02d:%02d".format(t/60, t%60)
    }

    private fun calcArriveTime(targetTime: String, minutesAfter: Int): String {
        val p = targetTime.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        val t = (h*60 + m + minutesAfter) % 1440
        return "%02d:%02d".format(t/60, t%60)
    }

    private fun drawStackBar(values: List<Int>, total: Int) {
        binding.stackBar.removeAllViews()
        if (total == 0) return
        values.forEachIndexed { i, v ->
            if (v > 0) binding.stackBar.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, v.toFloat())
                setBackgroundColor(Color.parseColor(stackColors[i]))
            })
        }
    }

    private fun drawLegend(values: List<Int>) {
        binding.legendGroup.removeAllViews()
        values.forEachIndexed { i, v ->
            if (v > 0) binding.legendGroup.addView(Chip(requireContext()).apply {
                text = "${stackLabels[i]} ${v}분"
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    Color.argb(40,
                        Color.red(Color.parseColor(stackColors[i])),
                        Color.green(Color.parseColor(stackColors[i])),
                        Color.blue(Color.parseColor(stackColors[i])))
                )
                setTextColor(Color.parseColor(stackColors[i]))
                isClickable = false; textSize = 10f
            })
        }
    }

    // ──────────────────────────────────────────────
    // 지하철 혼잡도 (서울 열린데이터광장 실시간 혼잡도)
    // ──────────────────────────────────────────────

    private fun showCongestionDetail() {
        if (subwaySegments.isEmpty()) {
            Toast.makeText(requireContext(), "지하철 경로 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (subwaySegments.size == 1) {
            // 지하철 1개 구간 → 바로 조회
            val (line, station) = subwaySegments[0]
            loadAndShowCongestion(line, station)
        } else {
            // 지하철 여러 구간 → 선택 다이얼로그
            val items = subwaySegments.map { (line, station) -> "${line}호선 (${station}역 탑승)" }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("혼잡도 확인할 호선 선택")
                .setItems(items) { _, idx ->
                    val (line, station) = subwaySegments[idx]
                    loadAndShowCongestion(line, station)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun loadAndShowCongestion(line: Int, station: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = fetchSubwayCongestion(line, station)
            withContext(Dispatchers.Main) {
                showCongestionDialog(line, station, data)
            }
        }
    }

    data class CarCongestion(
        val trainNo: String,
        val cars: List<Int>   // 1=여유 2=보통 3=혼잡 4=매우혼잡
    )

    private suspend fun fetchSubwayCongestion(line: Int, station: String): CarCongestion? =
        withContext(Dispatchers.IO) {
            try {
                val cleanStation = station.removeSuffix("역")
                val lineName = URLEncoder.encode("${line}호선", "UTF-8")
                val stName   = URLEncoder.encode(cleanStation, "UTF-8")
                val url = "http://openapi.seoul.go.kr:8088/$SEOUL_KEY/json/realtimeCongestion/1/5/$lineName/$stName/"
                Log.d(TAG, "혼잡도 API 호출: line=${line}호선, station=$cleanStation")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000; conn.readTimeout = 6000
                val code = conn.responseCode
                Log.d(TAG, "혼잡도 HTTP 응답코드: $code")
                if (code != 200) return@withContext simulateCongestion(line, station)

                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                Log.d(TAG, "혼잡도 응답: ${body.take(300)}")
                val json = JSONObject(body)
                // ERROR-500 or 인증키 오류 → 시뮬레이션으로 폴백
                val resultCode = json.optJSONObject("RESULT")?.optString("CODE") ?: ""
                if (resultCode.isNotEmpty()) {
                    Log.w(TAG, "혼잡도 API 오류($resultCode) → 시뮬레이션 데이터 사용")
                    return@withContext simulateCongestion(line, station)
                }
                val rows = json.optJSONObject("realtimeCongestion")
                    ?.optJSONArray("row")
                    ?: return@withContext simulateCongestion(line, station)
                if (rows.length() == 0) return@withContext simulateCongestion(line, station)

                val row     = rows.getJSONObject(0)
                val trainNo = row.optString("TRAIN_NO", "")
                val cars = (1..10).map { i ->
                    row.optInt("CONGESTION_CAR$i", 0)
                }.filter { it > 0 }
                if (cars.isEmpty()) return@withContext simulateCongestion(line, station)
                CarCongestion(trainNo, cars)
            } catch (e: Exception) {
                Log.e(TAG, "혼잡도 API 오류: ${e.message}")
                simulateCongestion(line, station)
            }
        }

    private fun simulateCongestion(line: Int, station: String): CarCongestion {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isPeakHour = h in 7..9 || h in 17..20
        val baseCongestion = when {
            station.contains("강남") || station.contains("신도림") || station.contains("홍대") -> if (isPeakHour) 4 else 3
            station.contains("서울역") || station.contains("잠실") -> if (isPeakHour) 3 else 2
            else -> if (isPeakHour) 3 else 1
        }
        val cars = (1..10).map {
            val offset = (Math.random() * 2 - 1).toInt()
            (baseCongestion + offset).coerceIn(1, 4)
        }
        return CarCongestion("시뮬레이션", cars)
    }

    private fun showCongestionDialog(line: Int, station: String, data: CarCongestion?) {
        val levelText = listOf("", "여유 🟢", "보통 🟡", "혼잡 🔴", "매우혼잡 🔴🔴")
        val levelColor = listOf(0, 0xFF4CAF50.toInt(), 0xFFFFB300.toInt(),
            0xFFE53935.toInt(), 0xFFB71C1C.toInt())

        val ctx = requireContext()
        val scroll = android.widget.ScrollView(ctx)
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        scroll.addView(container)

        if (data == null) return  // simulateCongestion으로 대체되어 null이 올 수 없음
        run {
            container.addView(android.widget.TextView(ctx).apply {
                text = "🚇 ${line}호선 $station 역\n열차 ${data.trainNo}"
                textSize = 15f; setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 16)
            })

            val minCongestion = data.cars.minOrNull() ?: 2
            val bestCars = data.cars.mapIndexedNotNull { i, v -> if (v == minCongestion) i + 1 else null }

            data.cars.forEachIndexed { i, level ->
                val row = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }
                row.addView(android.widget.TextView(ctx).apply {
                    text = "${i + 1}칸"
                    textSize = 14f; minWidth = 80
                })
                row.addView(android.widget.TextView(ctx).apply {
                    text = levelText.getOrElse(level) { "정보없음" }
                    textSize = 14f
                    if (level in 1..4) setTextColor(levelColor[level])
                })
                if (bestCars.contains(i + 1)) {
                    row.addView(android.widget.TextView(ctx).apply {
                        text = " ← 추천"
                        textSize = 12f; setTextColor(0xFF1976D2.toInt())
                    })
                }
                container.addView(row)
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("칸별 혼잡도")
            .setView(scroll)
            .setPositiveButton("닫기", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { findNavController().popBackStack(); return true }
        return super.onOptionsItemSelected(item)
    }
}