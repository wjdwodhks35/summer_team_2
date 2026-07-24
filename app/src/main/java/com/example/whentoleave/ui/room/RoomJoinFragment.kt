package com.example.whentoleave.ui.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.data.model.JoinRoomRequest
import com.example.whentoleave.databinding.FragmentRoomJoinBinding
import com.example.whentoleave.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RoomJoinFragment : Fragment() {

    private var _binding: FragmentRoomJoinBinding? = null
    private val binding get() = _binding!!

    private val api by lazy { RoomApiService.create() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomJoinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirmJoin.setOnClickListener { joinRoom() }
    }

    private fun joinRoom() {
        val code = binding.etInviteCode.text.toString().trim().uppercase()
        if (code.isBlank()) { showSnack("초대 코드를 입력해주세요"); return }

        val token = TokenManager.getBearerToken(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = api.joinRoom(token, JoinRoomRequest(code))
                when {
                    resp.isSuccessful -> {
                        val body = resp.body()!!
                        val bundle = Bundle().apply {
                            putLong("roomId", body.id)
                            putString("roomName", body.name)
                            putString("inviteCode", body.code)
                            putString("destination", body.destination)
                            putString("appointmentTime", body.appointmentTime)
                        }
                        findNavController().navigate(R.id.action_roomJoin_to_dashboard, bundle)
                    }
                    resp.code() == 404 -> showSnack("방을 찾을 수 없습니다. 코드를 확인해주세요")
                    resp.code() == 401 -> showSnack("로그인이 필요합니다")
                    else               -> showSnack("참여 실패 (${resp.code()})")
                }
            } catch (e: Exception) {
                showSnack("네트워크 오류: ${e.message}")
            }
        }
    }

    private fun showSnack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}