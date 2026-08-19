package com.anyonehub.abl.annotations

/**
 * Marks a class that needs to be dynamically compiled and loaded by the runtime engine.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AblCompile
