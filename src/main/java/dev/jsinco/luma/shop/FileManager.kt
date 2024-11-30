package dev.jsinco.luma.shop

import dev.jsinco.luma.ThanksgivingEvent
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Files

// using this because it already has serialization and deserialization
class FileManager(val fileName: String) {

    companion object {
        private val plugin: ThanksgivingEvent = ThanksgivingEvent.getInstance()
    }

    val file: File = File(plugin.dataFolder, fileName)
    private var yamlConfiguration: YamlConfiguration? = null


    fun generateYamlFile(): YamlConfiguration {
        generateFile()
        return getFileYaml()
    }

    fun generateFile() {
        try {
            if (!file.exists()) {
                file.createNewFile()

                val inputStream = plugin.getResource(fileName)
                if (inputStream != null) {
                    val outputStream = Files.newOutputStream(file.toPath())
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    inputStream.close()
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            plugin.logger.warning("Could not generate file: ${file.name} \n $ex")
        }
    }

    fun deleteFile() {
        if (file.exists()) {
            file.delete()
        }
    }

    fun getFileYaml(): YamlConfiguration {
        yamlConfiguration = YamlConfiguration.loadConfiguration(file)
        return yamlConfiguration!!
    }

    fun saveFileYaml() {
        yamlConfiguration?.save(file)
    }

    fun reloadFileYaml() {
        yamlConfiguration?.load(file)
    }
}