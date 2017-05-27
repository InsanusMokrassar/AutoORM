package com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces

interface ExampleTable {
    fun findNameBirthdayWhereNameIs(name: String): List<Example>
}