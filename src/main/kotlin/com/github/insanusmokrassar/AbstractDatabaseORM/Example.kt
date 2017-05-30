package com.github.insanusmokrassar.AbstractDatabaseORM

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
    val config = JSONIObject(configStringBuffer.toString())
    val databaseConnect = DatabaseManager(config).getDatabaseConnect("Example")
    val table = databaseConnect.getTable(ExampleTable::class, Example::class, ExampleOperations::class)
    table.insert(object : Example {
        override val id: Int? = null
        override val name: String = "Tom"
        override val birthday: String = "09.05.1995"
        override var old: Int = 19
    })
    table.findNameBirthdayWhereNameIs("Tom").forEach {
        Logger.getGlobal().info(it.toStringExample())
    }
    databaseConnect.close()
}

fun Example.toStringExample(): String {
    return "Example{id=$id, name=$name, birthday=$birthday, old=$old}"
}
