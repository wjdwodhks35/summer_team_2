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
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0)
    private var isArrivalBased = true  // true=도착기준, false=출발기준
    private var startLat = 0.0
    private var startLon = 0.0
    private var endLat = 0.0    // 추가
    private var endLon = 0.0    // 추가

    private fun showSnackbar(msg: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

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

        // 출발지 클릭 — GPS 또는 직접 입력 선택
        binding.rowStart.setOnClickListener { showAddressDialog(isStart = true) }
        binding.tvStartLocation.setOnClickListener { showAddressDialog(isStart = true) }
        binding.rowEnd.setOnClickListener { showAddressDialog(isStart = false) }
        binding.tvEndLocation.setOnClickListener { showAddressDialog(isStart = false) }
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
                putDouble("startLat", startLat)
                putDouble("startLon", startLon)
                putDouble("endLat", endLat)   // 추가
                putDouble("endLon", endLon)   // 추가
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

    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        LocationServices.getFusedLocationProviderClient(requireActivity())
            .lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    startLat = loc.latitude
                    startLon = loc.longitude
                    binding.tvStartLocation.text = "현재 위치"
                } else {
                    showSnackbar("GPS 위치를 가져올 수 없습니다")
                    showAddressDialog(isStart = true)   // fallback
                }
            }
    }

    data class PoiResult(val name: String, val address: String, val lat: Double, val lon: Double)

    private fun showAddressDialog(isStart: Boolean) {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val searchRow = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val searchInput = android.widget.EditText(requireContext()).apply {
            hint = "장소명 또는 주소 입력"
            isSingleLine = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val searchBtn = android.widget.Button(requireContext()).apply { text = "검색" }
        searchRow.addView(searchInput)
        searchRow.addView(searchBtn)

        val listView = android.widget.ListView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 600
            )
        }
        layout.addView(searchRow)
        layout.addView(listView)

        val poiResults = mutableListOf<PoiResult>()
        val adapter = android.widget.ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle(if (isStart) "출발지 검색" else "목적지 검색")
            .setView(layout)
            .setPositiveButton("직접 입력") { _, _ ->
                val addr = searchInput.text.toString().trim()
                if (addr.isNotEmpty()) {
                    if (isStart) binding.tvStartLocation.text = addr
                    else binding.tvEndLocation.text = addr
                }
            }
            .setNegativeButton("취소", null)
            .create()

        searchBtn.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isEmpty()) return@setOnClickListener
            lifecycleScope.launch {          // viewLifecycleOwner 없이 Fragment의 lifecycleScope 직접 사용
                val results = searchPoi(query)
                poiResults.clear()
                poiResults.addAll(results)
                adapter.clear()
                adapter.addAll(results.map { "${it.name}  ${it.address}" })
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val sel = poiResults[pos]
            if (isStart) { startLat = sel.lat; startLon = sel.lon; binding.tvStartLocation.text = sel.name }
            else {
                binding.tvEndLocation.text = sel.name
                endLat = sel.lat; endLon = sel.lon   // 추가
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun searchPoi(query: String): List<PoiResult> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val enc = java.net.URLEncoder.encode(query, "UTF-8")
                val conn = java.net.URL(
                    "https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword=$enc&count=7&appKey=R5NW778vk8ZdC7fek3nF3b1rFe23T21a0V5936X"
                ).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                if (conn.responseCode != 200) return@withContext emptyList()
                val pois = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONObject("searchPoiInfo").getJSONObject("pois").getJSONArray("poi")
                (0 until pois.length()).mapNotNull { i ->
                    val p = pois.getJSONObject(i)
                    val lat = p.optString("noorLat").toDoubleOrNull() ?: return@mapNotNull null
                    val lon = p.optString("noorLon").toDoubleOrNull() ?: return@mapNotNull null
                    PoiResult(p.optString("name"),
                        listOf(p.optString("upperAddrName"), p.optString("middleAddrName"), p.optString("roadName"))
                            .filter { it.isNotEmpty() }.joinToString(" "), lat, lon)
                }
            } catch (e: Exception) {
                android.util.Log.e("POI", "${e.javaClass.simpleName}: ${e.message}")
                emptyList()
            }
        }
}