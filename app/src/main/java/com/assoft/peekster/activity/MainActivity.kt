package com.assoft.peekster.activity

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.R
import com.assoft.peekster.adapter.ContentAdapter
import com.assoft.peekster.data.Data.gridLayout
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.databinding.ActivityMainBinding
import com.assoft.peekster.nav.BottomChildDrawerFragment
import com.assoft.peekster.nav.ShowHideFabStateAction
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.setupToolbarMenuItem
import com.assoft.peekster.util.ext.startActivity
import com.assoft.peekster.util.ext.toast
import org.koin.android.viewmodel.ext.android.viewModel
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : Activity() {

    /** Internal variable for obtaining the [ActivityMainBinding] binding. */
    private val binding: ActivityMainBinding by contentView(R.layout.activity_main)

    /** Reference to the [MediaViewModel] used for media events */
    private val viewModel: MediaViewModel by viewModel()

    private val bottomSheetFragment: BottomChildDrawerFragment by lazy(NONE) {
        supportFragmentManager.findFragmentById(R.id.bottom_sheet_layout) as BottomChildDrawerFragment
    }

    private var exitPressTime: Long = 0L

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gridLayout = false
        val contentAdapter = ContentAdapter(viewModel)
        val animation =
            AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        binding.apply {
            toolbar.setupToolbarMenuItem(bottomSheetFragment, this@MainActivity, viewModel)

            recyclerView.apply {
                layoutAnimation = animation
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = contentAdapter
            }

            stream.setOnClickListener {
                binding.fab.close(true)
                Handler().postDelayed({
                    userInteractionNavigation = true
                    Activities.StreamActivity.dynamicStart?.let { intent ->
                        startActivity(intent, false)
                    }
                }, (binding.fab.childCount * 50).toLong())
            }

            shareFiles.setOnClickListener {
                binding.fab.close(true)
                Handler().postDelayed({
                    userInteractionNavigation = true
                    Activities.TransferActivity.dynamicStart?.let { intent ->
                        startActivity(intent, false)
                    }
                }, (binding.fab.childCount * 50).toLong())
            }
            addMovies.setOnClickListener {
                binding.fab.close(true)
                Handler().postDelayed({
                    Activities.AddCategoryActivity.dynamicStart?.let { intent ->
                        startActivity(
                            intent,
                            false,
                            extras = hashMapOf("category" to "Movie")
                        )
                    }
                }, (binding.fab.childCount * 50).toLong())
            }

            addTvShows.setOnClickListener {
                binding.fab.close(true)
                Handler().postDelayed({
                    Activities.AddCategoryActivity.dynamicStart?.let { intent ->
                        startActivity(
                            intent,
                            false,
                            extras = hashMapOf("category" to "Tv Show")
                        )
                    }
                }, (binding.fab.childCount * 50).toLong())
            }

            addMusic.setOnClickListener {
                binding.fab.close(true)
                Handler().postDelayed({
                    Activities.AddCategoryActivity.dynamicStart?.let { intent ->
                        startActivity(
                            intent,
                            false,
                            extras = hashMapOf("category" to "Music")
                        )
                    }
                }, (binding.fab.childCount * 50).toLong())
            }
        }

        bottomSheetFragment.apply {
            addOnStateChangedAction(ShowHideFabStateAction(binding.fab))
        }

        viewModel.parentControlData.observe(this, Observer {
            contentAdapter.notifyDataSetChanged()
        })

        viewModel.getAllCategories().observe(this,
            Observer { list ->
                list?.let {
                    contentAdapter.submitList(it as MutableList<Category>)
                }
            })

        viewModel.navigateToShowAllCategory.observe(this, Observer {
            Activities.CategoryActivity.dynamicStart?.let { intent ->
                startActivity(
                    intent,
                    false,
                    extras = hashMapOf("category" to it)
                )
            }
        })

        viewModel.showPopupMenu.observe(this, Observer {
            val popup = PopupMenu(this, it.keys.first(), Gravity.START)
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.pop_up_menu, popup.menu)

            // registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.see_all -> {
                        Activities.CategoryActivity.dynamicStart?.let { intent ->
                            startActivity(
                                intent,
                                false,
                                extras = hashMapOf("category" to it.values.first())
                            )
                        }
                    }
                    R.id.remove -> {
                        viewModel.deleteCategory(it.values.first())
                    }
                }
                true
            }
            popup.show()
        })

        viewModel.navigateToMovieDetail.observe(this, Observer { movie ->
            // Navigate to Movie Detail
            Activities.MovieDetail.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to movie))
            }
        })

        viewModel.navigateToTvShowDetail.observe(this, Observer { tv ->
            // Navigate to TvShow Detail
            Activities.TvShowDetail.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to tv))
            }
        })

        viewModel.navigateToAudioPlayer.observe(this, Observer { audio ->
            Activities.AudioPlayer.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to audio))
            }
        })
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - exitPressTime < 2000) {
            exitApp()
            super.onBackPressed()
        } else {
            exitPressTime = System.currentTimeMillis()
            toast(getString(R.string.secure_exit), duration = Toast.LENGTH_SHORT)
        }
    }

    override fun onResume() {
        super.onResume()
        gridLayout = false
    }

    companion object {
        const val REQUEST_PERMISSION_ALL = 1
    }
}
