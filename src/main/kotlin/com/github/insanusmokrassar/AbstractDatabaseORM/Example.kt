package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.DatabaseManager
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleOperations
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable
import com.github.insanusmokrassar.IObjectKRealisations.JSONIObject
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.logging.LogManager
import java.util.logging.Logger

fun main(args: Array<String>) {
    val random = Random()
    try {
        FileInputStream(args[0]).use {
            LogManager.getLogManager().readConfiguration(it)
        }
    } catch (e: Exception) {
        println("Can't load logger preferences: {$e}")
    }
    val configStringBuffer = StringBuffer()
    val scanner = Scanner(File(args[1]))
    while (scanner.hasNextLine()) {
        val current : String = scanner.nextLine()
        configStringBuffer.append("$current\n\r")
    }
    var startTime = Date().time
    val config = JSONIObject(configStringBuffer.toString())
    val databaseConnect = DatabaseManager(config).getDatabaseConnect("Example")
    val table = databaseConnect.getTable(ExampleTable::class, Example::class, ExampleOperations::class)
    table.updateWhereNameIsOrOldIn(
            object: Example {
                override val id: Int? = null
                override val name: String = "IAm"
                override val birthday: String = "nothing"
                override var old: Int = 101
            },
            "Gosha",
            80,
            100
    )
    Logger.getGlobal().info("SelectTime: ${Date().time - startTime} ms")
    databaseConnect.close()
}

fun Example.toStringExample(): String {
    return "Example{id=$id, name=$name, birthday=$birthday, old=$old}"
}
