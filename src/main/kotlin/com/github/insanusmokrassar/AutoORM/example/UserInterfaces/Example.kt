package com.github.insanusmokrassar.AutoORM.example.UserInterfaces

import com.github.insanusmokrassar.AutoORM.core.PrimaryKey

interface Example {
    @PrimaryKey
    val id: Int?
    val name: String
    val birthday: String
    var old: Int
}
