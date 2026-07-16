package com.example.whentoleave.ui.transit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.whentoleave.databinding.FragmentTransitBinding

class TransitFragment : Fragment() {

    private var _binding: FragmentTransitBinding? = null
    private val binding get() = _binding!!

    // 주요 지하철역 목록 (추후 API로 교체)
    private val stations = listOf(
        "강남", "홍대입구", "신촌", "건대입구", "잠실",
        "고려대", "성신여대입구", "동대문역사문화공원",
        "광화문", "종각", "서울역", "신림", "사당"
    )

    private val lines = listOf("1호선", "2호선", "3호선", "4호선", "5호선", "6호선", "7호선", "8호선", "9호선")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 역 스피너
        binding.spinnerStation.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            stations
        )

        // 호선 스피너
        binding.spinnerLine.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            lines
        )

        // 스피너 선택 시 혼잡도 조회 (추후 API 연동)
        // 지금은 더미 데이터 표시
        binding.tvSeatProbability.text = "62%"
        binding.tvRecommendCars.text =
            "💡 1, 2, 7, 8, 10호 칸이 가장 여유로워요. 특히 앞쪽 칸(1~2호)을 추천합니다."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}