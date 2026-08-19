package com.anyonehub.abl.scanner

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Represents the in-memory execution map defining instantiation and injection order.
 */
data class ExecutionMap(
    val modules: Set<KClass<*>>,
    val compileTargets: Set<KClass<*>>,
    val injectionTargets: Map<KClass<*>, InjectionRequirements>,
    val entryPoints: Map<KClass<*>, List<KFunction<*>>>
)

data class InjectionRequirements(
    val requiresConstructorInjection: Boolean,
    val propertiesToInject: List<String>
)

class DependencyGraphBuilder {

    /**
     * Processes raw scanned metadata to build an in-memory execution map.
     * Determines what needs to be instantiated and injected before the entry point is called.
     */
    fun buildGraph(scannedData: List<ScannedMetadata>): ExecutionMap {
        val modules = mutableSetOf<KClass<*>>()
        val compileTargets = mutableSetOf<KClass<*>>()
        val injectionTargets = mutableMapOf<KClass<*>, InjectionRequirements>()
        val entryPoints = mutableMapOf<KClass<*>, List<KFunction<*>>>()

        for (metadata in scannedData) {
            if (metadata.isModule) {
                modules.add(metadata.targetClass)
            }
            if (metadata.isCompileTarget) {
                compileTargets.add(metadata.targetClass)
            }

            if (metadata.entryPoints.isNotEmpty()) {
                entryPoints[metadata.targetClass] = metadata.entryPoints
            }

            if (metadata.injectableConstructorParameters || metadata.injectableProperties.isNotEmpty()) {
                injectionTargets[metadata.targetClass] = InjectionRequirements(
                    requiresConstructorInjection = metadata.injectableConstructorParameters,
                    propertiesToInject = metadata.injectableProperties.map { it.name }
                )
            }
        }

        return ExecutionMap(
            modules = modules,
            compileTargets = compileTargets,
            injectionTargets = injectionTargets,
            entryPoints = entryPoints
        )
    }
}
