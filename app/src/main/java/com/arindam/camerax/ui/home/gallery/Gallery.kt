@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.arindam.camerax.ui.home.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import java.io.File

/**
 * Created by Arindam Karmakar on 18/09/23.
 */

@Composable
fun GalleryScreen(
    dataList: List<File?> = listOf(),
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit
) {
    val mediaList = rememberSaveable { mutableStateOf(listOf<File?>()) }
    val pagerState = rememberPagerState(pageCount = { mediaList.value.size })

    LaunchedEffect(mediaList) {
        mediaList.value = dataList.toMutableList()
    }

    Surface {
        GalleryPager(
            dataList = mediaList,
            pagerState = pagerState
        )
        Box {
            GalleryHeader(
                navigateBack = navigateBack
            )
            GalleryFooter(
                dataList = mediaList,
                pagerState = pagerState,
                navigateBack = navigateBack,
                onShareClicked = onShareClicked
            )
        }
    }
}

@DarkLightPreviews
@Composable
private fun GalleryScreenPreview() {
    AppTheme {
        GalleryScreen(
            navigateBack = {},
            onShareClicked = {}
        )
    }
}

@Composable
private fun GalleryHeader(
    navigateBack: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 10.dp, top = 10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back),
            modifier = Modifier
                .size(48.dp)
                .padding(10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false)
                ) { navigateBack.invoke() },
            contentDescription = "Back"
        )
    }
}

@Composable
private fun GalleryFooter(
    dataList: MutableState<List<File?>>,
    pagerState: PagerState,
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    DeleteDialog(
        show = showDialog.value,
        onConfirmed = {
            val deletedFile = dataList.value[pagerState.currentPage].also { it?.delete() }
            dataList.value = dataList.value.filterNot { it == deletedFile }

            // If all photos have been deleted, return to camera
            if (dataList.value.isEmpty()) navigateBack()
        },
        onDismiss = { showDialog.value = false }
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1F)
                .padding(start = 100.dp, bottom = 20.dp)
        ) {
            if (dataList.value.isEmpty()) return@Column
            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false)
                    ) { onShareClicked.invoke(pagerState.currentPage) },
                contentDescription = "Back"
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1F)
                .padding(end = 100.dp, bottom = 20.dp)
        ) {
            if (dataList.value.isEmpty()) return@Column
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false)
                    ) {
                        showDialog.value = true
                    },
                contentDescription = "Back"
            )
        }
    }
}

@Composable
private fun GalleryPager(
    dataList: MutableState<List<File?>>,
    pagerState: PagerState,
) {
    LaunchedEffect(pagerState) {
        // Collect from the a snapshotFlow reading the currentPage
        snapshotFlow { pagerState.currentPage }.collect { page ->
            pagerState.animateScrollToPage(page)
        }
    }

    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fill,
        beyondBoundsPageCount = 2,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        dataList.value[page]?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
    }
}

@Composable
fun DeleteDialog(
    show: Boolean = false,
    title: Int = R.string.delete_title,
    text: Int = R.string.delete_subtitle,
    confirmTitle: Int = R.string.delete_button_alt,
    dismissTitle: Int = R.string.delete_button_cancel,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        confirmButton = {
            TextButton(onClick = {
                onDismiss.invoke()
                onConfirmed.invoke()
            }) {
                Text(text = stringResource(id = confirmTitle))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss.invoke() }) {
                Text(text = stringResource(id = dismissTitle))
            }
        },
        title = {
            Text(text = stringResource(id = title))
        },
        text = {
            Text(text = stringResource(id = text))
        },
    )
}
