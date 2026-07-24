package com.example.whentoleave.ui.common

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.whentoleave.R
import com.example.whentoleave.databinding.BottomSheetPlaceSearchBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PlaceResult(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double
)

class PlaceSearchBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaceSearchBinding? = null
    private val binding get() = _binding!!

    var onPlaceSelected: ((PlaceResult) -> Unit)? = null

    private val adapter = PlaceAdapter { place ->
        onPlaceSelected?.invoke(place)
        dismiss()
    }

    private var searchJob: Job? = null

    companion object {
        private const val TMAP_KEY = "rVhf1vi1M79depsoEfcTa6qw8vfB1yPxcWqQHtTj"

        fun newInstance() = PlaceSearchBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPlaceSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전체 높이로 확장
        dialog?.setOnShowListener {
            val bs = (view.parent as? android.view.View)?.let {
                BottomSheetBehavior.from(it)
            }
            bs?.state = BottomSheetBehavior.STATE_EXPANDED
            bs?.peekHeight = resources.displayMetrics.heightPixels
        }

        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.adapter = adapter

        binding.btnClose.setOnClickListener { dismiss() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.length < 2) {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.GONE
                    return
                }
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // 디바운스
                    val results = searchPlaces(query)
                    binding.tvEmpty.visibility =
                        if (results.isEmpty()) View.VISIBLE else View.GONE
                    adapter.submitList(results)
                }
            }
        })

        // 키보드 즉시 표시
        binding.etSearch.requestFocus()
    }

    private suspend fun searchPlaces(query: String): List<PlaceResult> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val conn = URL(
                    "https://apis.openapi.sk.com/tmap/pois?version=1" +
                            "&searchKeyword=$encoded&count=15&searchtypCd=A"
                ).openConnection() as HttpURLConnection
                conn.setRequestProperty("appKey", TMAP_KEY)
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                if (conn.responseCode != 200) return@withContext emptyList()

                val pois = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                    .optJSONObject("searchPoiInfo")
                    ?.optJSONObject("pois")
                    ?.optJSONArray("poi")
                    ?: return@withContext emptyList()

                val list = mutableListOf<PlaceResult>()
                for (i in 0 until pois.length()) {
                    val poi = pois.getJSONObject(i)
                    val name = poi.optString("name", "")
                    val lat  = poi.optString("noorLat").toDoubleOrNull() ?: continue
                    val lon  = poi.optString("noorLon").toDoubleOrNull() ?: continue
                    if (lat == 0.0 || lon == 0.0) continue

                    // 주소 조합
                    val upper  = poi.optString("upperAddrName", "")
                    val middle = poi.optString("middleAddrName", "")
                    val lower  = poi.optString("lowerAddrName", "")
                    val detail = poi.optString("detailAddrName", "")
                    val addr   = listOf(upper, middle, lower, detail)
                        .filter { it.isNotBlank() }.joinToString(" ")

                    list.add(PlaceResult(name, addr, lat, lon))
                }
                list
            } catch (e: Exception) { emptyList() }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── 내부 어댑터 ──────────────────────────────────────
    private class PlaceAdapter(
        private val onClick: (PlaceResult) -> Unit
    ) : ListAdapter<PlaceResult, PlaceAdapter.VH>(DIFF) {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView    = view.findViewById(R.id.tv_place_name)
            val tvAddress: TextView = view.findViewById(R.id.tv_place_address)
            fun bind(place: PlaceResult) {
                tvName.text    = place.name
                tvAddress.text = place.address
                itemView.setOnClickListener { onClick(place) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_place_result, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(getItem(position))

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<PlaceResult>() {
                override fun areItemsTheSame(a: PlaceResult, b: PlaceResult) =
                    a.lat == b.lat && a.lon == b.lon
                override fun areContentsTheSame(a: PlaceResult, b: PlaceResult) = a == b
            }
        }
    }
}