package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.ConfigReader
import com.github.insanusmokrassar.IObjectKRealisations.JSONIObject
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.logging.LogManager
import java.util.logging.Logger

fun main(args: Array<String>) {
    println(System.getenv("PWD"))
    try {
        FileInputStream(args[0]).use {
            println(it)
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
    ConfigReader(config)
    Logger.getGlobal().info("HelloWorld")
}