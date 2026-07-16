package com.example.whentoleave.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0)
    private var isArrivalBased = true  // true=도착기준, false=출발기준

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기값 표시
        updateDateDisplay()
        updateTimeDisplay()

        // 도착/출발 기준 토글
        binding.toggleTimeType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isArrivalBased = (checkedId == R.id.btn_arrival_based)
                binding.tvTimeLabel.text = if (isArrivalBased) "도착 시간" else "출발 시간"
            }
        }
        binding.btnArrivalBased.isChecked = true

        // 날짜 선택
        binding.cardDate.setOnClickListener { showDatePicker() }

        // 시간 선택
        binding.cardTime.setOnClickListener { showTimePicker() }

        // 출발지/목적지 클릭 (추후 주소 검색 연동)
        binding.rowStart.setOnClickListener {
            binding.tvStartLocation.text = "현재 위치"  // TODO: 주소 검색
            binding.tvStartLocation.setTextColor(
                resources.getColor(R.color.text_primary, null)
            )
        }
        binding.rowEnd.setOnClickListener {
            // TODO: 주소 검색 Fragment or 다이얼로그
        }

        // 길찾기 버튼
        binding.btnSearch.setOnClickListener {
            val start = binding.tvStartLocation.text.toString()
            val end = binding.tvEndLocation.text.toString()

            if (start == "출발지 입력" || start.isEmpty()) {
                binding.tvStartLocation.error  // 시각적 힌트
                showSnackbar("출발지를 입력해주세요")
                return@setOnClickListener
            }
            if (end == "목적지 입력" || end.isEmpty()) {
                showSnackbar("목적지를 입력해주세요")
                return@setOnClickListener
            }

            // ResultFragment로 이동 (입력값 전달)
            val bundle = Bundle().apply {
                putString("startAddress", start)
                putString("endAddress", end)
                putString("targetDate", selectedDate.toString())
                putString("targetTime", selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                putString("timeType", if (isArrivalBased) "ARRIVAL" else "DEPARTURE")
                putString("purpose", getSelectedPurpose())
            }
            findNavController().navigate(R.id.action_home_to_result, bundle)
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                updateDateDisplay()
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                updateTimeDisplay()
            },
            selectedTime.hour,
            selectedTime.minute,
            false
        ).show()
    }

    private fun updateDateDisplay() {
        val today = LocalDate.now()
        binding.tvDate.text = when (selectedDate) {
            today -> "오늘"
            today.plusDays(1) -> "내일"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))
        }
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun getSelectedPurpose(): String {
        return when (binding.chipGroupPurpose.checkedChipId) {
            R.id.chip_school -> "SCHOOL"
            R.id.chip_work -> "WORK"
            R.id.chip_appointment -> "APPOINTMENT"
            R.id.chip_exam -> "EXAM"
            R.id.chip_interview -> "INTERVIEW"
            R.id.chip_hospital -> "HOSPITAL"
            else -> "GENERAL"
        }
    }

    private fun showSnackbar(msg: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}