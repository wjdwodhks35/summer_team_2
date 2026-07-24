package com.example.whentoleave.ui.room

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.data.api.RoomApiService
import com.example.whentoleave.data.model.CreateRoomRequest
import com.example.whentoleave.databinding.FragmentRoomCreateBinding
import com.example.whentoleave.ui.common.PlaceSearchBottomSheet
import com.example.whentoleave.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class RoomCreateFragment : Fragment() {

    private var _binding: FragmentRoomCreateBinding? = null
    private val binding get() = _binding!!

    private val api by lazy { RoomApiService.create() }

    private var destLat = 0.0
    private var destLon = 0.0
    private var destAddress = ""
    private var selectedDate = ""   // "yyyy-MM-dd"
    private var selectedTime = ""   // "HH:mm"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 목적지 검색
        binding.cardDestination.setOnClickListener {
            PlaceSearchBottomSheet.newInstance().also { sheet ->
                sheet.onPlaceSelected = { place ->
                    destLat = place.lat
                    destLon = place.lon
                    destAddress = place.name
                    binding.tvDestination.text = place.name
                    binding.tvDestination.setTextColor(
                        resources.getColor(R.color.navy, null)
                    )
                }
                sheet.show(childFragmentManager, "dest_search")
            }
        }

        // 날짜 선택
        binding.cardDate.setOnClickListener { showDatePicker() }

        // 시간 선택
        binding.cardTime.setOnClickListener { showTimePicker() }

        // 방 만들기
        binding.btnConfirmCreate.setOnClickListener { createRoom() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                val display = "${month + 1}월 ${day}일 (${getDayOfWeek(year, month, day)})"
                binding.tvDate.text = display
                binding.tvDate.setTextColor(resources.getColor(R.color.navy, null))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                val ampm = if (hour < 12) "오전" else "오후"
                val h = if (hour > 12) hour - 12 else hour
                binding.tvTime.text = "$ampm ${h}:${String.format("%02d", minute)}"
                binding.tvTime.setTextColor(resources.getColor(R.color.navy, null))
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        ).show()
    }

    private fun getDayOfWeek(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }

    private fun createRoom() {
        val name = binding.etRoomName.text.toString().trim()
        when {
            name.isBlank()       -> { showSnack("방 이름을 입력해주세요"); return }
            destAddress.isBlank() -> { showSnack("목적지를 선택해주세요"); return }
            selectedDate.isBlank() -> { showSnack("날짜를 선택해주세요"); return }
            selectedTime.isBlank() -> { showSnack("시간을 선택해주세요"); return }
        }

        val token = TokenManager.getBearerToken(requireContext())
        val appointmentDateTime = "$selectedDate $selectedTime"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = api.createRoom(
                    token,
                    CreateRoomRequest(
                        name = name,
                        destination = destAddress,
                        destLat = destLat,
                        destLon = destLon,
                        appointmentTime = appointmentDateTime
                    )
                )
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    // 초대 코드 다이얼로그 표시
                    showCodeDialog(body.id, body.name, body.code, body.destination, body.appointmentTime)
                } else {
                    showSnack(if (resp.code() == 401) "로그인이 필요합니다" else "방 생성 실패 (${resp.code()})")
                }
            } catch (e: Exception) {
                showSnack("네트워크 오류: ${e.message}")
            }
        }
    }

    private fun showCodeDialog(
        roomId: Long, roomName: String, code: String,
        destination: String, appointmentTime: String
    ) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🎉 방이 생성됐어요!")
            .setMessage("방 이름: $roomName\n목적지: $destination\n약속 시간: $appointmentTime\n\n초대 코드\n\n$code\n\n친구에게 코드를 공유하세요")
            .setPositiveButton("입장하기") { _, _ ->
                navigateToDashboard(roomId, roomName, code, destination, appointmentTime)
            }
            .setNeutralButton("코드 복사 후 입장") { _, _ ->
                copyToClipboard(code)
                navigateToDashboard(roomId, roomName, code, destination, appointmentTime)
            }
            .setCancelable(false)
            .show()
    }

    private fun copyToClipboard(code: String) {
        val cm = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("invite_code", code))
        showSnack("코드 복사됨: $code")
    }

    private fun navigateToDashboard(
        roomId: Long, roomName: String, code: String,
        destination: String, appointmentTime: String
    ) {
        val myNickname = TokenManager.getNickname(requireContext()) ?: ""
        val bundle = Bundle().apply {
            putLong("roomId", roomId)
            putString("roomName", roomName)
            putString("inviteCode", code)
            putString("destination", destination)
            putString("appointmentTime", appointmentTime)
            putString("hostName", myNickname)   // 방 만든 사람 = 방장
        }
        findNavController().navigate(R.id.action_roomCreate_to_dashboard, bundle)
    }

    private fun showSnack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}