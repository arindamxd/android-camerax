package com.arindam.camerax.ui.home.gallery

import android.content.ClipData
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.theme.applyEdgeToEdgeBarsForNightMode
import java.io.File

/**
 * In-app viewer for files in the app pictures directory. Share is hosted here (FileProvider);
 * delete goes through [GalleryViewModel] / [com.arindam.camerax.domain.usecase.DeleteMedia].
 */
class GalleryFragment : BaseFragmentCompose() {

    private val args: GalleryFragmentArgs by navArgs()
    private val viewModel: GalleryViewModel by viewModels {
        val app = requireActivity().application as CameraX
        GalleryViewModelFactory(app.container.cameraInteractors, File(args.rootDirectory))
    }

    override fun onResume() {
        super.onResume()
        requireActivity().applyEdgeToEdgeBarsForNightMode()
        viewModel.refresh()
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun setComposeView(view: ComposeView) = view.setContent {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.items) {
            if (state.items.isEmpty()) navigateBack()
        }
        AppTheme {
            GalleryScreen(
                items = state.items,
                videoAutoplay = state.videoAutoplay,
                navigateBack = { navigateBack() },
                onShareClicked = { currentItem ->
                    state.items.getOrNull(currentItem)?.let { mediaFile ->
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
                },
                onDelete = viewModel::delete
            )
        }
    }
}
