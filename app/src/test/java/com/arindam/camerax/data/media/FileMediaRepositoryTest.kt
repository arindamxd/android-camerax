package com.arindam.camerax.data.media

import com.arindam.camerax.testing.RobolectricPermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FileMediaRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun publish_missingFileKeepsExpectedCause() = runTest {
        val repository = FileMediaRepository(
            context = RobolectricPermissions.applicationContext(),
            io = UnconfinedTestDispatcher(testScheduler)
        )
        val result = repository.publish(folder.root.resolve("missing.jpg"))
        val error = result.exceptionOrNull()
        assertTrue(error is MediaPublishException.MissingFile)
        assertTrue((error as MediaPublishException).expected)
    }
}
