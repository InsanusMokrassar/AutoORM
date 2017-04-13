package com.github.insanusmokrassar.interfaces;

import com.github.insanusmokrassar.iobject.interfaces.CommonIObject;

public interface IObservableObject<KeyType, ValueType> extends CommonIObject<KeyType, ValueType> {
    void subscribe(KeyType key, Action1<ValueType> subscriber, Boolean notifyNow);
    void unsubscribe(KeyType key, Action1<ValueType> subscriber);

    void clear();
}
