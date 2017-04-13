package com.github.insanusmokrassar.AbstractDatabaseORM.interfaces;

import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.utils.Action1;
import com.github.insanusmokrassar.iobject.interfaces.CommonIObject;

public interface IObservableObject<KeyType, ValueType> extends CommonIObject<KeyType> {
    Subscription subscribe(KeyType key, Action1<ValueType> subscriber, Boolean notifyNow);

    void clear();
}
