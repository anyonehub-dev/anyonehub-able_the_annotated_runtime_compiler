package com.anyonehub.abl.di

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object RuntimeContainer {
    private val classRegistry = ConcurrentHashMap<KClass<*>, Any>()
    private val nameRegistry = ConcurrentHashMap<String, Any>()
    private val factoryRegistry = ConcurrentHashMap<String, () -> Any>()

    fun <T : Any> registerInstance(type: KClass<T>, instance: T) {
        classRegistry[type] = instance
        nameRegistry[type.java.name] = instance
        type.qualifiedName?.let { nameRegistry[it] = instance }
    }

    fun registerFactory(typeName: String, factory: () -> Any) {
        factoryRegistry[typeName] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(type: KClass<T>, classLoader: ClassLoader? = null): T? {
        // 1. Direct Class Match
        val direct = classRegistry[type]
        if (direct != null) return direct as T

        // 2. Name / Factory Match
        val factory = type.qualifiedName?.let { factoryRegistry[it] } ?: type.java.name.let { factoryRegistry[it] }
        if (factory != null) {
            val instance = factory()
            if (type.isInstance(instance)) {
                registerInstance(type, instance as T)
                return instance
            }
        }

        // 3. Polymorphic / Assignability Match
        for ((registeredType, instance) in classRegistry) {
            if (registeredType.isSubclassOf(type)) {
                return instance as T
            }
        }

        return null
    }

    fun getInstanceByName(canonicalName: String, classLoader: ClassLoader? = null): Any? {
        // 1. Direct Name Match
        val direct = nameRegistry[canonicalName]
        if (direct != null) return direct

        // 2. Factory Match
        val factory = factoryRegistry[canonicalName]
        if (factory != null) {
            val instance = factory()
            val kClass = try {
                (classLoader ?: Thread.currentThread().contextClassLoader ?: this::class.java.classLoader)
                    .loadClass(canonicalName).kotlin
            } catch (e: Exception) {
                null
            }
            if (kClass != null) {
                classRegistry[kClass] = instance
            }
            nameRegistry[canonicalName] = instance
            return instance
        }
        
        // 3. Polymorphic Match via ClassLoader loading
        try {
            val targetClass = (classLoader ?: Thread.currentThread().contextClassLoader ?: this::class.java.classLoader)
                .loadClass(canonicalName)
                
            for ((_, instance) in classRegistry) {
                if (targetClass.isInstance(instance)) {
                    return instance
                }
            }
        } catch (e: Exception) {
            // ClassNotFound, safe to ignore
        }

        return null
    }

    fun clear() {
        classRegistry.clear()
        nameRegistry.clear()
        factoryRegistry.clear()
    }
}
