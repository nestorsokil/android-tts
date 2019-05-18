package com.example.mobile.kotlin_android_draft

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

/**
 * Created by nsokil on 09.02.2017.
 */
object FileUtils {
    fun checkFile(dir: File, path: String, assetManager: AssetManager) {
        if (!dir.exists() && dir.mkdirs()){
            copyFiles(path, assetManager)
        }
        if(dir.exists()) {
            val datafilepath = path + "/tessdata/ukr.traineddata"
            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles(path, assetManager)
            }
        }
    }

    private fun copyFiles(path: String, assetManager: AssetManager) {
        try {
            val filepath = path + "/tessdata/ukr.traineddata"
            val instream = assetManager.open("ukr.traineddata")
            val outstream = FileOutputStream(filepath)

            val buffer = ByteArray(1024)
            var read = 0
            while (read != -1) {
                read = instream.read(buffer)
                outstream.write(buffer, 0, read)
            }
            outstream.flush()
            outstream.close()
            instream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}