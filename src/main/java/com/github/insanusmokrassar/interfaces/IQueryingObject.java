package com.github.insanusmokrassar.interfaces;

import java.util.List;

public interface IQueryingObject<Key, Value> extends IObservableObject<Key, Value> {
    List<Value> find(ISearchQuery<Value> searchQuery);

    ISearchQuery<Value> createEmptyQueryObject();
}
