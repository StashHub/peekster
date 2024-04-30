package com.assoft.peekster.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.assoft.peekster.R
import com.assoft.peekster.adapter.DynamicViewPagerAdapter
import com.assoft.peekster.databinding.ActivityWelcomeBinding
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.AppUtils
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.startActivity
import com.google.android.material.button.MaterialButton

open class WelcomeActivity : Activity() {

    /** Internal variable for obtaining the [ActivityWelcomeBinding] binding. */
    private val binding: ActivityWelcomeBinding by contentView(R.layout.activity_welcome)

    /**
     * Internal reference to the splash view [ViewGroup]
     */
    private lateinit var splashView: ViewGroup

    /**
     * Internal reference to the profile view [ViewGroup]
     */
    private lateinit var profileView: ViewGroup

    /**
     * Internal reference to the permission view [ViewGroup]
     */
    private lateinit var permissionView: ViewGroup

    private val pagerAdapter: DynamicViewPagerAdapter = DynamicViewPagerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skipPermissionRequest = true
        welcomePageDisallowed = true

        splashView = layoutInflater.inflate(R.layout.activity_splash, null, false) as ViewGroup
        pagerAdapter.addView(splashView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionView =
                layoutInflater.inflate(R.layout.activity_permissions, null, false) as ViewGroup
            pagerAdapter.addView(permissionView)

            permissionView.findViewById<MaterialButton>(R.id.setup_button).setOnClickListener {
                requestRequiredPermissions(false)
            }
        }

        profileView =
            layoutInflater.inflate(R.layout.activity_default_device, null, false) as ViewGroup
        pagerAdapter.addView(profileView)
        setUserProfile()

        binding.apply {
            viewPager.adapter = pagerAdapter
            nextButton.apply {
                setShowMotionSpecResource(R.animator.fab_show)
                setHideMotionSpecResource(R.animator.fab_hide)

                setOnClickListener {
                    if (viewPager.currentItem + 1 < pagerAdapter.count) {
                        nextButton.isEnabled = false
                        viewPager.currentItem++
                    } else {
                        mainViewModel.getStarted()
                    }
                }
            }

            mainViewModel.loading.observe(this@WelcomeActivity, Observer { loading ->
                if (loading) {
                    nextButton.hide()
                    profileView.findViewById<ProgressBar>(R.id.progressbar).visibility =
                        View.VISIBLE

                }
            })

            mainViewModel.introductionShown.observe(this@WelcomeActivity, Observer { shown ->
                if (shown) {
                    userInteractionNavigation = true
                    mainViewModel.stopLoading(false)
                    Activities.MainActivity.dynamicStart?.let { intent ->
                        startActivity(intent, true)
                    }
                }
            })
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (position == 1) {
                        checkPermissionsState()
                    } else nextButton.show()
                }

                override fun onPageSelected(position: Int) {
                    nextButton.setImageResource(
                        if (position + 1 >= pagerAdapter.count)
                            R.drawable.ic_check_white_24dp
                        else R.drawable.ic_arrow_right_24dp
                    )
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        }
    }

    override fun onResume() {
        super.onResume()
        animateSplashScreen()
        setUserProfile()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val permissionsOk: Boolean = AppUtils.checkRunningConditions(this)
        binding.nextButton.isEnabled = permissionsOk
        if (permissionsOk) binding.nextButton.show() else binding.nextButton.hide()


        permissionView.findViewById<MaterialButton>(R.id.setup_button).text =
            if (permissionsOk) "Access Granted" else getString(R.string.action_setup)

        permissionView.findViewById<MaterialButton>(R.id.setup_button).isEnabled =
            !permissionsOk

        permissionView.findViewById<MaterialButton>(R.id.setup_button).background.setTint(
            if (permissionsOk) ContextCompat.getColor(this, R.color.colorAccentDark)
            else ContextCompat.getColor(this, R.color.colorAccent)
        )
    }

    override fun onUserProfileUpdated() {
        super.onUserProfileUpdated()
        setUserProfile()
    }

    private fun setUserProfile() {
        val localDevice = AppUtils.getLocalDevice(applicationContext, mainViewModel)
        val imageView: ImageView =
            profileView.findViewById(R.id.layout_profile_picture_image_default)
        val editImageView: ImageView =
            profileView.findViewById(R.id.layout_profile_picture_image_preferred)
        val deviceNameText: TextView =
            profileView.findViewById(R.id.header_default_device_name_text)
        val versionText: TextView =
            profileView.findViewById(R.id.header_default_device_version_text)
        deviceNameText.text = localDevice?.nick_name
        versionText.text = localDevice?.version_name
        loadProfilePictureInto(localDevice?.nick_name, imageView)
        editImageView.setOnClickListener { startProfileEditor() }
        TransitionManager.beginDelayedTransition(profileView)
    }

    protected open fun animateSplashScreen() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom_centered)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                binding.nextButton.show()
            }

        })
        splashView.findViewById<ImageView>(R.id.iv_logo).animation = anim
    }
}