package com.example.mygallery.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mygallery.R
import com.example.mygallery.databinding.ActivityMainBinding
import com.example.mygallery.ui.album.AlbumFragment
import com.example.mygallery.ui.photos.PhotoFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        var albumFragment = AlbumFragment()
        var photoFragment = PhotoFragment()
        // for Fragement Frame

        supportFragmentManager.beginTransaction().add(R.id.frameContainer, albumFragment).commit()


        // Bottom Navigaatio

        binding.bottomNav.setOnItemSelectedListener { menuItem: MenuItem ->

            when (menuItem.itemId) {
                R.id.album -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, albumFragment).commit()
                    true
                }

                R.id.photo -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, photoFragment).commit()
                    true
                }

                R.id.menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, albumFragment).commit()
                    true
                }

                else -> {
                    false
                }
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