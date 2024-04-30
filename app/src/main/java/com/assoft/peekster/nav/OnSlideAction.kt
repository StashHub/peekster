/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.assoft.peekster.nav

import android.view.View
import androidx.annotation.FloatRange
import com.assoft.peekster.util.ext.normalize
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * An action to be performed when a bottom sheet's slide offset is changed.
 */
interface OnSlideAction {
    /**
     * Called when the bottom sheet's [slideOffset] is changed. [slideOffset] will always be a
     * value between -1.0 and 1.0. -1.0 is equal to [BottomSheetBehavior.STATE_HIDDEN], 0.0
     * is equal to [BottomSheetBehavior.STATE_HALF_EXPANDED] and 1.0 is equal to
     * [BottomSheetBehavior.STATE_EXPANDED].
     */
    fun onSlide(
        sheet: View,
        @FloatRange(
            from = -1.0,
            fromInclusive = true,
            to = 1.0,
            toInclusive = true
        ) slideOffset: Float
    )
}

/**
 * Change the alpha of [view] when a bottom sheet is slid.
 *
 * @param reverse Setting reverse to true will cause the view's alpha to approach 0.0 as the sheet
 *  slides up. The default behavior, false, causes the view's alpha to approach 1.0 as the sheet
 *  slides up.
 */
class AlphaSlideAction(
    private val view: View,
    private val reverse: Boolean = false
) : OnSlideAction {

    override fun onSlide(sheet: View, slideOffset: Float) {
        val alpha = slideOffset.normalize(-1F, 0F, 0F, 1F)
        view.alpha = if (!reverse) alpha else 1F - alpha
    }
}