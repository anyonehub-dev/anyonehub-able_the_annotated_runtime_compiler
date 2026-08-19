package com.anyonehub.abl.annotations

/**
 * Tags dependencies that the ModuleResolver must inject at runtime before execution.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class AblInject
