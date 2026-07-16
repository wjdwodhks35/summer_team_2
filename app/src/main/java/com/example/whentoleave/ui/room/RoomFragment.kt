package com.example.whentoleave.ui.room

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.data.model.CreateRoomRequest
import com.example.whentoleave.data.model.JoinRoomRequest
import com.example.whentoleave.databinding.FragmentRoomBinding
import com.example.whentoleave.util.TokenManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.UUID

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

        setupCodeInputs()

        // 참여하기
        binding.btnJoinRoom.setOnClickListener {
            val code = getEnteredCode()
            if (code.length == 6) joinRoom(code)
            else showSnackbar("초대코드 6자리를 입력해주세요")
        }

        // 방 만들기
        binding.btnCreateRoom.setOnClickListener {
            val title = binding.etRoomTitle.text.toString().trim()
            if (title.isEmpty()) {
                binding.tilRoomTitle.error = "방 이름을 입력해주세요"
                return@setOnClickListener
            }
            binding.tilRoomTitle.error = null
            showDestinationDialog(title)
        }
    }

    // 방 만들기 클릭 시 목적지 입력 다이얼로그
    private fun showDestinationDialog(title: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)

        // TextInputLayout + EditText 직접 생성
        val til = TextInputLayout(requireContext()).apply {
            hint = "공통 목적지 (예: 강남역 2번 출구)"
            setPadding(48, 16, 48, 0)
        }
        val et = TextInputEditText(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        til.addView(et)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("공통 목적지 설정")
            .setMessage("모든 멤버가 향할 목적지를 입력해주세요")
            .setView(til)
            .setPositiveButton("방 만들기") { _, _ ->
                val destination = et.text.toString().trim()
                createRoom(title, destination)
            }
            .setNegativeButton("나중에 설정") { _, _ ->
                createRoom(title, "")
            }
            .show()
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
        val api = RetrofitClient.instance.create(RoomApiService::class.java)
        val guestToken = if (TokenManager.isGuest(requireContext())) UUID.randomUUID().toString() else null

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.joinRoom(JoinRoomRequest(code, guestToken))
                if (response.isSuccessful) {
                    val room = response.body()!!
                    val bundle = Bundle().apply { putLong("roomId", room.roomId) }
                    findNavController().navigate(R.id.action_room_to_dashboard, bundle)
                } else {
                    showSnackbar("유효하지 않은 초대코드입니다")
                }
            } catch (e: Exception) {
                showSnackbar("서버 연결에 실패했습니다")
            }
        }
    }

    private fun createRoom(title: String, destination: String) {
        val api = RetrofitClient.instance.create(RoomApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.createRoom(CreateRoomRequest(title, destination, ""))
                if (response.isSuccessful) {
                    val room = response.body()!!
                    val bundle = Bundle().apply { putLong("roomId", room.roomId) }
                    findNavController().navigate(R.id.action_room_to_dashboard, bundle)
                } else {
                    showSnackbar("방 만들기에 실패했습니다")
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