package com.example.whentoleave.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
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
    }

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val stackColors = listOf(
        "#2D6A6A", "#8B90A8", "#FF6B57", "#3DDC97", "#FFB627", "#FF8C42", "#94A3B8"
    )
    private val stackLabels = listOf(
        "기본 이동", "대기", "혼잡", "날씨", "교통", "목적", "개인"
    )

    private var subwayLine    = 0
    private var subwayStation = ""

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
                applyUi(backend, odsay, weather, targetTime, targetDate)
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
        targetDate: String
    ) {
        binding.tvTransportInfo.text = odsay?.routeLabel ?: "대중교통"
        binding.tvSeatProb.text = "${estimateSeatProb(targetTime, targetDate)}%"
        binding.tvRainTime.text = if (weather.hasRain) weather.description else "없음"

        when {
            backend != null -> {
                binding.tvDepartTime.text = backend.recommendedDepartureTime.take(5)
                binding.tvArriveTime.text = backend.expectedArrivalTime.take(5)
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
                binding.tvDepartTime.text = calcDepartTime(targetTime, total)
                binding.tvArriveTime.text = targetTime.take(5)
                binding.tvTravelTime.text = "${total}분"   // 버퍼 포함 총 시간
                // vals 합 = total 이 되도록 구성 (0은 drawStackBar에서 제외됨)
                val vals = listOf(odsay.totalTime, 0, 0, weatherBuf, 0, purposeBuf, personalBuf)
                drawStackBar(vals, total); drawLegend(vals)
                binding.tvRecommendationSummary.text =
                    if (weather.hasRain) "출발 시간대에 ${weather.description} 예보가 있습니다. 여유 시간을 추가했습니다."
                    else "대중교통 이동시간 ${odsay.totalTime}분 + 여유 ${weatherBuf + purposeBuf + personalBuf}분을 반영했습니다."
            }
            else -> {
                binding.tvDepartTime.text = calcDepartTime(targetTime, 46)
                binding.tvArriveTime.text = targetTime.take(5)
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
                           val line: Int, val station: String)

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
                if (type == 3) continue
                val lanes = sp.optJSONArray("lane") ?: continue
                if (lanes.length() == 0) continue
                val lane = lanes.getJSONObject(0)
                when (type) {
                    1 -> {
                        val name = lane.optString("name", "")
                        if (name.isNotEmpty()) parts.add(name)
                        if (line == 0) {
                            line    = name.replace("호선","").trim().toIntOrNull() ?: 0
                            station = sp.optString("startName","")
                        }
                    }
                    2 -> {
                        val no = lane.optString("busNo","")
                        if (no.isNotEmpty()) parts.add("${no}번 버스")
                    }
                }
            }
            subwayLine = line; subwayStation = station
            val label = parts.take(2).joinToString(" · ").ifBlank { "대중교통" }
            OdsayResult(label, total, line, station)
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

    private fun showCongestionDetail() {
        val msg = if (subwayLine > 0)
            "${subwayLine}호선 $subwayStation 역 기준\n앞쪽 칸(1~3호)이 상대적으로 여유롭습니다."
        else "지하철 경로 정보가 없습니다."
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { findNavController().popBackStack(); return true }
        return super.onOptionsItemSelected(item)
    }
}