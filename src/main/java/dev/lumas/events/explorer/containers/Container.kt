package dev.lumas.events.explorer.containers

import dev.lumas.core.model.Service
import dev.lumas.core.util.ContextLogger

/**
 * Abstract base class for managing collections of objects that extend [ContainerReflective].
 * This class implements functionality to map, enumerate, and retrieve these objects by their
 * associated field names.
 *
 * @param T the type of object managed by this container, extending [ContainerReflective].
 */
abstract class Container<T : ContainerReflective>(val registry: ExplorerRegistry<T>) : Service {

    companion object {
        val LOGGER: ContextLogger = ContextLogger.getLogger(true)
    }

    protected val keys: MutableMap<String, T> = LinkedHashMap()
    private var enumerated = false

    override fun register() {
        ensureEnumerated()
    }

    override fun unregister() {
        // no-op
    }

    /**
     * Lists the fields of this container, populating the [keys] map with the field names and their corresponding objects.
     */
    protected fun ensureEnumerated() {
        if (!enumerated) {
            enumerated = true
            enumerate()
            if (keys.isNotEmpty()) {
                registry.register(this)
            } else {
                LOGGER.info("No applicable fields found in ${this::class.java.simpleName}, ignoring.")
            }
        }
    }

    /**
     * Retrieves the map of field names to objects in this container.
     */
    fun asMap(): Map<String, T> {
        ensureEnumerated();
        return keys
    }

    /**
     * Retrieves the collection of objects in this container.
     */
    fun values(): Collection<T> {
        ensureEnumerated();
        return keys.values
    }

    /**
     * Retrieves an object from this container by its field name.
     */
    fun valueOf(name: String): T? {
        ensureEnumerated();
        return keys[name]
    }

    /**
     * Alias for [valueOf]
     */
    fun fromString(name: String): T? {
        return valueOf(name)
    }


    /**
     * Retrieves the type of objects managed by this container.
     */
    protected fun type(): Class<T> {
        var cls: Class<*>? = this::class.java
        while (cls != null) {
            val annotation = cls.getAnnotation(ContainerType::class.java)
            if (annotation != null) {
                @Suppress("UNCHECKED_CAST")
                return annotation.value.java as Class<T>
            }
            cls = cls.superclass
        }
        throw IllegalStateException("No @ContainerType annotation found on ${this::class.java.simpleName} or its superclasses")
    }


    /**
     * Lists the fields of this container, populating the [keys] map with the field names and their corresponding objects.
     */
    private fun enumerate() {
        val type = type()
        this::class.java.declaredFields.forEach { field ->
            if (field.type == type) {
                try {
                    field.isAccessible = true
                    val containerReflective = field.get(null) as? T
                    if (containerReflective != null) {
                        val fieldName = field.name
                        containerReflective.FIELD_NAME = fieldName
                        keys[fieldName] = containerReflective
                    } else {
                        LOGGER.warning("Field '${field.name}' in ${this.javaClass.simpleName} is null! (Was it removed?)")
                    }
                } catch (e: Throwable) {
                    LOGGER.error("Could not access or cast field '${field.name}' in ${this.javaClass.simpleName}", e)
                }
            }
        }
    }
}