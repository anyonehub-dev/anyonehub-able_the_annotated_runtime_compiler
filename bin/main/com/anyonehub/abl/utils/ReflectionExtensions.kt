package com.anyonehub.abl.utils

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache for class-level annotations to minimize reflection overhead at runtime.
 */
object AnnotationCache {
    private val classAnnotationCache = ConcurrentHashMap<KClass<*>, List<Annotation>>()

    fun getAnnotations(kClass: KClass<*>): List<Annotation> {
        return classAnnotationCache.getOrPut(kClass) { kClass.annotations }
    }

    fun clear() {
        classAnnotationCache.clear()
    }
}

/**
 * Checks if the annotated element has the specified ABL annotation.
 * Inlined to avoid reflection overhead on the generic type resolution.
 */
inline fun <reified T : Annotation> KAnnotatedElement.hasAblAnnotation(): Boolean {
    return this.hasAnnotation<T>()
}

/**
 * Retrieves the specified ABL annotation from the element if present.
 */
inline fun <reified T : Annotation> KAnnotatedElement.getAblAnnotation(): T? {
    return this.findAnnotation<T>()
}

/**
 * Optimized check for class-level annotations utilizing the cache.
 */
inline fun <reified T : Annotation> KClass<*>.hasCachedAnnotation(): Boolean {
    return AnnotationCache.getAnnotations(this).any { it is T }
}

/**
 * Optimized retrieval for class-level annotations utilizing the cache.
 */
inline fun <reified T : Annotation> KClass<*>.getCachedAnnotation(): T? {
    return AnnotationCache.getAnnotations(this).filterIsInstance<T>().firstOrNull()
}
