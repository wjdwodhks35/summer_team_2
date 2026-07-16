package com.example.whentoleave.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.databinding.FragmentAccountBinding
import com.example.whentoleave.util.TokenManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    // 위치 권한 요청
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            binding.switchLocation.isChecked = true
            binding.tvLocationStatus.text = "켜짐 · 방 멤버에게 공유 중"
            Snackbar.make(binding.root, "위치 공유가 시작되었습니다", Snackbar.LENGTH_SHORT).show()
        } else {
            binding.switchLocation.isChecked = false
            binding.tvLocationStatus.text = "꺼짐"
            Snackbar.make(binding.root, "위치 권한이 필요합니다", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 사용자 정보 표시
        val nickname = TokenManager.getNickname(requireContext()) ?: "게스트"
        val isGuest = TokenManager.isGuest(requireContext())
        binding.tvNickname.text = nickname
        binding.tvEmail.text = if (isGuest) "게스트 사용자" else "ID: ${TokenManager.getUserId(requireContext())}"
        binding.tvAvatar.text = nickname.take(1)

        // 위치 공유 스위치
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestLocation()
            } else {
                binding.tvLocationStatus.text = "꺼짐"
                Snackbar.make(binding.root, "위치 공유가 중지되었습니다", Snackbar.LENGTH_SHORT).show()
            }
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠어요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃") { _, _ ->
                    TokenManager.clearAll(requireContext())
                    findNavController().navigate(
                        R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                }
                .show()
        }
    }

    private fun checkAndRequestLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            binding.tvLocationStatus.text = "켜짐 · 방 멤버에게 공유 중"
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}