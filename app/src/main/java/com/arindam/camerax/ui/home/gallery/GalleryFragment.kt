package com.arindam.camerax.ui.home.gallery

import android.content.DialogInterface
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import androidx.annotation.StyleRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.content.FileProvider
import androidx.navigation.fragment.navArgs
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.R
import com.arindam.camerax.databinding.FragmentGalleryBinding
import com.arindam.camerax.ui.base.BaseFragment
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.commons.Constants.FILE.EXTENSION_WHITELIST
import com.arindam.camerax.util.padWithDisplayCutout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.*

/**
 * Fragment used to present the user with a gallery of photos taken
 *
 * Created by Arindam Karmakar on 9/5/19.
 */

class GalleryFragment : BaseFragment<FragmentGalleryBinding>() {

    /** AndroidX navigation arguments */
    private val args: GalleryFragmentArgs by navArgs()

    private lateinit var mediaList: MutableList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        // Get root directory of media from navigation arguments
        val rootDirectory = File(args.rootDirectory)

        // Walk through all files in the root directory
        // We reverse the order of the list to present the last photos first
        mediaList = rootDirectory.listFiles { file ->
            EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()
    }

    override fun provideBinding(): FragmentGalleryBinding = FragmentGalleryBinding.inflate(layoutInflater)

    @OptIn(ExperimentalFoundationApi::class)
    override fun setComposeView() {
        binding.galleryComposeView.apply {
            setContent {
                AppTheme {
                    GalleryScreen(
                        mediaList,
                        onBackClicked = { // Handle back button press
                            navigateBack()
                        },
                        onShareClicked = { currentItem -> // Handle share button press
                            // Make sure that we have a file to share
                            mediaList.getOrNull(currentItem)?.let { mediaFile ->

                                // Create a sharing intent
                                val intent = Intent().apply {
                                    // Infer media type from file extension
                                    val mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaFile.extension)
                                    // Get URI from our FileProvider implementation
                                    val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", mediaFile)
                                    // Set the appropriate intent extra, type, action and flags
                                    putExtra(Intent.EXTRA_STREAM, uri)

                                    type = mediaType
                                    action = Intent.ACTION_SEND
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }

                                // Launch the intent letting the user choose which app to share with
                                startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
                            }
                        },
                        onDeleteClicked = { pager -> // Handle delete button press
                            // Make sure that we have a file to delete
                            mediaList.getOrNull(pager.currentPage)?.let { mediaFile ->
                                val listener = DialogInterface.OnClickListener { dialog, which ->
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        // Delete current photo
                                        mediaFile.delete()

                                        // Send relevant broadcast to notify other apps of deletion
                                        MediaScannerConnection.scanFile(context, arrayOf(mediaFile.absolutePath), null, null)

                                        // Notify our view pager
                                        mediaList.removeAt(pager.currentPage)

                                        // If all photos have been deleted, return to camera
                                        //if (mediaList.isEmpty()) navigateBack()
                                        navigateBack()
                                    } else {
                                        dialog.dismiss()
                                    }
                                }

                                MaterialAlertDialogBuilder(requireContext(), getAlertDialogButtonStyle())
                                    .setTitle(R.string.delete_title)
                                    .setMessage(R.string.delete_subtitle)
                                    .setPositiveButton(R.string.delete_button_alt, listener)
                                    .setNegativeButton(R.string.delete_button_cancel, listener)
                                    .show()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun setupView(view: View, savedInstanceState: Bundle?) {
        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            binding.root.padWithDisplayCutout()
        }
    }

    @StyleRes
    private fun getAlertDialogButtonStyle(): Int = R.style.MaterialAlertDialogButton_DayNight
}
