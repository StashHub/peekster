package com.assoft.peekster.nav

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.assoft.peekster.R
import com.assoft.peekster.databinding.ChildLockBottomSheetLayoutBinding
import com.assoft.peekster.activity.MediaViewModel
import com.assoft.peekster.util.ext.themeColor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.shape.MaterialShapeDrawable
import org.koin.android.viewmodel.ext.android.sharedViewModel
import kotlin.LazyThreadSafetyMode.NONE

/**
 * A [Fragment] which acts as a bottom sheet layout.
 */
class BottomChildDrawerFragment : Fragment() {

    private lateinit var binding: ChildLockBottomSheetLayoutBinding

    private val behavior: BottomSheetBehavior<FrameLayout> by lazy(NONE) {
        from(binding.backgroundContainer)
    }

    /** Reference to the [MediaViewModel] used for media events */
    private val viewModel: MediaViewModel by sharedViewModel()

    private val bottomSheetCallback = BottomChildDrawerCallback()

    private val backgroundShapeDrawable: MaterialShapeDrawable by lazy(NONE) {
        val backgroundContext = binding.backgroundContainer.context
        MaterialShapeDrawable(
            backgroundContext,
            null,
            R.attr.bottomSheetStyle,
            0
        ).apply {
            fillColor = ColorStateList.valueOf(
                backgroundContext.themeColor(R.attr.colorSurface)
            )
            elevation = resources.getDimension(R.dimen.plane_08)
            initializeElevationOverlay(requireContext())
        }
    }

    private val closeDrawerOnBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, closeDrawerOnBackPressed)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ChildLockBottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            backgroundContainer.background = backgroundShapeDrawable

            scrimView.setOnClickListener { close() }

            enableButton.setOnClickListener {
                viewModel.setParentalControl()
                close()
            }

            bottomSheetCallback.apply {
                // Scrim view transforms
                addOnSlideAction(AlphaSlideAction(scrimView))
                addOnStateChangedAction(VisibilityStateAction(scrimView))

                // If the drawer is open, pressing the system back button should close the drawer.
                addOnStateChangedAction(object : OnStateChangedAction {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        closeDrawerOnBackPressed.isEnabled = newState != STATE_HIDDEN
                    }
                })
            }

            behavior.addBottomSheetCallback(bottomSheetCallback)
            behavior.state = STATE_HIDDEN
        }
    }

    fun toggle() {
        when (behavior.state) {
            STATE_HIDDEN -> open()
            STATE_HALF_EXPANDED,
            STATE_EXPANDED,
            STATE_COLLAPSED -> close()
        }
    }

    private fun open() {
        behavior.state = STATE_HALF_EXPANDED
    }

    fun close() {
        behavior.state = STATE_HIDDEN
    }

    fun addOnSlideAction(action: OnSlideAction) {
        bottomSheetCallback.addOnSlideAction(action)
    }

    fun addOnStateChangedAction(action: OnStateChangedAction) {
        bottomSheetCallback.addOnStateChangedAction(action)
    }

}