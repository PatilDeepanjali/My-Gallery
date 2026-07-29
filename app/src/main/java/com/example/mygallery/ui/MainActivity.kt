package com.example.mygallery.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.mygallery.R
import com.example.mygallery.databinding.ActivityMainBinding
import com.example.mygallery.ui.album.AlbumFragment

class MainActivity : AppCompatActivity() {



        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

     var binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        var albumFragment = AlbumFragment()
        // for Fragement Frame

        supportFragmentManager.beginTransaction().add(R.id.frameContainer, albumFragment).commit()


        // Bottom Navigaatio

        binding.bottomNav.setOnItemSelectedListener { menuItem: MenuItem ->

            when (menuItem.itemId) {
                R.id.album -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.frameContainer, albumFragment).commit()
                    true
                }

                R.id.photo -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.frameContainer, albumFragment).commit()
                    true
                }

                R.id.menu -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.frameContainer, albumFragment).commit()
                    true
                }

                else -> {
                    false
                }
            }

        }


    }
}