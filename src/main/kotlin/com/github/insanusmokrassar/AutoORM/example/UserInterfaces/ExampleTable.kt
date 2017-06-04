package com.github.insanusmokrassar.AutoORM.example.UserInterfaces

interface ExampleTable {
    fun findNameBirthdayWhereNameIsOn(name: String, page: Int, size: Int): List<ExampleOperations>
    fun updateWhereNameIsAndOldIn(byThe: Example, name: String, first: Int, second: Int)
    fun insert(what: Example)
    fun removeAll()
}