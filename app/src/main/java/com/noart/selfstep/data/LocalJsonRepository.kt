package com.noart.selfstep.data

import android.content.Context
import com.noart.selfstep.model.SelfStepData
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LocalJsonRepository(context: Context) {
    private val dataFile = File(context.filesDir, FILE_NAME)

    fun load(): SelfStepData {
        if (!dataFile.exists()) return SelfStepData()

        return runCatching {
            SelfStepJson.decode(dataFile.readText(Charsets.UTF_8))
        }.getOrElse {
            preserveUnreadableFile()
            SelfStepData()
        }
    }

    fun save(data: SelfStepData) {
        val temporaryFile = File(dataFile.parentFile, "$FILE_NAME.tmp")
        temporaryFile.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(SelfStepJson.encode(data))
            writer.flush()
        }

        try {
            Files.move(
                temporaryFile.toPath(),
                dataFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporaryFile.toPath(),
                dataFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun preserveUnreadableFile() {
        val backup = File(dataFile.parentFile, "selfstep_data_corrupt_${System.currentTimeMillis()}.json")
        runCatching { Files.move(dataFile.toPath(), backup.toPath()) }
    }

    companion object {
        const val FILE_NAME = "selfstep_data.json"
    }
}
