package com.github.insanusmokrassar.AutoORM

import com.github.insanusmokrassar.AutoORM.core.DatabaseManager
import com.github.insanusmokrassar.AutoORM.example.UserInterfaces.Example
import com.github.insanusmokrassar.AutoORM.example.UserInterfaces.ExampleOperations
import com.github.insanusmokrassar.AutoORM.example.UserInterfaces.ExampleTable
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
    val config = JSONIObject(configStringBuffer.toString())
    val databaseConnect = DatabaseManager(config).databasesPools["Example"]!!.getConnection()
    val table = databaseConnect.getTable(ExampleTable::class, Example::class, ExampleOperations::class)
    try {
        while(true) {
            var startTime = Date().time
            table.removeAll()
            Logger.getGlobal().info("Remove All time: ${Date().time - startTime} ms")
            startTime = Date().time
            for (i: Int in 0..1000) {
                table.insert(
                        object : Example {
                            override val id: Int? = null
                            override var name: String = "IAm"
                            override val birthday: String = "nothing"
                            override var old: Int = random.nextInt(100)
                        }
                )
            }
            Logger.getGlobal().info("100000 inserts time: ${Date().time - startTime} ms")
            startTime = Date().time
            table.removeAll()
            Logger.getGlobal().info("Remove all time: ${Date().time - startTime} ms")
            startTime = Date().time
            databaseConnect.start()
            for (i: Int in 0..1000) {
                table.insert(
                        object : Example {
                            override val id: Int? = null
                            override var name: String = "IAm"
                            override val birthday: String = "nothing"
                            override var old: Int = random.nextInt(100)
                        }
                )
            }
            databaseConnect.submit()
            Logger.getGlobal().info("100000 Inserts in transaction time: ${Date().time - startTime} ms")
            startTime = Date().time
            table.updateWhereNameIsAndOldIn(
                    object : Example {
                        override val id: Int? = null
                        override var name: String = "Bash"
                        override val birthday: String = "nothing"
                        override var old: Int = 101
                    },
                    "IAm",
                    80,
                    100
            )
            Logger.getGlobal().info("UpdateTime: ${Date().time - startTime} ms")
            table.findNameBirthdayWhereNameIs("IAm").forEach {
                it.name = "Bard"
                it.update()
                Logger.getGlobal().info(it.toStringExample())
            }
            return
        }
    } finally {
        databaseConnect.free()
    }
}

fun Example.toStringExample(): String {
    return "Example{id=$id, name=$name, birthday=$birthday, old=$old}"
}
