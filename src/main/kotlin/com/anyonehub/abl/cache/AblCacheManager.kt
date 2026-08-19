package com.anyonehub.abl.cache

import com.anyonehub.abl.cache.fbs.ScannedMetadataFbs
import com.anyonehub.abl.cache.fbs.SymbolCacheTable
import com.google.flatbuffers.FlatBufferBuilder
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

data class ScannedMetadataFbsData(
    val targetClass: String,
    val isModule: Boolean,
    val isCompileTarget: Boolean,
    val entryPoints: List<String>,
    val injectableProperties: List<String>,
    val injectableConstructorParameters: Boolean
)

class AblCacheManager(private val cacheFile: File) {

    fun loadCache(): List<ScannedMetadataFbsData>? {
        if (!cacheFile.exists()) return null
        return try {
            RandomAccessFile(cacheFile, "r").use { raf ->
                val channel = raf.channel
                val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                val root = SymbolCacheTable.getRootAsSymbolCacheTable(buffer)
                
                val result = mutableListOf<ScannedMetadataFbsData>()
                for (i in 0 until root.metadataLength()) {
                    val fbsMeta = root.metadata(i) ?: continue
                    
                    val entryPoints = mutableListOf<String>()
                    for (j in 0 until fbsMeta.entryPointsLength()) {
                        entryPoints.add(fbsMeta.entryPoints(j))
                    }
                    
                    val injectables = mutableListOf<String>()
                    for (j in 0 until fbsMeta.injectablePropertiesLength()) {
                        injectables.add(fbsMeta.injectableProperties(j))
                    }
                    
                    result.add(ScannedMetadataFbsData(
                        targetClass = fbsMeta.targetClass(),
                        isModule = fbsMeta.isModule(),
                        isCompileTarget = fbsMeta.isCompileTarget(),
                        entryPoints = entryPoints,
                        injectableProperties = injectables,
                        injectableConstructorParameters = fbsMeta.injectableConstructorParameters()
                    ))
                }
                result
            }
        } catch (e: Exception) {
            System.err.println("Failed to load flatbuffers AST cache: ${e.message}")
            null
        }
    }

    fun saveCache(metadataList: List<ScannedMetadataFbsData>) {
        try {
            val builder = FlatBufferBuilder(1024)
            
            val metadataOffsets = IntArray(metadataList.size)
            for ((index, data) in metadataList.withIndex()) {
                val targetClassOffset = builder.createString(data.targetClass)
                
                val entryPointsOffsets = IntArray(data.entryPoints.size)
                for (i in data.entryPoints.indices) {
                    entryPointsOffsets[i] = builder.createString(data.entryPoints[i])
                }
                val entryPointsVector = ScannedMetadataFbs.createEntryPointsVector(builder, entryPointsOffsets)
                
                val injectablesOffsets = IntArray(data.injectableProperties.size)
                for (i in data.injectableProperties.indices) {
                    injectablesOffsets[i] = builder.createString(data.injectableProperties[i])
                }
                val injectablesVector = ScannedMetadataFbs.createInjectablePropertiesVector(builder, injectablesOffsets)
                
                ScannedMetadataFbs.startScannedMetadataFbs(builder)
                ScannedMetadataFbs.addTargetClass(builder, targetClassOffset)
                ScannedMetadataFbs.addIsModule(builder, data.isModule)
                ScannedMetadataFbs.addIsCompileTarget(builder, data.isCompileTarget)
                ScannedMetadataFbs.addEntryPoints(builder, entryPointsVector)
                ScannedMetadataFbs.addInjectableProperties(builder, injectablesVector)
                ScannedMetadataFbs.addInjectableConstructorParameters(builder, data.injectableConstructorParameters)
                
                metadataOffsets[index] = ScannedMetadataFbs.endScannedMetadataFbs(builder)
            }
            
            val metadataVector = SymbolCacheTable.createMetadataVector(builder, metadataOffsets)
            
            SymbolCacheTable.startSymbolCacheTable(builder)
            SymbolCacheTable.addMetadata(builder, metadataVector)
            val rootOffset = SymbolCacheTable.endSymbolCacheTable(builder)
            
            SymbolCacheTable.finishSymbolCacheTableBuffer(builder, rootOffset)
            
            val buffer = builder.dataBuffer()
            
            cacheFile.parentFile?.mkdirs()
            RandomAccessFile(cacheFile, "rw").use { raf ->
                val channel = raf.channel
                val mappedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, buffer.remaining().toLong())
                mappedBuffer.put(buffer)
            }
        } catch (e: Exception) {
            System.err.println("Failed to save flatbuffers AST cache: ${e.message}")
        }
    }
}
