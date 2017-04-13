package com.github.insanusmokrassar.AbstractDatabaseORM.interfaces;

import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.IObservableObject;
import com.github.insanusmokrassar.AbstractDatabaseORM.queries.ISearchQuery;

import java.util.List;

public interface IQueryingObject<Key, Value> extends IObservableObject<Key, Value> {
    List<Value> find(ISearchQuery<Value> searchQuery);

    ISearchQuery<Value> createEmptyQueryObject();
}
