package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleOperations;
import org.jetbrains.annotations.NotNull;

public class ExampleRealisation implements Example {
    protected Integer id;
    public String name;
    public String birthday;

    protected Boolean invalid = false;

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
}
