package com.arindam.camerax.domain.usecase

import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.EffectMode
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeMediaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CameraUseCasesTest {

    @Test
    fun capturePhoto_returnsRepositoryResult() = runTest {
        val camera = FakeCameraRepository()
        val file = File("still.jpg")
        camera.captureResult = Result.success(file)
        val result = CapturePhoto(camera)(
            outputDirectory = File("out"),
            lens = CameraLens.BACK,
            effect = EffectMode.NONE,
            motionPhoto = false
        )
        assertEquals(file, result.getOrNull())
    }

    @Test
    fun startRecording_forwardsMutedFlagToRepository() {
        val camera = FakeCameraRepository()
        camera.recordingResult = Result.success(File("clip.mp4"))
        val result = StartRecording(camera)(File("out"), muted = true, persistent = false)
        assertTrue(result.isSuccess)
        assertEquals(true, camera.lastRecordingMuted)
        assertEquals(false, camera.lastRecordingPersistent)
    }

    @Test
    fun startRecording_forwardsPersistentFlagToRepository() {
        val camera = FakeCameraRepository()
        camera.recordingResult = Result.success(File("clip.mp4"))
        StartRecording(camera)(File("out"), muted = false, persistent = true)
        assertEquals(false, camera.lastRecordingMuted)
        assertEquals(true, camera.lastRecordingPersistent)
    }

    @Test
    fun muteRecording_forwardsToRepository() {
        val camera = FakeCameraRepository()
        MuteRecording(camera)(muted = true)
        assertEquals(true, camera.lastMuteRecording)
        assertEquals(1, camera.muteRecordingCalls)
    }

    @Test
    fun startRecording_returnsFailureFromRepository() {
        val camera = FakeCameraRepository()
        val result = StartRecording(camera)(File("out"), muted = false)
        assertTrue(result.isFailure)
    }

    @Test
    fun publishMedia_recordsFile() = runTest {
        val media = FakeMediaRepository()
        val file = File("shot.jpg")
        val result = PublishMedia(media)(file)
        assertTrue(result.isSuccess)
        assertEquals(listOf(file), media.published)
    }

    @Test
    fun listAndDeleteMedia() = runTest {
        val media = FakeMediaRepository()
        val first = File("a.jpg")
        val second = File("b.jpg")
        media.add(first)
        media.add(second)
        assertEquals(listOf(first, second), ListMedia(media)(File("dir")))
        assertTrue(DeleteMedia(media)(first))
        assertEquals(listOf(second), ListMedia(media)(File("dir")))
    }

    @Test
    fun getLatestMedia_returnsFirstListedFile() = runTest {
        val media = FakeMediaRepository()
        val first = File("a.jpg")
        media.add(first)
        media.add(File("b.jpg"))
        assertEquals(first, GetLatestMedia(media)(File("dir")))
    }

    @Test
    fun stitchPanorama_usesRepositoryResult() = runTest {
        val media = FakeMediaRepository()
        val stitched = File("pano.jpg")
        media.stitchResult = Result.success(stitched)
        val result = StitchPanorama(media)(listOf(File("a.jpg")), File("out"))
        assertEquals(stitched, result.getOrNull())
    }
}
