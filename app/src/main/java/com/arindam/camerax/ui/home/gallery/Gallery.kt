@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    data: List<File?> = listOf(),
    onBackClicked: () -> Unit,
    onShareClicked: (Int) -> Unit,
    onDeleteClicked: (PagerState) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = {
        data.size
    })

    Surface {
        GalleryPager(
            pagerState = pagerState,
            data = data
        )
        Box {
            GalleryHeader(
                onBackClicked = onBackClicked
            )
            GalleryFooter(
                pagerState = pagerState,
                isDisabled = data.isEmpty(),
                onShareClicked = onShareClicked,
                onDeleteClicked = onDeleteClicked
            )
        }
    }
}

@Composable
private fun GalleryHeader(
    onBackClicked: () -> Unit
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
                ) { onBackClicked.invoke() },
            contentDescription = "Back"
        )
    }
}

@Composable
private fun GalleryFooter(
    pagerState: PagerState,
    isDisabled: Boolean,
    onShareClicked: (Int) -> Unit,
    onDeleteClicked: (PagerState) -> Unit
) {
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
            if (isDisabled) return@Column
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
            if (isDisabled) return@Column
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false)
                    ) { onDeleteClicked.invoke(pagerState) },
                contentDescription = "Back"
            )
        }
    }
}

@Composable
private fun GalleryPager(
    pagerState: PagerState,
    data: List<File?> = listOf(),
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
        data[page]?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
    }
}

@DarkLightPreviews
@Composable
private fun GalleryScreenPreview() {
    AppTheme {
        GalleryScreen(
            onBackClicked = {},
            onShareClicked = {},
            onDeleteClicked = {}
        )
    }
}
