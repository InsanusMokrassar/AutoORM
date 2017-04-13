package com.github.insanusmokrassar.AbstractDatabaseORM.queries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonSearchQuery<T> implements ISearchQuery<T> {
    protected final Class<? extends T> targetClass;
    protected final List<Field> fields = new ArrayList<>();
    protected String query = "";

    protected String standardTemplate = " %s %s%s\'%s\'";
    protected String searchQueryTemplate = " %s (%s)";

    public CommonSearchQuery(Class<? extends T> targetClass) {
        this.targetClass = targetClass;
        fields.addAll(Arrays.asList(targetClass.getFields()));
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
        query = constructBinarCondition(fields, query, model, getAndKey(), operator, standardTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> or(T model, String operator) {
        query = constructBinarCondition(fields, query, model, getOrKey(), operator, standardTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> and(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, getAndKey(), searchQueryTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> or(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, getOrKey(), searchQueryTemplate);
        return this;
    }

    @Override
    public ISearchQuery<T> not(ISearchQuery<T> query) {
        this.query = constructCompleteCondition(this.query, query, getNotKey(), searchQueryTemplate);
        return this;
    }

    public String getAndKey() {
        return "AND";
    }

    public String getOrKey() {
        return "OR";
    }

    public String getNotKey() {
        return "NOT";
    }

    @Override
    public ISearchQuery<T> getNew() {
        return new CommonSearchQuery<>(targetClass);
    }

    @Override
    public ISearchQuery<T> getNew(T model) {
        return new CommonSearchQuery<>(targetClass).and(model);
    }

    @Override
    public String toString() {
        return query;
    }

    protected static <T> String constructBinarCondition(List<Field> fields, String query, T model, String separator, String operator, String template) {
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
                System.err.println("E\\" + CommonSearchQuery.class.getSimpleName() + ": Can't get access to the field");
                e.printStackTrace();
            }
        }
        query += result;
        return query;
    }

    protected static String constructCompleteCondition(String query, ISearchQuery searchQuery, String separator, String template) {
        query += String.format(
                template,
                (query.isEmpty() ? "" : separator),
                searchQuery.toString()
        );
        return query;
    }

}
