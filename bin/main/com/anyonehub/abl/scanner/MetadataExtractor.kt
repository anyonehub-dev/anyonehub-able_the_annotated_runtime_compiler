package com.anyonehub.abl.scanner

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Safely extracts parameters and values from localized annotations.
 */
object MetadataExtractor {

    /**
     * Extracts properties of an annotation instance into a Map.
     * Essential for parsing runtime parameters defined within @AblModule or other annotations.
     */
    fun extractParameters(annotation: Annotation): Map<String, Any?> {
        val parameters = mutableMapOf<String, Any?>()
        try {
            val kClass = annotation.annotationClass
            for (property in kClass.memberProperties) {
                parameters[property.name] = property.getter.call(annotation)
            }
        } catch (e: Exception) {
            // In a production engine, this would route to EngineLogger
            System.err.println("Failed to extract parameters from annotation $annotation: ${e.message}")
        }
        return parameters
    }

    /**
     * Retrieves the fully qualified name of the target class safely.
     */
    fun extractClassName(target: KClass<*>): String {
        return target.qualifiedName ?: target.java.name
    }
}
