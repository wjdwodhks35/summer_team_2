package com.example.whentoleave.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.data.api.TripApiService
import com.example.whentoleave.data.model.TripRequest
import com.example.whentoleave.data.model.TripResponse
import com.example.whentoleave.databinding.FragmentResultBinding
import kotlinx.coroutines.launch

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    // 시간 스택 바 색상 (순서: 기본이동, 대기, 혼잡, 날씨, 교통, 목적, 개인)
    private val stackColors = listOf(
        "#2D6A6A",  // teal - 기본 이동
        "#8B90A8",  // slate - 대기
        "#FF6B57",  // coral - 혼잡
        "#3DDC97",  // mint - 날씨
        "#FFB627",  // amber - 교통
        "#FF8C42",  // orange - 목적
        "#1B1F3B"   // navy - 개인
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // HomeFragment에서 넘어온 데이터
        val startAddress = arguments?.getString("startAddress") ?: ""
        val endAddress = arguments?.getString("endAddress") ?: ""
        val targetDate = arguments?.getString("targetDate") ?: ""
        val targetTime = arguments?.getString("targetTime") ?: ""
        val timeType = arguments?.getString("timeType") ?: "ARRIVAL"
        val purpose = arguments?.getString("purpose") ?: "GENERAL"

        binding.tvRouteSummary.text = "$startAddress → $endAddress"

        // API 호출
        fetchResult(startAddress, endAddress, targetDate, targetTime, timeType, purpose)
    }

    private fun fetchResult(
        start: String, end: String,
        date: String, time: String,
        timeType: String, purpose: String
    ) {
        val api = RetrofitClient.instance.create(TripApiService::class.java)
        val targetDateTime = "${date}T${time}:00"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.calculateTrip(
                    TripRequest(start, end, targetDateTime, timeType, purpose)
                )
                if (response.isSuccessful) {
                    response.body()?.let { displayResult(it) }
                }
            } catch (e: Exception) {
                // 서버 연결 전 임시 더미 데이터 표시
                displayDummy()
            }
        }
    }

    private fun displayResult(result: TripResponse) {
        binding.tvDepartTime.text = result.recommendedDepartureTime
        binding.tvArriveTime.text = result.estimatedArrivalTime
        binding.tvTransportInfo.text = result.transportType + " · 추천"
        binding.tvSeatProb.text = "${result.seatProbability}%"
        binding.tvRainTime.text = if (result.hasRain) result.rainTime ?: "있음" else "없음"
        binding.tvTravelTime.text = "${result.totalTravelMinutes}분"
        binding.tvRecommendationSummary.text = result.recommendationNote

        // 시간 스택 바 그리기
        val bd = result.timeBreakdown
        val total = bd.baseTravelMinutes + bd.waitMinutes + bd.congestionMinutes +
                bd.weatherMinutes + bd.trafficMinutes + bd.purposeMinutes + bd.personalMinutes
        val values = listOf(
            bd.baseTravelMinutes, bd.waitMinutes, bd.congestionMinutes,
            bd.weatherMinutes, bd.trafficMinutes, bd.purposeMinutes, bd.personalMinutes
        )
        drawStackBar(values, total)
    }

    private fun displayDummy() {
        // 백엔드 연결 전 임시 표시
        binding.tvDepartTime.text = "18:14"
        binding.tvArriveTime.text = "19:00"
        binding.tvTransportInfo.text = "지하철 2호선 · 추천"
        binding.tvSeatProb.text = "62%"
        binding.tvRainTime.text = "없음"
        binding.tvTravelTime.text = "46분"
        binding.tvRecommendationSummary.text =
            "출발 후 10분 이내에 혼잡 구간이 있어 여유 시간을 추가했습니다."

        drawStackBar(listOf(30, 5, 5, 0, 3, 2, 1), 46)
    }

    private fun drawStackBar(values: List<Int>, total: Int) {
        binding.stackBar.removeAllViews()
        if (total == 0) return

        values.forEachIndexed { index, value ->
            if (value > 0) {
                val v = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        value.toFloat()
                    )
                    setBackgroundColor(Color.parseColor(stackColors[index]))
                }
                binding.stackBar.addView(v)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}