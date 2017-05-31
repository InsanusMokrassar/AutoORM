package com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces

interface ExampleTable {
    fun findNameBirthdayWhereNameIs(name: String): List<ExampleOperations>
    fun updateWhereNameIsOrOldIn(byThe: Example, name: String, first: Int, second: Int)
    fun insert(what: Example)
}