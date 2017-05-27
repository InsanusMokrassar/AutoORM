package com.github.insanusmokrassar.AbstractDatabaseORM.example;

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example;
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExampleTableRealisation implements ExampleTable{

    protected final TableProvider<Example> provider;

    public ExampleTableRealisation(TableProvider<Example> provider) {
        this.provider = provider;
    }

    @NotNull
    public List<Example> findNameBirthdayWhereNameIs(@NotNull String name) {
        SearchQueryCompiler queryCompiler = provider.getEmptyQuery();
        queryCompiler.setNeededFields("name", "birthday");
        queryCompiler.field("name", false);
        queryCompiler.filter("eq", name);
        Collection<Example> result = provider.find(queryCompiler);
        return new ArrayList<Example>(result);
    }
}
