package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.interfaces.SearchQueryCompiler;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.interfaces.TableProvider;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import org.jetbrains.annotations.NotNull;

public class ExampleRealisation implements Example {
    protected Integer id;
    public String name;
    public String birthday;

    protected final TableProvider<Example> provider;

    protected Boolean invalid = false;

    public ExampleRealisation(TableProvider<Example> provider) {
        this.provider = provider;
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

    public void update() {
        if (invalid) {
            throw new IllegalStateException("Object is invalidated");
        }
        provider.update(this, provider.getEmptyQuery());
    }

    public void insert() {
        if (invalid) {
            throw new IllegalStateException("Object is invalidated");
        }
        provider.insert(this);
    }

    public void remove() {
        if (invalid) {
            throw new IllegalStateException("Object is invalidated");
        }
        SearchQueryCompiler queryCompiler = provider.getEmptyQuery();
        queryCompiler.field("id", false);
        queryCompiler.filter("eq", id);
        provider.remove(queryCompiler);
    }
}
