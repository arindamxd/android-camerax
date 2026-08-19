package com.arindam.camerax.ui.home.gallery

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.navigation.fragment.navArgs
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.commons.Constants.FILE.EXTENSION_WHITELIST
import java.io.File
import java.util.Locale

/**
 * In-app viewer for files in the app pictures directory. Share is hosted here (FileProvider);
 * delete lives in [GalleryScreen] against the same in-memory list.
 */
class GalleryFragment : BaseFragmentCompose() {

    private val args: GalleryFragmentArgs by navArgs()
    private lateinit var mediaList: MutableList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootDirectory = File(args.rootDirectory)
        mediaList = rootDirectory.listFiles { file ->
            EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun setComposeView(view: ComposeView) = view.setContent {
        AppTheme(isDarkTheme = true) {
            GalleryScreen(
                dataList = mediaList,
                navigateBack = { navigateBack() },
                onShareClicked = { currentItem ->
                    mediaList.getOrNull(currentItem)?.let { mediaFile ->
                        val intent = Intent().apply {
                            val mediaType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(mediaFile.extension)
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                BuildConfig.APPLICATION_ID + ".provider",
                                mediaFile
                            )
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri("", uri)
                            type = mediaType
                            action = Intent.ACTION_SEND
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
                    }
                }
            )
        }
    }
}
