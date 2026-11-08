package com.kafi.utilityhub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kafi.utilityhub.databinding.ActivityMainBinding
import com.kafi.utilityhub.ui.HistoryFragment
import com.kafi.utilityhub.ui.HomeFragment
import com.kafi.utilityhub.ui.SettingsFragment
import com.kafi.utilityhub.ui.ToolsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { show(HomeFragment()); true }
                R.id.nav_tools -> { show(ToolsFragment()); true }
                R.id.nav_history -> { show(HistoryFragment()); true }
                R.id.nav_settings -> { show(SettingsFragment()); true }
                else -> false
            }
        }

        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
