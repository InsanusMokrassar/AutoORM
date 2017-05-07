package com.github.insanusmokrassar.AbstractDatabaseORM;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.*;

public class Example {
    public static void main(String[] args) {
        try(InputStream is = Example.class.getResourceAsStream(args[0])) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException | IndexOutOfBoundsException | NullPointerException e) {
            System.err.println("Now logging is switched off because config was not found or path is not set:" + e.getMessage());
            e.printStackTrace();
        }
        Logger.getGlobal().finest("HelloWorld");
    }
}
