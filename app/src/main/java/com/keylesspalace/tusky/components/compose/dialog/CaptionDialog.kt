/* Copyright 2019 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.compose.dialog

import android.app.Activity
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.InputFilter
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import at.connyduck.sparkbutton.helpers.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.keylesspalace.tusky.R
import kotlinx.coroutines.launch

// https://github.com/tootsuite/mastodon/blob/c6904c0d3766a2ea8a81ab025c127169ecb51373/app/models/media_attachment.rb#L32
private const val MEDIA_DESCRIPTION_CHARACTER_LIMIT = 1500

fun <T> T.makeCaptionDialog(
    existingDescription: String?,
    previewUri: Uri,
    onUpdateDescription: suspend (String) -> Boolean
) where T : Activity, T : LifecycleOwner {
    val dialogLayout = LinearLayout(this)
    val padding = Utils.dpToPx(this, 8)
    dialogLayout.setPadding(padding, padding, padding, padding)

    dialogLayout.orientation = LinearLayout.VERTICAL
    val imageView = PhotoView(this).apply {
        maximumScale = 6f
    }

    val margin = Utils.dpToPx(this, 4)
    dialogLayout.addView(imageView)
    (imageView.layoutParams as LinearLayout.LayoutParams).weight = 1f
    imageView.layoutParams.height = 0
    (imageView.layoutParams as LinearLayout.LayoutParams).setMargins(0, margin, 0, 0)

    val input = EditText(this)
    input.hint = resources.getQuantityString(
        R.plurals.hint_describe_for_visually_impaired,
        MEDIA_DESCRIPTION_CHARACTER_LIMIT, MEDIA_DESCRIPTION_CHARACTER_LIMIT
    )
    dialogLayout.addView(input)
    (input.layoutParams as LinearLayout.LayoutParams).setMargins(margin, margin, margin, margin)
    input.setLines(2)
    input.inputType = (
        InputType.TYPE_CLASS_TEXT
            or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
    input.setText(existingDescription)
    input.filters = arrayOf(InputFilter.LengthFilter(MEDIA_DESCRIPTION_CHARACTER_LIMIT))

    val okListener = { dialog: DialogInterface, _: Int ->
        lifecycleScope.launch {
            if (!onUpdateDescription(input.text.toString())) {
                showFailedCaptionMessage()
            }
        }
        dialog.dismiss()
    }

    val dialog = AlertDialog.Builder(this)
        .setView(dialogLayout)
        .setPositiveButton(android.R.string.ok, okListener)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    val window = dialog.window
    window?.setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    )

    dialog.show()

    // Load the image and manually set it into the ImageView because it doesn't have a fixed  size.
    Glide.with(this)
        .load(previewUri)
        .downsample(DownsampleStrategy.CENTER_INSIDE)
        .into(object : CustomTarget<Drawable>(4096, 4096) {
            override fun onLoadCleared(placeholder: Drawable?) {
                imageView.setImageDrawable(placeholder)
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                imageView.setImageDrawable(resource)
            }
        })
}

private fun Activity.showFailedCaptionMessage() {
    Toast.makeText(this, R.string.error_failed_set_caption, Toast.LENGTH_SHORT).show()
}
