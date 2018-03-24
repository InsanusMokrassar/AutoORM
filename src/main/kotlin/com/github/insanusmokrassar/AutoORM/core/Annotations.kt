package com.github.insanusmokrassar.AutoORM.core

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class PrimaryKey

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Autoincrement

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class OrderBy(val ascend: Boolean = true)