package com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.databases;

import com.github.insanusmokrassar.AbstractDatabaseORM.queries.ISearchQuery;
import com.github.insanusmokrassar.iobject.interfaces.IObject;

import java.util.List;

public interface ITransaction {
    int insertOrReplace(IObject insertPairs);
    int insertOrReplace(List<IObject> insertPairs);

    void cancelTransaction();
    void completeTransaction();

    int remove(ISearchQuery query);
}
