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

class AblRuntimeScanner {

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
     * Batch scans multiple targets.
     */
    fun scanMultiple(targets: List<KClass<*>>): List<ScannedMetadata> {
        return targets.map { scan(it) }
    }
}
