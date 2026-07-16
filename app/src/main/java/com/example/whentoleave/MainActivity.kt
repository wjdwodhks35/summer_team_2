package com.example.whentoleave

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.databinding.ActivityMainBinding
import com.example.whentoleave.util.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // 바텀 네비게이션 숨길 화면 목록
    private val hideNavFragments = setOf(
        R.id.loginFragment,
        R.id.registerFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 바텀 네비게이션 연결
        binding.bottomNav.setupWithNavController(navController)

        // 로그인/회원가입 화면에서 바텀 네비게이션 숨기기
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hideNavFragments) {
                binding.bottomNav.visibility = View.GONE
            } else {
                binding.bottomNav.visibility = View.VISIBLE
            }
        }

        // 이미 로그인됐으면 바로 홈으로
        if (TokenManager.isLoggedIn(this)) {
            navController.navigate(R.id.homeFragment)
        }
    }
}