package com.arindam.camerax.ui.home.gallery

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

    private fun viewModel(media: FakeMediaRepository) = GalleryViewModel(
        cameraInteractors(
            FakeCameraRepository(),
            media,
            FakeSettingsRepository(),
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
