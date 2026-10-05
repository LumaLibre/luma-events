package dev.lumas.events.configurable;

import dev.lumas.events.annotation.MapTied;
import io.leangen.geantyref.GenericTypeReflector;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.NamingScheme;
import org.spongepowered.configurate.util.NamingSchemes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
@UtilityClass
public final class ConfigSchema {

    public static final NamingScheme NAMING_SCHEME = NamingSchemes.LOWER_CASE_DASHED;

    private static final Map<Class<?>, List<SchemaField>> FIELDS = new ConcurrentHashMap<>();

    public record SchemaField(String key, String legacyKey, Type type, boolean mapSpecific) {
    }

    public static List<SchemaField> fields(Class<?> type) {
        return FIELDS.computeIfAbsent(type, ConfigSchema::discoverFields);
    }

    public static void normalizeKeys(ConfigurationNode node, Type type) throws SerializationException {
        if (node.virtual()) {
            return;
        }
        Class<?> raw = GenericTypeReflector.erase(type);
        if (isObject(raw)) {
            if (!node.isMap()) {
                return;
            }
            renameLegacyKeys(node, raw);
            for (SchemaField field : fields(raw)) {
                normalizeKeys(node.node(field.key()), field.type());
            }
        } else if (Collection.class.isAssignableFrom(raw)) {
            Type element = typeParameter(type, Collection.class, 0);
            if (element != null) {
                for (ConfigurationNode child : node.childrenList()) {
                    normalizeKeys(child, element);
                }
            }
        } else if (Map.class.isAssignableFrom(raw)) {
            Type value = typeParameter(type, Map.class, 1);
            if (value != null) {
                for (ConfigurationNode child : node.childrenMap().values()) {
                    normalizeKeys(child, value);
                }
            }
        }
    }

    public static void merge(ConfigurationNode target, ConfigurationNode source, Type type, boolean overwrite) throws SerializationException {
        if (source.virtual()) {
            return;
        }
        if (target.virtual()) {
            target.from(source);
            return;
        }
        Class<?> raw = GenericTypeReflector.erase(type);
        if (isObject(raw) && target.isMap() && source.isMap()) {
            for (SchemaField field : fields(raw)) {
                merge(target.node(field.key()), source.node(field.key()), field.type(), overwrite);
            }
            return;
        }
        if (overwrite) {
            target.from(source);
        }
    }

    public static void extractCommon(List<? extends ConfigurationNode> sources, Type type, ConfigurationNode shared) throws SerializationException {
        for (SchemaField field : fields(GenericTypeReflector.erase(type))) {
            if (field.mapSpecific()) {
                continue;
            }
            List<ConfigurationNode> values = sources.stream().map(source -> (ConfigurationNode) source.node(field.key())).toList();
            if (values.isEmpty() || values.stream().anyMatch(ConfigurationNode::virtual)) {
                continue;
            }
            if (hasMapSpecificFields(field.type())) {
                if (values.stream().allMatch(ConfigurationNode::isMap)) {
                    extractCommon(values, field.type(), shared.node(field.key()));
                }
            } else if (values.stream().allMatch(value -> Objects.equals(value.raw(), values.getFirst().raw()))) {
                shared.node(field.key()).from(values.getFirst());
            }
        }
    }

    public static void removeMatching(ConfigurationNode target, ConfigurationNode base, Type type) {
        for (SchemaField field : fields(GenericTypeReflector.erase(type))) {
            ConfigurationNode value = target.node(field.key());
            ConfigurationNode baseValue = base.node(field.key());
            if (value.virtual() || baseValue.virtual()) {
                continue;
            }
            if (isObject(GenericTypeReflector.erase(field.type())) && value.isMap() && baseValue.isMap()) {
                removeMatching(value, baseValue, field.type());
                if (value.empty()) {
                    target.removeChild(field.key());
                }
            } else if (Objects.equals(value.raw(), baseValue.raw())) {
                target.removeChild(field.key());
            }
        }
    }

    public static void split(ConfigurationNode source, Type type, ConfigurationNode shared, ConfigurationNode specific) throws SerializationException {
        for (SchemaField field : fields(GenericTypeReflector.erase(type))) {
            ConfigurationNode value = source.node(field.key());
            if (value.virtual()) {
                continue;
            }
            if (field.mapSpecific()) {
                specific.node(field.key()).from(value);
            } else if (hasMapSpecificFields(field.type())) {
                split(value, field.type(), shared.node(field.key()), specific.node(field.key()));
            } else {
                shared.node(field.key()).from(value);
            }
        }
    }

    private static boolean hasMapSpecificFields(Type type) {
        Class<?> raw = GenericTypeReflector.erase(type);
        if (!isObject(raw)) {
            return false;
        }
        for (SchemaField field : fields(raw)) {
            if (field.mapSpecific() || hasMapSpecificFields(field.type())) {
                return true;
            }
        }
        return false;
    }

    private static void renameLegacyKeys(ConfigurationNode node, Class<?> type) throws SerializationException {
        Map<String, String> keysByLegacyName = new HashMap<>();
        for (SchemaField field : fields(type)) {
            keysByLegacyName.put(field.legacyKey().toLowerCase(Locale.ROOT), field.key());
        }

        Map<Object, Object> renames = new LinkedHashMap<>();
        for (Object key : node.childrenMap().keySet()) {
            String renamed = keysByLegacyName.get(String.valueOf(key).toLowerCase(Locale.ROOT));
            if (renamed != null && !renamed.equals(key)) {
                renames.put(key, renamed);
            }
        }
        if (renames.isEmpty()) {
            return;
        }

        Map<Object, ConfigurationNode> children = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            Object key = renames.getOrDefault(entry.getKey(), entry.getKey());
            if (renames.containsKey(entry.getKey()) && !node.node(key).virtual()) {
                continue;
            }
            children.put(key, entry.getValue().copy());
        }

        node.set(Collections.emptyMap());
        for (Map.Entry<Object, ConfigurationNode> entry : children.entrySet()) {
            node.node(entry.getKey()).from(entry.getValue());
        }
    }

    private static boolean isObject(Class<?> type) {
        return type.isAnnotationPresent(ConfigSerializable.class);
    }

    private static @Nullable Type typeParameter(Type type, Class<?> owner, int index) {
        return GenericTypeReflector.getTypeParameter(type, owner.getTypeParameters()[index]);
    }

    private static List<SchemaField> discoverFields(Class<?> type) {
        List<SchemaField> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                fields.add(new SchemaField(
                        NAMING_SCHEME.coerce(field.getName()),
                        field.getName(),
                        field.getGenericType(),
                        field.isAnnotationPresent(MapTied.class)
                ));
            }
        }
        return List.copyOf(fields);
    }
}
