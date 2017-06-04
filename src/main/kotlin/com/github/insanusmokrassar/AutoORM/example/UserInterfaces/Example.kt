package com.github.insanusmokrassar.AutoORM.example.UserInterfaces

import com.github.insanusmokrassar.AutoORM.core.Autoincrement
import com.github.insanusmokrassar.AutoORM.core.PrimaryKey

interface Example {
    @PrimaryKey
    @Autoincrement
    val id: Int?
    var name: String
    val birthday: String
    @PrimaryKey
    val old: Int?
}
