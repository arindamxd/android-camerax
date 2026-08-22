package com.arindam.camerax.ui.home.gallery

import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeDeviceFeaturesRepository
import com.arindam.camerax.testing.FakeMediaRepository
import com.arindam.camerax.testing.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GalleryViewModelTest {

    @Test
    fun delete_removesItemFromState() {
        val media = FakeMediaRepository()
        val first = File("one.jpg")
        val second = File("two.jpg")
        media.add(first)
        media.add(second)
        val vm = GalleryViewModel(
            cameraInteractors(
                FakeCameraRepository(),
                media,
                FakeSettingsRepository(),
                FakeDeviceFeaturesRepository()
            ),
            File("dir")
        )
        assertEquals(listOf(first, second), vm.uiState.value.items)
        vm.delete(first)
        assertEquals(listOf(second), vm.uiState.value.items)
    }
}
