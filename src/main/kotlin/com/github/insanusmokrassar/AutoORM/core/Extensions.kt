package com.github.insanusmokrassar.AutoORM.core

import java.lang.reflect.Method
import kotlin.reflect.*
import kotlin.reflect.full.instanceParameter

/**
 * List of classes which can be primitive
 */
val nativeTypes = listOf(
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        String::class,
        Boolean::class
)

/**
 * @return Экземпляр KClass, содержащий данный KCallable объект.
 */
fun <T>KCallable<T>.intsanceKClass() : KClass<*> {
    return this.instanceParameter?.type?.classifier as KClass<*>
}

/**
 * @return true если значение параметра может быть null.
 */
fun KCallable<*>.isNullable() : Boolean {
    return this.returnType.isMarkedNullable
}

/**
 * @return Экземпляр KClass, возвращаемый KCallable.
 */
fun KCallable<*>.returnClass() : KClass<*> {
    return this.returnType.classifier as KClass<*>
}

/**
 * @return true, если KCallable - поле какого-то класса.
 */
fun <T>KCallable<T>.isField() : Boolean {
    return this is KProperty<T>
}


/**
 * @return true, если KCallable - функция.
 */
fun <T>KCallable<T>.isFunction() : Boolean {
    return this is KFunction<T>
}

/**
 * @return true, если возвращает некоторый примитив.
 */
fun KCallable<*>.isReturnNative() : Boolean {
    return nativeTypes.contains(this.returnClass())
}

/**
 * @return Преобразованная строка вида "String", "int" если не nullable, "Integer" если nullable, "List<String>".
 */
fun KCallable<*>.toJavaPropertyString() : String {
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

/**
 * @return Преобразованная строка вида "String", "int" если не nullable, "Integer" если nullable, "List<String>".
 */
fun KType.toJavaPropertyString(printInvariants: Boolean = false) : String {
    val returnClass = classifier as KClass<*>
    val returnedType = StringBuilder()
    if (returnClass.javaPrimitiveType != null) {
        returnedType.append(returnClass.javaPrimitiveType!!.simpleName)
        return returnedType.toString()
    } else {
        returnedType.append(returnClass.javaObjectType.simpleName)
    }
    val genericBuilder = StringBuilder()
    if (arguments.isNotEmpty()) {
        genericBuilder.append("<")
        val arguments = arguments
        arguments.forEach {
            if (it.variance != null && it.variance != KVariance.INVARIANT) {
                val typeClass = it.type?.classifier as KClass<*>
                genericBuilder.append(typeClass.javaObjectType.simpleName)
                if (!arguments.isLast(it)) {
                    genericBuilder.append(", ")
                }
            } else {
                if (printInvariants) {
                    genericBuilder.append(it.type.toString())
                    if (!arguments.isLast(it)) {
                        genericBuilder.append(", ")
                    }
                }
            }
            genericBuilder.append(">")
        }
    }
    if (genericBuilder.toString() != "<>") {
        returnedType.append(genericBuilder)
    }
    return returnedType.toString()
}

fun KType.toJavaClass(): Class<*> {
    return (this.classifier as KClass<*>).java
}

/**
 * @return Преобразованная строка вида "String", "int" если не nullable, "Integer" если nullable, "List<String>".
 */
fun KClass<*>.toJavaPropertyString(isNullable: Boolean) : String {
    if (this == Unit::class) {
        return "void"
    }
    val returnedType = StringBuilder()
    if (javaPrimitiveType != null && !isNullable) {
        returnedType.append(javaPrimitiveType!!.simpleName)
        return returnedType.toString()
    } else {
        returnedType.append(javaObjectType.simpleName)
    }
    return returnedType.toString()
}


/**
 * @return Список KCallable объектов, помеченных аннотацией [PrimaryKey].
 */
fun KClass<*>.getPrimaryFields() : List<KCallable<*>> {
    return this.members.filter {
        it is KProperty<*> && it.isPrimaryField()
    }
}

/**
 * @return true если объект помечен аннотацией [PrimaryKey].
 */
fun KProperty<*>.isPrimaryField() : Boolean {
    this.annotations.forEach {
        if (it.annotationClass == PrimaryKey::class) {
            return@isPrimaryField true
        }
    }
    return false
}

/**
 * @return true если объект помечен аннотацией [Autoincrement].
 */
fun KProperty<*>.isAutoincrement() : Boolean {
    this.annotations.forEach {
        if (it.annotationClass == Autoincrement::class) {
            return@isAutoincrement true
        }
    }
    return false
}

/**
 * @return true если поле является изменяемым.
 */
fun KProperty<*>.isMutable() : Boolean {
    return this is KMutableProperty
}

/**
 * @return Строка, пригодная для использования в SQL запросах.
 */
fun String.asSQLString() : String {
    if (this.matches(Regex("^\'.*\'$"))) {
        return this
    } else {
        return "\'$this\'"
    }
}

/**
 * @return Список объектов, которые должны быть включены в конструктор - val (неизменяемые)
 * и not null поля.
 */
fun KClass<*>.getRequiredInConstructor() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*> && (!it.isMutable() || !it.isNullable())
    } as List<KProperty<*>>
}

/**
 * @return Список полей класса.
 */
fun KClass<*>.getVariables() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*>
    } as List<KProperty<*>>
}

/**
 * @return Список полей класса, которые необходимо реализовать в наследниках.
 */
fun KClass<*>.getVariablesToOverride() : List<KProperty<*>> {
    return this.members.filter {
        it is KProperty<*> && it.isAbstract
    } as List<KProperty<*>>
}

/**
 * @return Список методов класса.
 */
fun KClass<*>.getMethods() : List<KFunction<*>> {
    return this.members.filter {
        it is KFunction<*>
    } as List<KFunction<*>>
}

/**
 * @return Список методов класса, которые необходимо реализовать в наследниках.
 */
fun KClass<*>.getMethodsToOverride() : List<KFunction<*>> {
    return this.members.filter {
        it is KFunction<*> && it.isAbstract
    } as List<KFunction<*>>
}

fun Class<*>.getMethodsToOverride() : List<Method> {
    return arrayListOf(*this.methods)
}

/**
 * @return Строку-идентификатор пакета, в котором находится класс.
 */
fun KClass<*>.getPackage(): String {
    return this.qualifiedName!!.removeSuffix(".${this.simpleName}")
}

/**
 * @return true, если класс является интерфейсом.
 */
fun KClass<*>.isInterface(): Boolean {
    return isAbstract && constructors.isEmpty()
}

/**
 * @return Список слов, разделённых при помощи camelCase
 */
fun String.camelCaseWords(): List<String> {
    val result = ArrayList<String>()
    val current = StringBuilder()
    this.forEach {
        if (it.isUpperCase()) {
            if (!current.isEmpty()) {
                result.add(current.toString().toLowerCase())
                current.clear()
            }
        }
        current.append(it)
    }
    if (current.isNotEmpty()) {
        result.add(current.toString().toLowerCase())
    }
    return result
}

/**
 * Освобождает [StringBuilder] от содержимого.
 */
fun StringBuilder.clear() {
    replace(0, length, "")
}

/**
 * @return true, если объект [what] является последним в [List].
 */
fun <T>Collection<T>.isLast(what: T): Boolean {
    return indexOf(what) == size - 1
}

/**
 * Совершает выборку с помощью [by], возвращающей значение для каждого элемента.
 * @return Список из полей, выбранных с помощью лямбды [by].
 */
fun <T, R>Collection<T>.select(by: (T) -> R): List<R> {
    val result = ArrayList<R>()
    this.forEach {
        result.add(by(it))
    }
    return result
}
