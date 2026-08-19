package com.anyonehub.abl.packaging

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ApkPackager {

    /**
     * Packages compiled DEX and linked resources into an unaligned APK.
     * 
     * @param dexBytes The Dalvik bytecode (classes.dex).
     * @param linkedApkFile The linked APK from AAPT2 containing resources.arsc and manifest.
     * @param outputFile The final destination for the unaligned APK.
     */
    fun packageApk(dexBytes: ByteArray, linkedApkFile: File, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // Write classes.dex
            val dexEntry = ZipEntry("classes.dex")
            zos.putNextEntry(dexEntry)
            zos.write(dexBytes)
            zos.closeEntry()
            
            // Copy contents from linked.apk (resources.arsc, AndroidManifest.xml, res/*)
            ZipFile(linkedApkFile).use { zipFile ->
                for (entry in zipFile.entries().toList()) {
                    if (entry.name != "classes.dex") { // Ensure no conflict if present
                        val outEntry = ZipEntry(entry.name)
                        outEntry.method = entry.method
                        if (entry.method == ZipEntry.STORED) {
                            outEntry.size = entry.size
                            outEntry.compressedSize = entry.compressedSize
                            outEntry.crc = entry.crc
                        }
                        outEntry.time = entry.time
                        outEntry.comment = entry.comment
                        outEntry.extra = entry.extra

                        zos.putNextEntry(outEntry)
                        zipFile.getInputStream(entry).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}
