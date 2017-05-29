package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleOperations;

public class ExampleOperationsRealisation extends ExampleRealisation implements ExampleOperations {

    protected final TableProvider<Example, ExampleOperations> provider;

    public ExampleOperationsRealisation(TableProvider<Example, ExampleOperations> provider, String name, String birthday, Integer id) {
        super(name, birthday);
        this.id = id;
        this.provider = provider;
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
