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
import com.example.whentoleave.ui.common.PlaceSearchBottomSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import android.Manifest
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whentoleave.util.RecentSearchManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0)
    private var isArrivalBased = true

    private var startLat = 0.0
    private var startLon = 0.0
    private var endLat   = 0.0
    private var endLon   = 0.0

    private lateinit var recentAdapter: RecentSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDateDisplay()
        updateTimeDisplay()

        // 최근 검색 RecyclerView 설정
        recentAdapter = RecentSearchAdapter { recent ->
            // 최근 검색 항목 탭 시 → 출발지/목적지 자동 입력
            binding.tvStartLocation.text = recent.startAddress
            binding.tvStartLocation.setTextColor(resources.getColor(R.color.text_primary, null))
            binding.tvEndLocation.text = recent.endAddress
            binding.tvEndLocation.setTextColor(resources.getColor(R.color.text_primary, null))
            startLat = 0.0; startLon = 0.0
            endLat = 0.0; endLon = 0.0
        }
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter
        loadRecentSearches()

        // 도착/출발 기준 토글
        binding.toggleTimeType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isArrivalBased = (checkedId == R.id.btn_arrival_based)
                binding.tvTimeLabel.text = if (isArrivalBased) "도착 시간" else "출발 시간"
            }
        }
        binding.btnArrivalBased.isChecked = true

        binding.cardDate.setOnClickListener { showDatePicker() }
        binding.cardTime.setOnClickListener { showTimePicker() }

        // ── 출발지: GPS or 장소 검색 ──
        binding.rowStart.setOnClickListener {
            val hasGps = ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasGps) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                return@setOnClickListener
            }
            // getCurrentLocation으로 신선한 위치 획득 (lastLocation은 캐시라 부정확)
            val client = LocationServices.getFusedLocationProviderClient(requireActivity())
            client.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    startLat = loc.latitude
                    startLon = loc.longitude
                    binding.tvStartLocation.text = "현재 위치"
                    binding.tvStartLocation.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                } else {
                    // getCurrentLocation 실패 시 lastLocation fallback
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            startLat = last.latitude; startLon = last.longitude
                            binding.tvStartLocation.text = "현재 위치"
                            binding.tvStartLocation.setTextColor(
                                resources.getColor(R.color.text_primary, null)
                            )
                        } else {
                            openPlaceSearch(isStart = true)
                        }
                    }.addOnFailureListener { openPlaceSearch(isStart = true) }
                }
            }.addOnFailureListener {
                // API가 없는 구버전 → lastLocation
                client.lastLocation.addOnSuccessListener { last ->
                    if (last != null) {
                        startLat = last.latitude; startLon = last.longitude
                        binding.tvStartLocation.text = "현재 위치"
                        binding.tvStartLocation.setTextColor(
                            resources.getColor(R.color.text_primary, null)
                        )
                    } else {
                        openPlaceSearch(isStart = true)
                    }
                }.addOnFailureListener { openPlaceSearch(isStart = true) }
            }
        }

        // 출발지 텍스트 직접 탭 → 항상 검색
        binding.tvStartLocation.setOnClickListener { openPlaceSearch(isStart = true) }

        // ── 목적지: 장소 검색 ──
        binding.rowEnd.setOnClickListener { openPlaceSearch(isStart = false) }
        binding.tvEndLocation.setOnClickListener { openPlaceSearch(isStart = false) }

        // ── 길찾기 ──
        binding.btnSearch.setOnClickListener {
            val start = binding.tvStartLocation.text.toString()
            val end   = binding.tvEndLocation.text.toString()

            if (start == "출발지 입력" || start.isEmpty()) {
                showSnackbar("출발지를 입력해주세요"); return@setOnClickListener
            }
            if (end == "목적지 입력" || end.isEmpty()) {
                showSnackbar("목적지를 입력해주세요"); return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("startAddress", start)
                putString("endAddress",   end)
                putString("targetDate",   selectedDate.toString())
                putString("targetTime",
                    selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                putString("timeType",  if (isArrivalBased) "ARRIVAL" else "DEPARTURE")
                putString("purpose",   getSelectedPurpose())
                putDouble("startLat",  startLat)
                putDouble("startLon",  startLon)
                putDouble("endLat",    endLat)
                putDouble("endLon",    endLon)
            }
            // 최근 검색 저장
            RecentSearchManager.save(requireContext(), start, end)
            findNavController().navigate(R.id.action_home_to_result, bundle)
            // 대중교통 탭 시각적 포커스 (NavController 재탐색 없이 메뉴 체크만 변경)
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.menu?.findItem(R.id.transitFragment)?.isChecked = true
        }
    }

    // ── 장소 검색 바텀시트 ────────────────────────────────
    private fun openPlaceSearch(isStart: Boolean) {
        PlaceSearchBottomSheet.newInstance().also { sheet ->
            sheet.onPlaceSelected = { place ->
                if (isStart) {
                    startLat = place.lat; startLon = place.lon
                    binding.tvStartLocation.text = place.name
                    binding.tvStartLocation.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                } else {
                    endLat = place.lat; endLon = place.lon
                    binding.tvEndLocation.text = place.name
                    binding.tvEndLocation.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                }
            }
            sheet.show(childFragmentManager, "place_search")
        }
    }

    // ── 날짜/시간 ─────────────────────────────────────────
    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            updateDateDisplay()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(requireContext(), { _, hour, minute ->
            selectedTime = LocalTime.of(hour, minute)
            updateTimeDisplay()
        }, selectedTime.hour, selectedTime.minute, false).show()
    }

    private fun updateDateDisplay() {
        val today = LocalDate.now()
        binding.tvDate.text = when (selectedDate) {
            today              -> "오늘"
            today.plusDays(1)  -> "내일"
            else               -> selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))
        }
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun getSelectedPurpose(): String = when (binding.chipGroupPurpose.checkedChipId) {
        R.id.chip_school      -> "SCHOOL"
        R.id.chip_work        -> "WORK"
        R.id.chip_appointment -> "APPOINTMENT"
        R.id.chip_exam        -> "EXAM"
        R.id.chip_interview   -> "INTERVIEW"
        R.id.chip_hospital    -> "HOSPITAL"
        else                  -> "GENERAL"
    }

    private fun loadRecentSearches() {
        val list = RecentSearchManager.getAll(requireContext())
        recentAdapter.submitList(list)
    }

    override fun onResume() {
        super.onResume()
        loadRecentSearches()
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