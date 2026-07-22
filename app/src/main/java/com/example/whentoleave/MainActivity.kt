package com.example.whentoleave

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.whentoleave.data.api.RetrofitClient
import com.example.whentoleave.databinding.ActivityMainBinding
import com.example.whentoleave.util.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

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

        // 탭 포커스 버그 수정 — setupWithNavController 대신 수동 처리
        setupBottomNav()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in hideNavFragments) View.GONE else View.VISIBLE
        }

        // 로그인 여부에 따라 분기
        if (TokenManager.isLoggedIn(this)) {
            navController.navigate(R.id.homeFragment)
        }
        // nav_graph startDestination이 loginFragment면 비로그인 시 자동으로 거기 머무름
    }

    private fun setupBottomNav() {
        val navOpts = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> navController.navigate(R.id.homeFragment, null, navOpts)
                R.id.transitFragment -> navController.navigate(R.id.transitFragment, null, navOpts)
                R.id.savedFragment -> navController.navigate(R.id.savedFragment, null, navOpts)
                R.id.roomFragment -> navController.navigate(R.id.roomFragment, null, navOpts)
                R.id.accountFragment -> navController.navigate(R.id.accountFragment, null, navOpts)
            }
            true
        }
    }
}