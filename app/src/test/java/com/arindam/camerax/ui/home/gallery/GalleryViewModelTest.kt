package com.arindam.camerax.ui.home.gallery

import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeDeviceFeaturesRepository
import com.arindam.camerax.testing.FakeMediaRepository
import com.arindam.camerax.testing.FakeSettingsRepository
import com.arindam.camerax.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_loadsItemsOffMainThread() {
        val media = FakeMediaRepository()
        val first = File("one.jpg")
        val second = File("two.mp4")
        media.add(first)
        media.add(second)
        val vm = viewModel(media)
        assertFalse(vm.uiState.value.loading)
        assertEquals(listOf(first, second), vm.uiState.value.items.map { it.file })
        assertEquals(true, vm.uiState.value.items[1].isVideo)
    }

    @Test
    fun refresh_emptyDirectoryShowsNoItems() {
        val vm = viewModel(FakeMediaRepository())
        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun refresh_usesGalleryVideoAutoplayFromSettings() {
        val settings = FakeSettingsRepository(CaptureSettings(galleryVideoAutoplay = true))
        val vm = viewModel(FakeMediaRepository(), settings)
        assertTrue(vm.uiState.value.videoAutoplay)
    }

    @Test
    fun refresh_classifiesRawAndStillFiles() {
        val media = FakeMediaRepository()
        val still = File("photo.jpg")
        val raw = File("frame.dng")
        media.add(still)
        media.add(raw)
        val vm = viewModel(media)
        assertFalse(vm.uiState.value.items[0].isRaw)
        assertTrue(vm.uiState.value.items[1].isRaw)
    }

    @Test
    fun delete_removesItemFromState() {
        val media = FakeMediaRepository()
        val first = File("one.jpg")
        val second = File("two.jpg")
        media.add(first)
        media.add(second)
        val vm = viewModel(media)
        vm.delete(first)
        assertEquals(listOf(second), vm.uiState.value.items.map { it.file })
    }

    private fun viewModel(
        media: FakeMediaRepository,
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ) = GalleryViewModel(
        cameraInteractors(
            FakeCameraRepository(),
            media,
            settings,
            FakeDeviceFeaturesRepository()
        ),
        File("dir"),
        AppDispatchers(
            main = mainDispatcherRule.dispatcher,
            default = mainDispatcherRule.dispatcher,
            io = mainDispatcherRule.dispatcher
        )
    )
}
