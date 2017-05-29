package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import org.jetbrains.annotations.NotNull;

public class ExampleRealisation implements Example {
    public Integer id;
    public final String name;
    public final String birthday;

    protected Boolean invalid = false;

    public ExampleRealisation(String name, String birthday) {
        this.name = name;
        this.birthday = birthday;
    }

    @NotNull
    public String getName() {
        if (invalid) {
            throw new IllegalStateException("Object is invalidated");
        }
        return name;
    }

    @NotNull
    public String getBirthday() {
        if (invalid) {
            throw new IllegalStateException("Object is invalidated");
        }
        return birthday;
    }

    public Integer getId() {
        return id;
    }
}
