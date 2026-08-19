package com.anyonehub.abl.annotations

/**
 * Identifies the exact function where the compiled execution pipeline should begin.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AblEntryPoint
