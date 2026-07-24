package com.example.whentoleave.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.whentoleave.R
import com.example.whentoleave.data.api.AuthApiService
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.data.model.RegisterRequest
import com.example.whentoleave.databinding.FragmentRegisterBinding
import com.example.whentoleave.util.TokenManager
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로 가기
        binding.tvBack.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        // 가입하기 버튼
        binding.btnRegister.setOnClickListener {
            val nickname = binding.etNickname.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()
            if (validate(nickname, email, password, passwordConfirm)) {
                register(nickname, email, password)
            }
        }
    }

    private fun validate(
        nickname: String, email: String,
        password: String, passwordConfirm: String
    ): Boolean {
        hideError()

        if (nickname.isEmpty()) {
            binding.tilNickname.error = "닉네임을 입력해주세요"
            return false
        }
        binding.tilNickname.error = null

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "올바른 이메일을 입력해주세요"
            return false
        }
        binding.tilEmail.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "비밀번호는 6자 이상이어야 합니다"
            return false
        }
        binding.tilPassword.error = null

        if (password != passwordConfirm) {
            binding.tilPasswordConfirm.error = "비밀번호가 일치하지 않습니다"
            return false
        }
        binding.tilPasswordConfirm.error = null

        return true
    }

    private fun register(nickname: String, email: String, password: String) {
        setLoading(true)
        val api = RetrofitClient.instance.create(AuthApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.register(RegisterRequest(email, password, nickname))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    TokenManager.saveToken(requireContext(), body.accessToken)
                    TokenManager.saveUserInfo(requireContext(), body.userId, body.nickname)
                    findNavController().navigate(R.id.action_register_to_home)
                } else {
                    when (response.code()) {
                        409 -> showError("이미 사용 중인 이메일입니다")
                        else -> showError("서버 오류가 발생했습니다 (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                showError("서버 연결에 실패했습니다")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}