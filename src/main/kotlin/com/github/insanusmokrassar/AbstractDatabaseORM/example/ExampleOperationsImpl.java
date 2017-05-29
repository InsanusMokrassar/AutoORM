package com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces;import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.Override;
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider;
import java.lang.String;
import java.lang.Integer;
import kotlin.Unit;


public class ExampleOperationsImpl implements ExampleOperations {
    private TableProvider provider;
    private String birthday;

    @NotNull
    public String getBirthday() {
        return birthday;
    }

    private Integer id;

    @Nullable
    public Integer getId() {
        return id;
    }

    private String name;

    @NotNull
    public String getName() {
        return name;
    }

    private int old;

    @NotNull
    public int getOld() {
        return old;
    }
    @NotNull
    public void setOld(int old) {
        this.old = old;
    }
    public ExampleOperationsImpl(TableProvider provider, String birthday, Integer id, String name, int old) {
        this.provider = provider;
        this.birthday = birthday;
        this.id = id;
        this.name = name;
        this.old = old;
    }
    public void insert() {
        provider.insert(this);
    }
    public void remove() {
        provider.remove(provider.getEmptyQuery().field("id", false).filter("eq", id));
    }
    public void update() {
        provider.update(this, provider.getEmptyQuery().field("id", false).filter("eq", id));
    }

}
