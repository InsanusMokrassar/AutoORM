package com.github.insanusmokrassar.AbstractDatabaseORM

import java.io.FileInputStream
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
    Logger.getGlobal().info("HelloWorld")
}