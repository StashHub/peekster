package com.assoft.peekster.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.assoft.peekster.R
import com.assoft.peekster.databinding.ActivitySplashBinding
import com.assoft.peekster.di.injectFeature
import com.assoft.peekster.util.contentView
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import java.util.*

private const val PERMISSION_DELAY = 2_000L
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

/**
 * A 'trampoline' activity to determine where the app should
 * go on app launch
 */
class SplashActivity : AppCompatActivity() {

    /** Reference to the [ActivitySplashBinding] binding layout */
    private val binding: ActivitySplashBinding by contentView(R.layout.activity_splash)

    /** Handler for the current thread used to advance the view pager. */
    private val handler = Handler()

    /** Auto-advance the view pager to give overview of app benefits. */
    private val permissionHandler: Runnable = object : Runnable {
        override fun run() {
            requestPermission()
            handler.removeCallbacks(this)
        }
    }

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectFeature()
        binding.lifecycleOwner = this

        if (!hasPermissions(this)) {
            handler.postDelayed(permissionHandler, PERMISSION_DELAY)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Request WRITE_EXTERNAL_STORAGE permission and navigate to MainActivity if
     * granted.
     */
    private fun requestPermission() {
        Permissions.check(
            this,
            PERMISSIONS_REQUIRED,
            null /*rationale*/,
            null /*options*/,
            object : PermissionHandler() {
                override fun onGranted() {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onDenied(
                    context: Context,
                    deniedPermissions: ArrayList<String>
                ) {
                    handler.postDelayed(permissionHandler, 0)
                }
            })
    }

    companion object {
        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}