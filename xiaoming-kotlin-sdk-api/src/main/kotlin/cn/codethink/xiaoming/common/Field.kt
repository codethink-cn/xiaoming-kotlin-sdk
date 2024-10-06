/*
 * Copyright 2024 CodeThink Technologies and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.io.data.Raw
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.lang.reflect.Member
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/**
 * An annotation to specify the naming policy of fields in a class inherited [Data].
 *
 * The reason why we not use [JsonNaming] is that the naming functions in
 * [PropertyNamingStrategy] required a serialization or deserialization config.
 *
 * @author Chuanwise
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FIELD)
annotation class NamingPolicy(
    val policy: DefaultFieldNamingPolicy = DefaultFieldNamingPolicy.DEFAULT,
    val policyClass: KClass<out FieldNamingPolicy> = Nothing::class
)

interface FieldNamingPolicy {
    fun translate(name: String): String
}

/**
 * @author Chuanwise
 * @see PropertyNamingStrategies
 * @see FieldNamingPolicy
 */
enum class DefaultFieldNamingPolicy(
    private val base: PropertyNamingStrategies.NamingBase? = null
) : FieldNamingPolicy {
    DEFAULT,

    KEBAB_CASE(PropertyNamingStrategies.KebabCaseStrategy.INSTANCE),
    LOWER_CAMEL_CASE(PropertyNamingStrategies.LowerCamelCaseStrategy.INSTANCE),
    UPPER_CAMEL_CASE(PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE),
    SNAKE_CASE(PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE),
    UPPER_SNAKE_CASE(PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE),
    LOWER_CASE(PropertyNamingStrategies.LowerCaseStrategy.INSTANCE),
    LOWER_DOT_CASE(PropertyNamingStrategies.LowerDotCaseStrategy.INSTANCE);

    override fun translate(name: String): String = base?.translate(name) ?: name
}

/**
 * Specify default value for field.
 *
 * @author Chuanwise
 */
enum class DefaultValue {
    /**
     * The field is required and not-nullable, or exception will be thrown.
     */
    NULL,

    /**
     * Corresponding empty value as default value.
     */
    EMPTY,

    /**
     * No default value provided.
     */
    UNDEFINED
}

/**
 * An annotation to override some default settings of accessing and modifying
 * a delegated field in [Raw] class.
 *
 * @author Chuanwise
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Field(
    /**
     * By default, the name of the field is the same as the name of the property.
     * Use [Field.name] to override it like this:
     *
     * ```kt
     * // read this field by "raw.get("field", String::class.java)"
     * val field: String by raw
     *
     * // read this field by "raw.get("cn.codethink.xiaoming:extension-field", String::class.java)"
     * @Field("cn.codethink.xiaoming:extension-field")
     * val extensionField: String by raw
     * ```
     */
    val name: String = "",

    /**
     * If the type of field `T` is nullable, the default value of [nullable] is `true`.
     * If it's false, don't forget to provide a default value when calling [Raw.get].
     */
    val nullable: Tristate = Tristate.NULL,

    /**
     * If the field is optional, the default value of [optional] is `true`.
     */
    val optional: Tristate = Tristate.NULL,

    /**
     * @see DefaultValue
     */
    val defaultValue: DefaultValue = DefaultValue.UNDEFINED
)

fun Field?.nameOrDefault(
    defaultValue: String
): String = this?.name?.takeIf { it.isNotBlank() } ?: defaultValue

inline fun <reified T> Field?.nullableOrDefault(): Boolean = this?.nullable?.value ?: defaultNullable<T>()
inline fun <reified T> Field?.optionalOrDefault(): Boolean = this?.optional?.value ?: defaultOptional<T>()
inline fun <reified T> Field?.defaultValueFactoryOrNull(): Supplier<T?>? = when (this?.defaultValue) {
    null -> null
    DefaultValue.NULL -> {
        null
    }

    DefaultValue.EMPTY, DefaultValue.UNDEFINED -> {
        if (defaultValue == DefaultValue.UNDEFINED && (optional.value == false || (nullable.value == true))) {
            null
        } else {
            Supplier {
                if (T::class.java.isArray) {
                    java.lang.reflect.Array.newInstance(T::class.java.componentType, 0) as T
                }

                @Suppress("IMPLICIT_CAST_TO_ANY")
                when (T::class) {
                    String::class -> ""
                    Int::class -> 0
                    Long::class -> 0L
                    Short::class -> 0.toShort()
                    Byte::class -> 0.toByte()
                    Double::class -> 0.0
                    Float::class -> 0.0f
                    Char::class -> 0.toChar()
                    Boolean::class -> false
                    Map::class -> emptyMap<Nothing, Nothing>()
                    List::class -> emptyList<Nothing>()
                    Set::class -> emptySet<Nothing>()
                    else -> getOrConstruct(T::class.java)
                } as T
            }
        }
    }
}

private val KProperty<*>.declaringClass: Class<*>?
    get() = (this.javaField as Member? ?: this.javaGetter)?.declaringClass

fun applyNamingPolicyIfPresent(property: KProperty<*>): String {
    var name = property.name

    var namingPolicyAnn = property.annotations.firstOrNull { it is NamingPolicy } as NamingPolicy?
    if (namingPolicyAnn == null) {
        // If contained class is annotated with [Data], use the naming policy.
        val declaringClass = property.declaringClass
        if (declaringClass != null) {
            // Find the [Data] annotation in the owner class or its super classes.
            namingPolicyAnn = declaringClass.getAnnotation(NamingPolicy::class.java)
                ?: declaringClass.allAssignableClasses.firstOrNull {
                    it.annotations.any { ann -> ann is NamingPolicy }
                }?.annotations?.firstOrNull { it is NamingPolicy } as NamingPolicy?
        }
    }
    if (namingPolicyAnn == null) {
        return name
    }

    val namingPolicy = if (namingPolicyAnn.policyClass != Nothing::class) {
        getOrConstruct(namingPolicyAnn.policyClass.java)
    } else namingPolicyAnn.policy

    name = namingPolicy.translate(name)
    return name
}

inline operator fun <reified T : Any?> Raw.getValue(thisRef: Any?, property: KProperty<*>): T {
    val annotation = property.annotations.firstOrNull { it is Field } as Field?
    return get(
        name = annotation.nameOrDefault(applyNamingPolicyIfPresent(property)),
        type = object : TypeReference<T>() {}.type,
        optional = annotation.optionalOrDefault<T>(),
        nullable = annotation.nullableOrDefault<T>(),
        defaultValueFactory = { annotation.defaultValueFactoryOrNull<T>() }
    ) as T
}

inline operator fun <reified T : Any?> Raw.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val annotation = property.annotations.firstOrNull { it is Field } as Field?
    return set(
        name = annotation.nameOrDefault(applyNamingPolicyIfPresent(property)),
        value = value,
        optional = annotation.optionalOrDefault<T>(),
        nullable = annotation.nullableOrDefault<T>()
    )
}