package com.example.whentoleave.ui.room

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.data.model.LocationUpdateRequest
import com.example.whentoleave.data.model.RoomMember
import com.example.whentoleave.databinding.FragmentRoomDashboardBinding
import com.example.whentoleave.databinding.ItemRoomMemberBinding
import com.example.whentoleave.ui.common.PlaceSearchBottomSheet
import com.example.whentoleave.util.NotificationHelper
import com.example.whentoleave.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RoomDashboardFragment : Fragment() {

    private var _binding: FragmentRoomDashboardBinding? = null
    private val binding get() = _binding!!

    private val api by lazy { RoomApiService.create() }

    private var roomId = 0L
    private var inviteCode = ""
    private var roomName = ""
    private var destinationName = ""
    private var appointmentTimeStr = ""
    private var destLat = 0.0
    private var destLon = 0.0

    private var arrivalNotifSent = false

    private lateinit var memberAdapter: MemberAdapter
    private var pollingJob: Job? = null
    private var currentMembers: List<RoomMember> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomId = arguments?.getLong("roomId") ?: 0L
        inviteCode = arguments?.getString("inviteCode") ?: ""
        roomName = arguments?.getString("roomName") ?: ""
        destinationName = arguments?.getString("destination") ?: ""
        appointmentTimeStr = arguments?.getString("appointmentTime") ?: ""

        // 탭 전환해도 방에 머물도록 현재 방 저장
        if (roomId > 0L) {
            TokenManager.saveCurrentRoom(requireContext(), roomId, inviteCode)
        }

        // 뒤로가기 → 방 세션 클리어 후 RoomFragment로 복귀
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    TokenManager.clearCurrentRoom(requireContext())
                    isEnabled = false
                    findNavController().popBackStack()
                }
            }
        )

        binding.tvDestination.text = destinationName
        binding.tvAppointmentTime.text = appointmentTimeStr
        binding.tvInviteCode.text = inviteCode
        val initHostName = arguments?.getString("hostName") ?: ""
        if (initHostName.isNotBlank()) binding.tvHostName.text = "방장 $initHostName"

        binding.btnCopyCode.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("invite_code", inviteCode))
            showSnack("초대 코드 복사됨: $inviteCode")
        }

        binding.cardInvite.setOnClickListener {
            showSnack("초대 코드: $inviteCode  (복사하기 버튼으로 복사하세요)")
        }

        binding.btnShowMap.setOnClickListener { openMapScreen() }

        val myUserId = TokenManager.getUserId(requireContext())
        memberAdapter = MemberAdapter(myUserId) { showDepartureSelector() }
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembers.adapter = memberAdapter

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3002)
            }
        }
        NotificationHelper.createChannel(requireContext())

        startPolling()
    }

    // ── 방 정보 30초 폴링 ────────────────────────────────
    private fun startPolling() {
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                loadRoomInfo()
                delay(30_000L)
            }
        }
    }

    private suspend fun loadRoomInfo() {
        try {
            val token = TokenManager.getBearerToken(requireContext())
            val resp = api.getRoomInfo(token, roomId)
            if (!resp.isSuccessful) {
                if (resp.code() == 404) {
                    // 방이 삭제됐으면 세션 클리어 후 목록으로
                    TokenManager.clearCurrentRoom(requireContext())
                    findNavController().popBackStack()
                }
                return
            }
            if (resp.isSuccessful) {
                val body = resp.body() ?: return
                inviteCode = body.code
                roomName = body.name
                destinationName = body.destination
                appointmentTimeStr = body.appointmentTime
                destLat = body.destLat
                destLon = body.destLon
                currentMembers = body.members

                binding.tvInviteCode.text = body.code
                binding.tvDestination.text = body.destination
                binding.tvAppointmentTime.text = body.appointmentTime
                binding.tvHostName.text = "방장 ${body.hostName}"
                binding.tvMemberCount.text = "${body.memberCount}명"
                memberAdapter.submitList(body.members)

                checkArrivalNotification()
            }
        } catch (_: Exception) { }
    }

    // ── 도착 5분 전 알림 ─────────────────────────────────
    private fun checkArrivalNotification() {
        if (arrivalNotifSent || appointmentTimeStr.isBlank()) return
        try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val appointmentTime = LocalDateTime.parse(appointmentTimeStr, fmt)
            val minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), appointmentTime)
            if (minutesLeft in 0..5) {
                arrivalNotifSent = true
                NotificationHelper.showArrivalSoonNotification(
                    requireContext(), roomName, destinationName, roomId.toInt()
                )
                showSnack("⏰ $destinationName 도착 ${minutesLeft}분 전입니다!")
            }
        } catch (_: Exception) { }
    }

    // ── 출발지 설정 (장소 검색만 사용) ───────────────────
    private fun showDepartureSelector() {
        PlaceSearchBottomSheet.newInstance().also { sheet ->
            sheet.onPlaceSelected = { place ->
                uploadLocation(place.lat, place.lon, place.name)
            }
            sheet.show(childFragmentManager, "departure_search")
        }
    }

    private fun uploadLocation(lat: Double, lon: Double, address: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = TokenManager.getBearerToken(requireContext())
                val response = api.updateLocation(
                    token, roomId,
                    LocationUpdateRequest(lat = lat, lon = lon, address = address, isShared = true)
                )
                if (response.isSuccessful) {
                    showSnack("✅ 출발지 설정: $address")
                    loadRoomInfo()
                } else {
                    showSnack("출발지 설정 실패 (${response.code()}). 다시 시도해 주세요.")
                }
            } catch (e: Exception) {
                showSnack("네트워크 오류: ${e.message}")
            }
        }
    }

    // ── 지도 화면 열기 ────────────────────────────────────
    private fun openMapScreen() {
        if (destLat == 0.0 && destLon == 0.0) {
            showSnack("목적지 좌표 정보가 없습니다. 방을 새로 만들어 테스트해 주세요.")
            return
        }
        val membersArray = JSONArray()
        for (m in currentMembers) {
            if (m.isLocationShared && m.departureLat != null && m.departureLon != null) {
                membersArray.put(JSONObject().apply {
                    put("name", m.name)
                    put("lat", m.departureLat)
                    put("lon", m.departureLon)
                    put("isHost", m.isHost)
                })
            }
        }
        val intent = Intent(requireContext(), RoomMapActivity::class.java).apply {
            putExtra("roomName", roomName)
            putExtra("destLat", destLat)
            putExtra("destLon", destLon)
            putExtra("destName", destinationName)
            putExtra("members", membersArray.toString())
        }
        startActivity(intent)
    }

    private fun showSnack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        pollingJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}

