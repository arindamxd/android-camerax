package com.arindam.camerax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.File

private const val FILE_NAME_KEY = "file_name"

/**
 * Fragment used for each individual page showing a photo inside of [GalleryFragment]
 *
 * Created by Arindam Karmakar on 9/5/19.
 */
class PhotoFragment internal constructor() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ImageView(context)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val file = File(it.getString(FILE_NAME_KEY))
            Glide.with(this).load(file).into(view as ImageView)
        }
    }

    companion object {
        fun create(image: File) = PhotoFragment().apply {
            arguments = Bundle().apply {
                putString(FILE_NAME_KEY, image.absolutePath)
            }
        }
    }
}
