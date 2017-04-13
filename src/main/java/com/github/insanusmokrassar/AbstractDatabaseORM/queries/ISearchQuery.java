package com.github.insanusmokrassar.AbstractDatabaseORM.queries;

public interface ISearchQuery<T> {

    ISearchQuery<T> and(T model);
    ISearchQuery<T> or(T model);

    ISearchQuery<T> and(T model, String operator);
    ISearchQuery<T> or(T model, String operator);

    ISearchQuery<T> and(ISearchQuery<T> query);
    ISearchQuery<T> or(ISearchQuery<T> query);
    ISearchQuery<T> not(ISearchQuery<T> query);

    ISearchQuery<T> getNew();
    ISearchQuery<T> getNew(T model);
}
