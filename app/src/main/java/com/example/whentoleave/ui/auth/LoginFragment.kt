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
import com.example.whentoleave.data.model.LoginRequest
import com.example.whentoleave.databinding.FragmentLoginBinding
import com.example.whentoleave.util.TokenManager
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 로그인 버튼
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validate(email, password)) login(email, password)
        }

        // 게스트 로그인
        binding.btnGuest.setOnClickListener {
            TokenManager.saveGuest(requireContext())
            goHome()
        }

        // 회원가입 이동
        binding.tvGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun validate(email: String, password: String): Boolean {
        hideError()
        if (email.isEmpty()) {
            binding.tilEmail.error = "이메일을 입력해주세요"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "올바른 이메일 형식이 아닙니다"
            return false
        }
        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = "비밀번호를 입력해주세요"
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "비밀번호는 6자 이상이어야 합니다"
            return false
        }
        binding.tilPassword.error = null
        return true
    }

    private fun login(email: String, password: String) {
        setLoading(true)
        val api = RetrofitClient.instance.create(AuthApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    TokenManager.saveToken(requireContext(), body.accessToken)
                    TokenManager.saveUserInfo(requireContext(), body.userId, body.nickname)
                    goHome()
                } else {
                    showError("이메일 또는 비밀번호가 올바르지 않습니다")
                }
            } catch (e: Exception) {
                showError("서버 연결에 실패했습니다. 네트워크를 확인해주세요")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun goHome() {
        findNavController().navigate(R.id.action_login_to_home)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGuest.isEnabled = !isLoading
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