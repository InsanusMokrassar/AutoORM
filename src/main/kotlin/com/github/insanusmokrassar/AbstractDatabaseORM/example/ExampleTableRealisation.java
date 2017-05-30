package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleOperations;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExampleTableRealisation implements ExampleTable{

    protected final TableProvider<Example, ExampleOperations> provider;

    public ExampleTableRealisation(TableProvider<Example, ExampleOperations> provider) {
        this.provider = provider;
    }

    @NotNull
    public List<ExampleOperations> findNameBirthdayWhereNameIs(@NotNull String name) {
        SearchQueryCompiler queryCompiler = provider.getEmptyQuery();
        queryCompiler.setNeededFields("name", "birthday", "old");
        queryCompiler.field("name", false);
        queryCompiler.filter("eq", name);
        Collection<ExampleOperations> result = provider.find(queryCompiler);
        return new ArrayList<ExampleOperations>(result);
    }

    public void insert(@NotNull Example what) {
        provider.insert(what);
    }
}
