package com.github.insanusmokrassar.AbstractDatabaseORM;

import com.github.insanusmokrassar.AbstractDatabaseORM.annotations.Key;
import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.IQueryingObject;
import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.databases.IDatabaseAdapter;
import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.databases.ITransaction;
import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.utils.Action1;
import com.github.insanusmokrassar.iobject.exceptions.ReadException;
import com.github.insanusmokrassar.iobject.exceptions.WriteException;
import com.github.insanusmokrassar.iobject.interfaces.CommonIObject;
import com.github.insanusmokrassar.iobject.interfaces.IReadableObject;
import com.github.insanusmokrassar.iobject.realisations.SimpleCommonIObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoFieldedDatabase<KeysType extends Number, ValuesType> implements IQueryingObject<KeysType, ValuesType> {
    protected final Class<? extends KeysType> keysClass;
    protected final Class<? extends ValuesType> valuesClass;
    protected final IDatabaseAdapter adapter;

    protected final Map<KeysType, List<Action1<ValuesType>>> subscribers = new HashMap<>();
    protected final ExecutorService executorService;

    protected final String CREATE_DATABASE_TEMPLATE = "create table %s (%s);";
    protected final String CREATE_DATABASE_PRIMARY_KEY_TEMPLATE = "%s %s primary key %s ";
    protected final String CREATE_DATABASE_FIELDS_TEMPLATE = "%s, %s text";

    protected final List<Field> fields = new ArrayList<>();
    protected final List<String> fieldNames = new ArrayList<>();
    protected Field keyField = null;
    protected Key keyAnnotation = null;
    protected String tableName;

    protected CommonIObject<KeysType> cached = new SimpleCommonIObject<>();

    public AutoFieldedDatabase(IDatabaseAdapter adapter, Integer subscribersThreadsCount, Class<? extends KeysType> keysClass, Class<? extends ValuesType> valuesClass) {
        this.keysClass = keysClass;
        this.valuesClass = valuesClass;
        executorService = Executors.newFixedThreadPool(subscribersThreadsCount);
        this.adapter = adapter;
        refreshFields();
        createTable(db);
    }

    public AutoFieldedDatabase(IDatabaseAdapter adapter, Class<? extends KeysType> keysClass, Class<? extends ValuesType> valuesClass) {
        this(adapter, 1, keysClass, valuesClass);
    }

    @Override
    public void subscribe(KeysType key, Action1<ValuesType> subscriber, Boolean notifyNow) {
        synchronized (subscribers) {
            if (!subscribers.containsKey(key)) {
                subscribers.put(key, new ArrayList<Action1<ValuesType>>());
            }
            List<Action1<ValuesType>> toPut = subscribers.get(key);
            if (!toPut.contains(subscriber)) {
                toPut.add(subscriber);
            }
            if (notifyNow) {
                try {
                    notifyAboutChangesOne(get(key), subscriber);
                } catch (ReadException e) {
                    Log.e(getClass().getSimpleName(), "Can't get key: " + key, e);
                }
            }
        }
    }

    @Override
    public void unsubscribe(KeysType key, Action1<ValuesType> subscriber) {
        synchronized (subscribers) {
            if (subscribers.containsKey(key)) {
                List<Action1<ValuesType>> list = subscribers.get(key);
                if (list.contains(subscriber)) {
                    list.remove(subscriber);
                }
                if (list.isEmpty()) {
                    subscribers.remove(key);
                }
            }
        }
    }

    protected void createTable(SQLiteDatabase db) {
        try {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

            List<String> tablesNames = new ArrayList<>();
            if (c.moveToFirst()) {
                do {
                    tablesNames.add(c.getString(0));
                } while (c.moveToNext());
            }
            if (!tablesNames.contains(tableName)) {
                db.execSQL(constructRequest());
            }
            c.close();
        } catch (NullPointerException e) {
            Log.e(getClass().getSimpleName(), "You must annotate on of your fields as Key", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void clear() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        db.delete(tableName, null, null);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public ValuesType get(KeysType key) throws ReadException {
        Cursor c = null;
        if (cached.keys().contains(key)) {
            return cached.get(key);
        }
        try {
            SQLiteDatabase db = getReadableDatabase();
            c = db.query(tableName, null, keyField.getName() + "=?", new String[]{key.toString()}, null, null, null);
            if (c.moveToFirst()) {
                ValuesType value = valuesClass.newInstance();
                for (Field field : fields) {
                    field.set(value,
                            field.getType().getConstructor(String.class).newInstance(
                                    c.getString(
                                            c.getColumnIndex(
                                                    field.getName()
                                            )
                                    )
                            ));
                }
                try {
                    cached.put(key, value);
                } catch (WriteException e) {
                    Log.e(getClass().getSimpleName(), "Can't cache value now", e);
                }
                return value;
            } else {
                return null;
            }
        } catch (InstantiationException e) {
            throw new ReadException("Can't create model isntance: no empty constructor", e);
        } catch (IllegalAccessException e) {
            throw new ReadException("Can't create model isntance: empty constructor is unreachable", e);
        } catch (InvocationTargetException e) {
            throw new ReadException("Can't construct model fields: field can't be created using string as argument", e);
        } catch (NoSuchMethodException e) {
            throw new ReadException("Can't construct model fields: field class is not contain constructor from string", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public Set<KeysType> keys() {
        Cursor c = getReadableDatabase().query(tableName, new String[]{keyField.getName()}, null, null, null, null, null);

        Set<KeysType> keys = new HashSet<>();

        try {
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(keyField.getName());
                Constructor<? extends KeysType> constructor = keysClass.getConstructor(String.class);
                do {
                    keys.add(
                            constructor.newInstance(c.getString(columnIndex))
                    );
                } while (c.moveToNext());
            }
        } catch (NoSuchMethodException e) {
            Log.e(getClass().getSimpleName(), "Can't create key instance: key class have not constructor with string arg", e);
        } catch (IllegalAccessException e) {
            Log.e(getClass().getSimpleName(), "Can't create key instance : constructor is inaccessible", e);
        } catch (InvocationTargetException e) {
            Log.e(getClass().getSimpleName(), "Can't create key instance", e);
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        c.close();
        return keys;
    }

    @Override
    public void put(KeysType key, ValuesType value) throws WriteException {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        for (Field field : fields) {
            try {
                cv.put(field.getName(), field.get(value).toString());
            } catch (IllegalAccessException e) {
                throw new WriteException("Can't put one of fields to database", e);
            } catch (NullPointerException e) {
                Log.e(getClass().getSimpleName(), "Can't put value for " + field.getName(), e);
            }
        }

        db.beginTransaction();

        Long id = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        db.setTransactionSuccessful();
        db.endTransaction();

        if (key == null) {
            try {
                Constructor constructor = keyField.getType().getConstructor(String.class);
                key = (KeysType) constructor.newInstance(id.toString());
                keyField.set(value, key);
                cached.put(key, value);
            } catch (NoSuchMethodException e) {
                Log.e(getClass().getSimpleName(), "Can't set key", e);
            } catch (IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Can't set key, constructor for strin is inaccessible", e);
            } catch (InstantiationException e) {
                Log.e(getClass().getSimpleName(), "Can't set key", e);
            } catch (InvocationTargetException e) {
                Log.e(getClass().getSimpleName(), "Can't set key", e);
            }
        }
    }

    @Override
    public void putAll(Map<KeysType, ValuesType> map) throws WriteException {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        try {
            for (KeysType key : map.keySet()) {
                ContentValues cv = new ContentValues();
                ValuesType value = map.get(key);
                for (Field field : fields) {
                        cv.put(field.getName(), field.get(value).toString());
                }
                Long id = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

                if (key == null) {
                    try {
                        Constructor constructor = keyField.getType().getConstructor(String.class);
                        key = (KeysType) constructor.newInstance(id.toString());
                        keyField.set(value, key);
                        cached.put(key, value);
                    } catch (NoSuchMethodException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    } catch (IllegalAccessException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key, constructor for strin is inaccessible", e);
                    } catch (InstantiationException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    } catch (InvocationTargetException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (IllegalAccessException e) {
            throw new WriteException("Can't put one of fields to database", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void putAll(IReadableObject<KeysType, ValuesType> iReadableObject) throws WriteException {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        try {
            for (KeysType key : iReadableObject.keys()) {
                ContentValues cv = new ContentValues();
                ValuesType value = iReadableObject.get(key);
                for (Field field : fields) {
                    cv.put(field.getName(), field.get(value).toString());
                }
                Long id = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (key == null) {
                    try {
                        Constructor constructor = keyField.getType().getConstructor(String.class);
                        key = (KeysType) constructor.newInstance(id.toString());
                        keyField.set(value, key);
                        cached.put(key, value);
                    } catch (NoSuchMethodException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    } catch (IllegalAccessException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key, constructor for strin is inaccessible", e);
                    } catch (InstantiationException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    } catch (InvocationTargetException e) {
                        Log.e(getClass().getSimpleName(), "Can't set key", e);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (IllegalAccessException e) {
            throw new WriteException("Can't put one of fields to database", e);
        } catch (ReadException e) {
            throw new WriteException("Can't put one of fields to database, in iReadable field is unexpected", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void remove(KeysType key) throws WriteException {
        SQLiteDatabase db = getWritableDatabase();

        ITransaction transaction = adapter.beginTransaction();

        transaction.remove(adapter.getNewSearchQuery(valuesClass).and());

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    protected void notifyAboutChanges(KeysType key, ValuesType value) {
        if (subscribers.containsKey(key)) {
            for (Action1<ValuesType> current : subscribers.get(key)) {
                executorService.submit(new ActionCaller<>(current, value));
            }
        }
    }

    protected void notifyAboutChangesOne(ValuesType value, Action1<ValuesType> target) {
        executorService.submit(
                new ActionCaller<>(
                        target,
                        value
                )
        );
    }

    protected String constructRequest() {
        String keysString = String.format(
                CREATE_DATABASE_PRIMARY_KEY_TEMPLATE,
                keyField.getName(),
                keyAnnotation.type(),
                keyAnnotation.autoincrement() ? "autoincrement" : ""
        );

        for (Field currentField : fields) {
            if(currentField.equals(keyField)) {
                continue;
            }
            keysString = String.format(
                    CREATE_DATABASE_FIELDS_TEMPLATE,
                    keysString,
                    currentField.getName()
            );
        }
        return String.format(
                CREATE_DATABASE_TEMPLATE,
                tableName,
                keysString
        );
    }

    protected void refreshFields() {
        fields.addAll(FieldsExtractor.getPublicFields(valuesClass));
        for (Field currentField : fields) {
            Annotation annotation = currentField.getAnnotation(Key.class);
            if (annotation != null) {
                keyAnnotation = (Key) annotation;
                keyField = currentField;
                break;
            }
        }
        tableName = valuesClass.getSimpleName();
    }

    @Override
    public List<ValuesType> find(ISearchQuery<ValuesType> searchQuery) {
        List<ValuesType> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(tableName, null, searchQuery.toString(), null, null, null, null);
        if (c.moveToFirst()) {
            try {
                if (fieldNames.isEmpty()) {
                    for (Field field : fields) {
                        fieldNames.add(field.getName());
                    }
                }
                List<Integer> indexes = DatabasesHelper.getKeysIndexes(c, fieldNames);
                Constructor<? extends ValuesType> constructor = valuesClass.getConstructor();
                do {
                    ValuesType value = constructor.newInstance();
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        Integer index = indexes.get(i);
                        field.set(value, field.getType().getConstructor(String.class).newInstance(c.getString(index)));
                    }
                    result.add(value);
                } while (c.moveToNext());
            } catch (NoSuchMethodException e) {
                Log.e(getClass().getSimpleName(), "Object contains not empty constructor", e);
            } catch (InstantiationException e) {
                Log.e(getClass().getSimpleName(), "Can't create object", e);
            } catch (IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Can't get empty object constructor", e);
            } catch (InvocationTargetException e) {
                Log.e(getClass().getSimpleName(), "Can't create object using empty constructor or set value of field", e);
            } catch (Throwable e) {
                Log.e(getClass().getSimpleName(), "Can't construct object", e);
            }
        }
        c.close();
        return result;
    }

    @Override
    public ISearchQuery<ValuesType> createEmptyQueryObject() {
        return new SQLSearchQuery<>(valuesClass);
    }
}
