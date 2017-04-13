package com.github.insanusmokrassar.SearchQueryRealisations;

import com.github.insanusmokrassar.androidutils.commonutils.FieldsExtractor;
import com.github.insanusmokrassar.androidutils.commonutils.AndroidSpecific.Log;
import com.github.insanusmokrassar.androidutils.databaseutils.sql.free.Operators;
import com.github.insanusmokrassar.androidutils.databaseutils.sql.free.interfaces.ISearchQuery;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SQLSearchQuery<T> implements ISearchQuery<T>{
    protected final Class<? extends T> targetClass;
    protected final List<Field> fields = new ArrayList<>();
    protected String query = "";

    protected static final String standardTemplate = " %s %s%s\'%s\'";
    protected static final String searchQueryTemplate = " %s (%s)";

    public SQLSearchQuery(Class<? extends T> targetClass) {
        this.targetClass = targetClass;
        fields.addAll(FieldsExtractor.getPublicFields(targetClass));
    }

    @Override
    public ISearchQuery<T> and(T model) {
        return and(model, Operators.EQ_OPERATOR);
    }

    @Override
    public ISearchQuery<T> or(T model) {
        return or(model, Operators.EQ_OPERATOR);
    }

    @Override
    public ISearchQuery<T> and(T model, String operator) {
        query = constructBinarCondition(query, model, "AND", operator, standardTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> or(T model, String operator) {
        query = constructBinarCondition(query, model, "OR", operator, standardTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> and(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, "AND", searchQueryTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> or(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, "OR", searchQueryTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> not(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, "NOT", searchQueryTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> getNew() {
        return new SQLSearchQuery<>(targetClass);
    }

    @Override
    public String toString() {
        return query;
    }

    protected String constructBinarCondition(String query, T model, String separator, String operator, String template) {
        String result = "";
        for (Field field : fields) {
            try {
                Object object = field.get(model);
                if (object != null) {
                    result += String.format(
                            template,
                            query.isEmpty() ? "" : separator,
                            field.getName(),
                            operator,
                            object.toString()
                    );
                }
            } catch (IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Can't get access to the field", e);
            }
        }
        query += result;
        return query;
    }

    protected String constructCompleteCondition(String query, ISearchQuery<T> searchQuery, String separator, String template) {
        query += String.format(
                template,
                (this.query.isEmpty() ? "" : separator),
                searchQuery.toString()
        );
        return query;
    }
}