// ── 멤버 어댑터 ──────────────────────────────────────────
class MemberAdapter(
    private val myUserId: Long,
    private val onMyCardClick: () -> Unit
) : ListAdapter<RoomMember, MemberAdapter.VH>(DIFF) {

    private val AVATAR_COLORS = listOf(
        "#1B1F3B", "#3B82F6", "#10B981", "#F59E0B",
        "#EF4444", "#8B5CF6", "#EC4899", "#06B6D4"
    )

    inner class VH(val b: ItemRoomMemberBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemRoomMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        val b = holder.b
        val isMe = m.userId == myUserId

        // 아바타
        val colorHex = AVATAR_COLORS[position % AVATAR_COLORS.size]
        try {
            val bg = b.tvAvatar.background.mutate() as android.graphics.drawable.GradientDrawable
            bg.setColor(Color.parseColor(colorHex))
        } catch (_: Exception) { }
        b.tvAvatar.text = m.name.take(1)

        // 이름
        b.tvMemberName.text = m.name
        b.tvHostBadge.visibility = if (m.isHost) View.VISIBLE else View.GONE
        b.tvMeBadge.visibility   = if (isMe) View.VISIBLE else View.GONE

        // 출발지 — 내 카드이고 미설정이면 안내 문구
        b.tvDepartureAddress.text = when {
            m.isLocationShared && !m.departureAddress.isNullOrBlank() -> m.departureAddress
            isMe && !m.isLocationShared -> "📍 탭해서 출발지를 설정하세요"
            else -> "위치 비공개"
        }
        b.tvDepartureAddress.setTextColor(
            if (isMe && !m.isLocationShared)
                android.graphics.Color.parseColor("#3B82F6")   // 파란색 CTA
            else
                android.graphics.Color.parseColor("#555555")
        )

        // 예상 소요 시간
        b.tvTravelTime.text = when {
            !m.isLocationShared -> "--:--"
            m.estimatedTravelMin != null -> {
                val h = m.estimatedTravelMin / 60
                val min = m.estimatedTravelMin % 60
                if (h > 0) "${h}시간 ${min}분" else "${min}분"
            }
            !m.recommendedDepartureTime.isNullOrBlank() -> m.recommendedDepartureTime
            else -> "--:--"
        }

        // 내 카드 탭 → 출발지 검색
        if (isMe) {
            b.root.setOnClickListener { onMyCardClick() }
            b.root.isClickable = true
        } else {
            b.root.setOnClickListener(null)
            b.root.isClickable = false
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RoomMember>() {
            override fun areItemsTheSame(a: RoomMember, b: RoomMember) = a.userId == b.userId
            override fun areContentsTheSame(a: RoomMember, b: RoomMember) = a == b
        }
    }
}
