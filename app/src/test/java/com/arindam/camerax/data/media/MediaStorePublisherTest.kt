package com.arindam.camerax.data.media

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.arindam.camerax.testing.RobolectricPermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MediaStorePublisherTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun mediaStoreCollections_api28UsesLegacyUri() {
        val collections = mediaStoreCollections(video = false, sdkInt = Build.VERSION_CODES.P)
        assertEquals(listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI), collections)
    }

    @Test
    fun mediaStoreCollections_api29TriesPrimaryThenExternal() {
        val collections = mediaStoreCollections(video = false, sdkInt = Build.VERSION_CODES.Q)
        assertEquals(
            listOf(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).distinct(),
            collections
        )
    }

    @Test
    fun shouldSkipLegacyStoragePublish_onlyBelowQWithoutPermission() {
        assertTrue(shouldSkipLegacyStoragePublish(Build.VERSION_CODES.P, hasWritePermission = false))
        assertFalse(shouldSkipLegacyStoragePublish(Build.VERSION_CODES.P, hasWritePermission = true))
        assertFalse(shouldSkipLegacyStoragePublish(Build.VERSION_CODES.Q, hasWritePermission = false))
    }

    @Test
    fun abandonPending_skipsNullAndSwallowsDeleteErrors() {
        var deleted = false
        abandonPending(null) { deleted = true }
        assertFalse(deleted)
        val uri = Uri.parse("content://media/external/images/media/1")
        var removed: Uri? = null
        abandonPending(uri) { removed = it }
        assertEquals(uri, removed)
        abandonPending(uri) { error("delete failed") }
    }

    @Test
    fun publish_missingFileIsExpectedFailure() {
        val result = MediaStorePublisher.publish(
            RobolectricPermissions.applicationContext(),
            folder.root.resolve("missing.jpg")
        )
        val error = result.exceptionOrNull()
        assertTrue(error is MediaPublishException.MissingFile)
        assertTrue((error as MediaPublishException).expected)
    }

    @Config(sdk = [28])
    @Test
    fun publish_api28WithoutWritePermissionIsExpectedFailure() {
        RobolectricPermissions.denyWriteExternalStorage()
        val file = folder.newFile("shot.jpg").apply { writeText("jpeg") }
        val result = MediaStorePublisher.publish(
            RobolectricPermissions.applicationContext(),
            file
        )
        val error = result.exceptionOrNull()
        assertTrue(error is MediaPublishException.NoStoragePermission)
        assertTrue((error as MediaPublishException).expected)
    }
}
