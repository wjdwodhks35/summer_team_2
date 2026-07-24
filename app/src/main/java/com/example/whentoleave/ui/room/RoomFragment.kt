package com.example.whentoleave.ui.room

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.data.model.JoinRoomRequest
import com.example.whentoleave.databinding.FragmentRoomBinding
import com.example.whentoleave.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RoomFragment : Fragment() {

    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 탭 전환 후 복귀 시 저장된 방 세션 복원
        val savedRoomId = TokenManager.getCurrentRoomId(requireContext())
        if (savedRoomId > 0L) {
            val bundle = Bundle().apply {
                putLong("roomId", savedRoomId)
                putString("inviteCode", TokenManager.getCurrentRoomInvite(requireContext()))
            }
            findNavController().navigate(R.id.action_room_to_dashboard, bundle)
            return
        }

        setupCodeInputs()

        // 참여하기
        binding.btnJoinRoom.setOnClickListener {
            val code = getEnteredCode()
            if (code.length == 6) joinRoom(code)
            else showSnackbar("초대코드 6자리를 입력해주세요")
        }

        // 방 만들기 → 전용 화면으로 이동
        binding.btnCreateRoom.setOnClickListener {
            findNavController().navigate(R.id.action_room_to_create)
        }
    }

    private fun setupCodeInputs() {
        val inputs = listOf(
            binding.code1, binding.code2, binding.code3,
            binding.code4, binding.code5, binding.code6
        )
        inputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < inputs.size - 1) {
                        inputs[index + 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun getEnteredCode(): String {
        return listOf(
            binding.code1, binding.code2, binding.code3,
            binding.code4, binding.code5, binding.code6
        ).joinToString("") { it.text.toString() }
    }

    private fun joinRoom(code: String) {
        val api = RoomApiService.create()
        val token = TokenManager.getBearerToken(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.joinRoom(token, JoinRoomRequest(code))
                if (response.isSuccessful) {
                    val room = response.body()!!
                    val bundle = Bundle().apply {
                        putLong("roomId", room.id)
                        putString("inviteCode", room.code ?: "")
                        putString("destination", room.destination ?: "")
                        putString("appointmentTime", room.appointmentTime ?: "")
                        putString("hostName", room.hostName ?: "")
                    }
                    findNavController().navigate(R.id.action_room_to_dashboard, bundle)
                } else {
                    showSnackbar("유효하지 않은 초대코드입니다")
                }
            } catch (e: Exception) {
                showSnackbar("서버 연결에 실패했습니다")
            }
        }
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}