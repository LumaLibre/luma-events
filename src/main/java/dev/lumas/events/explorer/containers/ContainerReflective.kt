package dev.lumas.events.explorer.containers

abstract class ContainerReflective {
    @Suppress("PropertyName")
    var FIELD_NAME: String? = null
        internal set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContainerReflective

        return FIELD_NAME == other.FIELD_NAME
    }

    override fun hashCode(): Int {
        return FIELD_NAME?.hashCode() ?: 0
    }

    override fun toString(): String {
        return FIELD_NAME ?: "${this.javaClass.simpleName}(FIELD_NAME=Unknown)"
    }
}