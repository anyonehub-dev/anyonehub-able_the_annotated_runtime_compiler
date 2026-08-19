package com.anyonehub.abl.scanner

import com.anyonehub.abl.annotations.AblCompile
import com.anyonehub.abl.annotations.AblEntryPoint
import com.anyonehub.abl.annotations.AblInject
import com.anyonehub.abl.annotations.AblModule
import com.anyonehub.abl.utils.hasAblAnnotation
import com.anyonehub.abl.utils.hasCachedAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

import com.anyonehub.abl.cache.AblCacheManager
import com.anyonehub.abl.cache.ScannedMetadataFbsData

/**
 * Represents the raw metadata extracted from scanning a single class.
 */
data class ScannedMetadata(
    val targetClass: KClass<*>,
    val isModule: Boolean,
    val isCompileTarget: Boolean,
    val entryPoints: List<KFunction<*>>,
    val injectableProperties: List<KProperty1<*, *>>,
    val injectableConstructorParameters: Boolean
)

class AblRuntimeScanner(private val cacheManager: AblCacheManager? = null) {

    /**
     * Sweeps the target's members, properties, and constructors using reflection.
     * Identifies modules, entry points, and injectable targets.
     */
    fun scan(target: KClass<*>): ScannedMetadata {
        val isModule = target.hasCachedAnnotation<AblModule>()
        val isCompileTarget = target.hasCachedAnnotation<AblCompile>()

        val entryPoints = target.declaredMemberFunctions.filter { function ->
            function.hasAblAnnotation<AblEntryPoint>()
        }

        val injectableProperties = target.declaredMemberProperties.filter { property ->
            property.hasAblAnnotation<AblInject>()
        }

        val primaryConstructor = target.primaryConstructor
        val injectableConstructorParameters = primaryConstructor?.hasAblAnnotation<AblInject>() == true

        return ScannedMetadata(
            targetClass = target,
            isModule = isModule,
            isCompileTarget = isCompileTarget,
            entryPoints = entryPoints,
            injectableProperties = injectableProperties,
            injectableConstructorParameters = injectableConstructorParameters
        )
    }
    
    /**
     * Batch scans multiple targets, utilizing FlatBuffers cache if available.
     */
    fun scanMultiple(targets: List<KClass<*>>): List<ScannedMetadata> {
        if (cacheManager != null) {
            val cached = cacheManager.loadCache()
            if (cached != null && cached.isNotEmpty()) {
                println("AblRuntimeScanner: Loaded AST metadata from FlatBuffers cache")
                return cached.map { restoreFromCache(it) }
            }
        }
        
        println("AblRuntimeScanner: Performing reflective sweep")
        val scanned = targets.map { scan(it) }
        
        if (cacheManager != null) {
            val fbsData = scanned.map { convertToFbsData(it) }
            cacheManager.saveCache(fbsData)
            println("AblRuntimeScanner: Saved AST metadata to FlatBuffers cache")
        }
        
        return scanned
    }
    
    private fun restoreFromCache(cachedData: ScannedMetadataFbsData): ScannedMetadata {
        val kClass = Class.forName(cachedData.targetClass).kotlin
        
        val entryPoints = cachedData.entryPoints.mapNotNull { name ->
            kClass.declaredMemberFunctions.firstOrNull { it.name == name }
        }
        
        val injectables = cachedData.injectableProperties.mapNotNull { name ->
            kClass.declaredMemberProperties.firstOrNull { it.name == name }
        }
        
        return ScannedMetadata(
            targetClass = kClass,
            isModule = cachedData.isModule,
            isCompileTarget = cachedData.isCompileTarget,
            entryPoints = entryPoints,
            injectableProperties = injectables,
            injectableConstructorParameters = cachedData.injectableConstructorParameters
        )
    }
    
    private fun convertToFbsData(metadata: ScannedMetadata): ScannedMetadataFbsData {
        return ScannedMetadataFbsData(
            targetClass = metadata.targetClass.java.name,
            isModule = metadata.isModule,
            isCompileTarget = metadata.isCompileTarget,
            entryPoints = metadata.entryPoints.map { it.name },
            injectableProperties = metadata.injectableProperties.map { it.name },
            injectableConstructorParameters = metadata.injectableConstructorParameters
        )
    }
}
