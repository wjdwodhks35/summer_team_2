package com.example.whentoleave.ui.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whentoleave.R
import com.example.whentoleave.data.local.AppDatabase
import com.example.whentoleave.data.local.SavedRoute
import com.example.whentoleave.databinding.FragmentSavedBinding
import com.example.whentoleave.ui.common.PlaceSearchBottomSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SavedRouteAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        adapter = SavedRouteAdapter(
            onGoClick   = { route -> navigateToResult(route) },
            onLongClick = { route -> confirmDelete(route) }
        )
        binding.rvSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSaved.adapter = adapter

        db.savedRouteDao().getAll().observe(viewLifecycleOwner) { routes ->
            adapter.submitList(routes)
            binding.tvEmpty.visibility = if (routes.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    // ── 경로 추가 다이얼로그 (PlaceSearchBottomSheet 연동) ──
    private fun showAddDialog() {
        val dp  = requireContext().resources.displayMetrics.density
        val pad = (16 * dp).toInt()

        val etLabel   = EditText(requireContext()).apply { hint = "경로 이름 (예: 집→학교)" }
        val tvStart   = TextView(requireContext()).apply {
            text = "출발지: 선택 안 됨"; setTextColor(0xFF999999.toInt())
        }
        val tvEnd     = TextView(requireContext()).apply {
            text = "목적지: 선택 안 됨"; setTextColor(0xFF999999.toInt())
        }
        val btnStart  = android.widget.Button(requireContext()).apply { text = "출발지 검색" }
        val btnEnd    = android.widget.Button(requireContext()).apply { text = "목적지 검색" }

        var startAddress = ""
        var endAddress   = ""

        btnStart.setOnClickListener {
            PlaceSearchBottomSheet.newInstance().also { sheet ->
                sheet.onPlaceSelected = { place ->
                    startAddress = place.name
                    tvStart.text = "출발지: ${place.name}"
                    tvStart.setTextColor(0xFF1B1F3B.toInt())
                }
                sheet.show(childFragmentManager, "search_start")
            }
        }

        btnEnd.setOnClickListener {
            PlaceSearchBottomSheet.newInstance().also { sheet ->
                sheet.onPlaceSelected = { place ->
                    endAddress = place.name
                    tvEnd.text = "목적지: ${place.name}"
                    tvEnd.setTextColor(0xFF1B1F3B.toInt())
                }
                sheet.show(childFragmentManager, "search_end")
            }
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(etLabel)
            addView(btnStart)
            addView(tvStart)
            addView(btnEnd)
            addView(tvEnd)
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (8 * dp).toInt() }
        listOf(etLabel, btnStart, tvStart, btnEnd, tvEnd).forEach { it.layoutParams = lp }

        AlertDialog.Builder(requireContext())
            .setTitle("경로 저장")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                val label = etLabel.text.toString().trim()
                when {
                    label.isBlank()        -> showSnack("경로 이름을 입력해주세요")
                    startAddress.isBlank() -> showSnack("출발지를 선택해주세요")
                    endAddress.isBlank()   -> showSnack("목적지를 선택해주세요")
                    else -> viewLifecycleOwner.lifecycleScope.launch {
                        db.savedRouteDao().insert(
                            SavedRoute(label = label, startAddress = startAddress, endAddress = endAddress)
                        )
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 삭제 확인 ────────────────────────────────────────
    private fun confirmDelete(route: SavedRoute) {
        AlertDialog.Builder(requireContext())
            .setTitle("경로 삭제")
            .setMessage("\"${route.label}\" 경로를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { db.savedRouteDao().delete(route) }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 길찾기 → 결과 화면 (대중교통 탭 포커스) ─────────
    private fun navigateToResult(route: SavedRoute) {
        val bundle = Bundle().apply {
            putString("startAddress", route.startAddress)
            putString("endAddress",   route.endAddress)
            putString("targetDate",   java.time.LocalDate.now().toString())
            putString("targetTime",
                java.time.LocalTime.now().plusHours(1).withMinute(0)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
            putString("timeType",  "ARRIVAL")
            putString("purpose",   "GENERAL")
            putDouble("startLat",  0.0)
            putDouble("startLon",  0.0)
            putDouble("endLat",    0.0)
            putDouble("endLon",    0.0)
        }
        findNavController().navigate(R.id.action_saved_to_result, bundle)
        // 대중교통 탭 시각적 포커스
        activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
            ?.menu?.findItem(R.id.transitFragment)?.isChecked = true
    }

    private fun showSnack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}