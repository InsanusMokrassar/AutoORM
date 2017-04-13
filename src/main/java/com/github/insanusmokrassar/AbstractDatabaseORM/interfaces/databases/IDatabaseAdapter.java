package com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.databases;

import com.github.insanusmokrassar.AbstractDatabaseORM.annotations.Key;
import com.github.insanusmokrassar.AbstractDatabaseORM.queries.ISearchQuery;
import com.github.insanusmokrassar.iobject.interfaces.IObject;

import java.util.List;

public interface IDatabaseAdapter {
    ITransaction beginTransaction();

    Iterable<IObject> find(ISearchQuery query);

    Boolean updateOrCreateDatabase(Key keyInfo, String primaryKeyName, List<String> otherKeys);

    <T> ISearchQuery<T> getNewSearchQuery(Class<? extends T> queryObjectClass);
}
