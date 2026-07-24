package com.example.whentoleave.ui.transit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whentoleave.databinding.FragmentTransitBinding
import kotlin.math.max
import kotlin.math.min

class TransitFragment : Fragment() {

    private var _binding: FragmentTransitBinding? = null
    private val binding get() = _binding!!

    private lateinit var carAdapter: CongestionCarAdapter

    // 호선 정보
    private val lines = listOf(
        "1호선", "2호선", "3호선", "4호선", "5호선",
        "6호선", "7호선", "8호선", "9호선"
    )
    private val lineNumbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    private val lineBadgeColors = listOf(
        "#0052A4", "#00A84D", "#EF7C1C", "#00A5DE", "#996CAC",
        "#CD7C2F", "#747F00", "#E6186C", "#BDB092"
    )

    // 호선별 주요 역 (선택 → 해당 호선 역만 표시)
    private val stationsByLine = listOf(
        // 1호선
        listOf("서울역", "시청", "종각", "종로3가", "동대문", "청량리", "신이문", "석계", "수원", "인천"),
        // 2호선
        listOf("시청", "을지로입구", "을지로3가", "동대문역사문화공원", "성수", "건대입구",
                "잠실", "삼성", "선릉", "강남", "교대", "홍대입구", "신촌", "신도림",
                "구로디지털단지", "합정", "당산"),
        // 3호선
        listOf("수서", "대청", "도곡", "매봉", "양재", "교대", "고속터미널",
                "압구정", "옥수", "동대입구", "충무로", "종로3가", "안국", "경복궁", "불광"),
        // 4호선
        listOf("당고개", "노원", "창동", "미아", "성신여대입구", "혜화", "동대문",
                "충무로", "명동", "서울역", "동작", "사당", "남태령"),
        // 5호선
        listOf("방화", "마곡", "발산", "목동", "영등포구청", "여의도", "여의나루",
                "공덕", "광화문", "동대문역사문화공원", "왕십리", "군자", "천호", "강동", "상일동"),
        // 6호선
        listOf("불광", "연신내", "디지털미디어시티", "월드컵경기장", "마포구청",
                "합정", "상수", "공덕", "이태원", "약수", "신당", "고려대", "태릉입구"),
        // 7호선
        listOf("도봉산", "노원", "태릉입구", "건대입구", "뚝섬유원지", "청담",
                "강남구청", "학동", "논현", "고속터미널", "이수", "보라매",
                "가산디지털단지", "철산", "온수"),
        // 8호선
        listOf("암사", "천호", "강동구청", "몽촌토성", "잠실", "석촌",
                "송파", "가락시장", "복정", "모란"),
        // 9호선
        listOf("김포공항", "마곡나루", "가양", "증미", "당산", "국회의사당",
                "여의도", "고속터미널", "신논현", "언주", "봉은사", "종합운동장", "올림픽공원")
    )

    // 현재 선택 상태
    private var selectedLineIdx = 0
    private var selectedStationIdx = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView (가로 칸 혼잡도)
        carAdapter = CongestionCarAdapter(generateCongestion(stationsByLine[0][0]))
        binding.rvCars.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvCars.adapter = carAdapter

        // ① 호선 스피너 먼저 세팅
        binding.spinnerLine.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, lines
        )

        // ② 초기 역 스피너 (1호선 역)
        updateStationSpinner(0)

        // 호선 선택 시 → 역 스피너 업데이트 (station 선택 listener 는 updateStationSpinner 내부에서 설정)
        binding.spinnerLine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedLineIdx = pos
                selectedStationIdx = 0
                updateStationSpinner(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 초기 화면 표시
        updateDisplay(stationsByLine[0][0], 0)
    }

    /** 선택된 호선에 맞게 역 스피너를 갱신하고 onChange 리스너를 다시 설정 */
    private fun updateStationSpinner(lineIdx: Int) {
        val stations = stationsByLine[lineIdx]
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, stations
        )
        binding.spinnerStation.adapter = adapter
        binding.spinnerStation.setSelection(0, false)

        binding.spinnerStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedStationIdx = pos
                updateDisplay(stations[pos], lineIdx)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDisplay(station: String, lineIdx: Int) {
        val congestion = generateCongestion(station)
        carAdapter.update(congestion)

        // 앉을 확률 (혼잡도 반비례)
        val avgCongestion = congestion.average().toInt()
        val seatProb = max(0, min(100, 100 - avgCongestion))
        binding.tvSeatProbability.text = "$seatProb%"

        // 제목 & 배지
        binding.tvSubwayTitle.text = "${lines[lineIdx]} · ${station}역"
        binding.tvLineBadge.text = lineNumbers[lineIdx]
        try {
            binding.tvLineBadge.setBackgroundColor(
                android.graphics.Color.parseColor(lineBadgeColors[lineIdx])
            )
        } catch (e: Exception) { }

        // 추천 칸 (혼잡도 낮은 순 상위 3개)
        val sorted = congestion.mapIndexed { i, v -> i + 1 to v }.sortedBy { it.second }
        val top3 = sorted.take(3).map { "${it.first}호" }.joinToString(", ")
        val lineColor = lineBadgeColors[lineIdx]
        binding.tvRecommendCars.text = "💡 추천 탑승 칸: $top3\n앉아서 갈 확률 $seatProb%로 ${
            when {
                seatProb >= 70 -> "여유롭게 이용할 수 있어요."
                seatProb >= 40 -> "보통 수준입니다. 앞칸이 더 여유로울 수 있어요."
                else           -> "혼잡합니다. 다음 열차를 기다리거나 앞/뒤 칸을 이용하세요."
            }
        }"
    }

    /** 역 이름과 시간대를 기반으로 10칸 혼잡도 시뮬레이션 (0–100) */
    private fun generateCongestion(station: String): List<Int> {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isPeak = hour in 7..9 || hour in 17..20

        val base = when {
            station.contains("강남") || station.contains("홍대") || station.contains("신도림") -> if (isPeak) 82 else 58
            station.contains("잠실") || station.contains("신림") || station.contains("사당") ||
            station.contains("여의도") || station.contains("광화문") -> if (isPeak) 72 else 48
            station.contains("서울역") || station.contains("청량리") -> if (isPeak) 65 else 42
            else -> if (isPeak) 50 else 30
        }

        // 앞뒤 칸보다 가운데 칸이 더 혼잡한 패턴 반영
        return (1..10).map { i ->
            val midBoost = when (i) { in 4..7 -> 12; in 3..8 -> 6; else -> 0 }
            val noise = (Math.random() * 20 - 10).toInt()
            min(100, max(5, base + midBoost + noise))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
