package com.example.mygallery.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mygallery.R
import com.example.mygallery.databinding.ActivityMainBinding
import com.example.mygallery.ui.album.AlbumFragment
import com.example.mygallery.ui.photo.PhotoFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only show AlbumFragment the FIRST time Activity is created.
        // After rotation, Android restores the existing Fragment.
        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.frameContainer,
                    AlbumFragment()
                )
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { menuItem: MenuItem ->

            when (menuItem.itemId) {

                R.id.album -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.frameContainer,
                            AlbumFragment()
                        )
                        .commit()

                    true
                }

                R.id.photo -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.frameContainer,
                            PhotoFragment()
                        )
                        .commit()

                    true
                }

                R.id.menu -> {

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.frameContainer,
                            AlbumFragment()
                        )
                        .commit()

                    true
                }

                else -> false
            }
        }
    }

    fun hideBottomNavigation() {
        binding.bottomNav.visibility = View.GONE
    }

    fun showBottomNavigation() {
        binding.bottomNav.visibility = View.VISIBLE
    }
}