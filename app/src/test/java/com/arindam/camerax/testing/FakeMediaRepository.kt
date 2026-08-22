package com.arindam.camerax.testing

import com.arindam.camerax.domain.repository.MediaRepository
import java.io.File

/** In-memory [MediaRepository] for unit tests. */
class FakeMediaRepository : MediaRepository {

    private val files = mutableListOf<File>()
    var publishResult: Result<Unit> = Result.success(Unit)
    var stitchResult: Result<File>? = null
    val published = mutableListOf<File>()

    fun add(file: File) {
        files += file
    }

    override fun picturesDirectory(): File = File("pictures")

    override suspend fun latest(directory: File): File? = list(directory).firstOrNull()

    override suspend fun list(directory: File): List<File> = files.toList()

    override suspend fun delete(file: File): Boolean = files.remove(file)

    override suspend fun stitchPanorama(frames: List<File>, outputDirectory: File): Result<File> =
        stitchResult ?: Result.success(frames.first())

    override suspend fun publish(file: File): Result<Unit> {
        published += file
        return publishResult
    }
}
