package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter

fun <T>KCallable<T>.intsancesKClass() : KClass<*>{
    return this.instanceParameter?.type?.classifier as KClass<*>
}

fun KCallable<*>.isNullable() : Boolean {
    return this.returnType.isMarkedNullable
}

fun KCallable<*>.returnClass() : KClass<*> {
    return this.returnType.classifier as KClass<*>
}

fun <T>KCallable<T>.isField() : Boolean {
    return this is KProperty<T>
}

fun <T>KCallable<T>.isFunction() : Boolean {
    return this is KFunction<T>
}

fun KCallable<*>.isReturnNative() : Boolean {
    return nativeTypes.contains(this.returnClass())
}

fun KProperty<*>.isPrimaryField() : Boolean {
    this.annotations.forEach {
        if (it.annotationClass == PrimaryKey::class) {
            return@isPrimaryField true
        }
    }
    return false
}

fun KProperty<*>.toJavaPropertyString() : String {
    val returnClass = returnType.classifier as KClass<*>
    val returnedType = StringBuilder()
    if (returnClass.javaPrimitiveType != null && !isNullable()) {
        returnedType.append(returnClass.javaPrimitiveType!!.simpleName)
        return returnedType.toString()
    } else {
        returnedType.append(returnClass.javaObjectType.simpleName)
    }
    if (returnType.arguments.isNotEmpty()) {
        returnedType.append("<")
        val arguments = returnType.arguments
        arguments.forEach {
            val typeClass = it.type!!.classifier as KClass<*>
            returnedType.append(typeClass.javaObjectType.simpleName)
            if (arguments.indexOf(it) < arguments.size - 1) {
                returnedType.append(", ")
            }
            returnedType.append(">")
        }
    }

    return returnedType.toString()
}

fun KProperty<*>.isMutable() : Boolean {
    return this is KMutableProperty
}

inline fun <T> Iterable<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}

fun String.asSQLString() : String {
    if (this.matches(Regex("^\'.*\'$"))) {
        return this
    } else {
        return "\'$this\'"
    }
}

fun KClass<*>.getRequiredInConstructor() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*> && (it !is KMutableProperty<*> || !it.isNullable())
    } as List<KProperty<*>>
}

fun KClass<*>.getVariables() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*>
    } as List<KProperty<*>>
}

fun KClass<*>.getVariablesToOverride() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*> && it.isAbstract
    } as List<KProperty<*>>
}

fun KClass<*>.getMethods() : List<KFunction<*>> {
    return this.members.filter {
        it is KFunction<*>
    } as List<KFunction<*>>
}

fun KClass<*>.getMethodsToOverride() : List<KFunction<*>> {
    return this.members.filter {
        it is KFunction<*> && it.isAbstract
    } as List<KFunction<*>>
}

fun KClass<*>.getPackage(): String {
    return this.qualifiedName!!.removeSuffix(".${this.simpleName}")
}
