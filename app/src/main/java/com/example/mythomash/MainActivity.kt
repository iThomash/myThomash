package com.example.mythomash

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mythomash.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import java.lang.Math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var shakeDetector: FlashlightShakeDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        gestureDetector = GestureDetector(this, SwipeGestureListener())
        shakeDetector = FlashlightShakeDetector(this)

        // Attach touch listener to the root view
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        setSupportActionBar(binding.appBarMain.toolbar)

        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configure AppBar and Navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_info, R.id.media_backup, R.id.sensor_logger_layout),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stop()
    }

    // Gesture listener to detect swipes
    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100 // Minimum distance for a swipe
        private val swipeVelocityThreshold = 100 // Minimum velocity for a swipe

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - (e1?.x ?: 0f)
            val diffY = e2.y - (e1?.y ?: 0f)

            // Check if it's a right swipe
            if (abs(diffX) > abs(diffY) &&
                abs(diffX) > swipeThreshold &&
                abs(velocityX) > swipeVelocityThreshold
            ) {
                if (diffX > 0) {
                    // Swipe right: Open the drawer
                    drawerLayout.openDrawer(GravityCompat.START)
                    return true
                }
            }
            return false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}