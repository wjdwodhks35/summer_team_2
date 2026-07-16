package com.example.whentoleave.ui.room

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.databinding.FragmentRoomDashboardBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RoomDashboardFragment : Fragment() {

    private var _binding: FragmentRoomDashboardBinding? = null
    private val binding get() = _binding!!

    private var roomId: Long = -1L
    private var inviteCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomId = arguments?.getLong("roomId") ?: -1L

        // 초대코드 복사
        binding.tvCopyCode.setOnClickListener {
            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("초대코드", inviteCode))
            Snackbar.make(binding.root, "초대코드가 복사되었습니다", Snackbar.LENGTH_SHORT).show()
        }

        if (roomId != -1L) loadRoomData()
    }

    private fun loadRoomData() {
        val api = RetrofitClient.instance.create(RoomApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.getRoomMembers(roomId)
                if (response.isSuccessful) {
                    val members = response.body() ?: emptyList()
                    binding.tvMemberCount.text = "${members.size}명"
                    val readyCount = members.count { it.isReady }
                    binding.tvRoomStatus.text = "${readyCount}명 준비중"
                }
            } catch (e: Exception) {
                // 더미 표시 유지
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}