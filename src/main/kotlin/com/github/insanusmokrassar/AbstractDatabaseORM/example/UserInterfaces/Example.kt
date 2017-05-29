package com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces

import com.github.insanusmokrassar.AbstractDatabaseORM.core.PrimaryKey

interface Example {
    @PrimaryKey
    val id: Int?
    val name: String
    val birthday: String
}