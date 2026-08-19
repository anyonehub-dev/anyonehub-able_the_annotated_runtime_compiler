package com.anyonehub.abl.di

import com.anyonehub.abl.annotations.AblInject
import com.anyonehub.abl.exceptions.AblScannerException
import com.anyonehub.abl.scanner.ExecutionMap
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class ModuleResolver {

    private val resolutionStack = mutableSetOf<KClass<*>>()

    /**
     * Resolves @AblModule classes and satisfies dependencies across both local 
     * and dynamic runtime ClassLoaders.
     * 
     * @param executionMap The structured map from the scanner.
     * @param classLoader Optional ClassLoader for dynamic runtime resolution.
     */
    fun resolve(executionMap: ExecutionMap, classLoader: ClassLoader? = null) {
        val loader = classLoader ?: Thread.currentThread().contextClassLoader

        // Phase 1: Resolve all modules and cache them in RuntimeContainer
        for (moduleClass in executionMap.modules) {
            resolveClass(moduleClass, loader)
        }

        // Phase 2: Resolve specific injection targets
        for ((targetClass, _) in executionMap.injectionTargets) {
            resolveClass(targetClass, loader)
        }
    }

    private fun resolveClass(kClass: KClass<*>, classLoader: ClassLoader): Any {
        if (resolutionStack.contains(kClass)) {
            val cycle = resolutionStack.joinToString(" -> ") { it.simpleName ?: it.java.name } + " -> ${kClass.simpleName}"
            throw AblScannerException("Circular dependency detected: $cycle")
        }

        // Return if already resolved (polymorphic lookup handles interfaces if bound by a module)
        @Suppress("UNCHECKED_CAST")
        val existing = RuntimeContainer.getInstance(kClass as KClass<Any>, classLoader)
        if (existing != null) return existing

        resolutionStack.add(kClass)

        try {
            val constructor = kClass.primaryConstructor
            val instance = if (constructor != null && constructor.hasAnnotation<AblInject>()) {
                val args = constructor.parameters.map { param ->
                    @Suppress("UNCHECKED_CAST")
                    val paramType = param.type.jvmErasure as KClass<Any>
                    RuntimeContainer.getInstance(paramType, classLoader) 
                        ?: resolveClass(paramType, classLoader)
                }.toTypedArray()
                
                constructor.isAccessible = true
                constructor.call(*args)
            } else if (constructor != null && constructor.parameters.isEmpty()) {
                constructor.isAccessible = true
                constructor.call()
            } else if (constructor == null) {
                // Try object instance or Java default constructor for non-Kotlin/interop classes
                kClass.objectInstance ?: kClass.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            } else {
                throw AblScannerException("Class ${kClass.qualifiedName} requires an @AblInject constructor or a no-arg constructor.")
            }

            // Property Injection
            for (property in kClass.memberProperties) {
                if (property.hasAnnotation<AblInject>()) {
                    if (property is KMutableProperty<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val propType = property.returnType.jvmErasure as KClass<Any>
                        val dependency = RuntimeContainer.getInstance(propType, classLoader) 
                            ?: resolveClass(propType, classLoader)
                        
                        property.isAccessible = true
                        property.setter.call(instance, dependency)
                    } else {
                        throw AblScannerException("Property ${property.name} in ${kClass.qualifiedName} is annotated with @AblInject but is a val (immutable). Must be var.")
                    }
                }
            }

            // Auto-cache and register the resolved instance
            @Suppress("UNCHECKED_CAST")
            RuntimeContainer.registerInstance(kClass as KClass<Any>, instance)

            return instance
        } catch (e: Exception) {
            if (e is AblScannerException) throw e
            throw AblScannerException("Failed to resolve dependencies for ${kClass.qualifiedName}", e)
        } finally {
            resolutionStack.remove(kClass)
        }
    }
}
