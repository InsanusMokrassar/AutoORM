package com.github.insanusmokrassar.AbstractDatabaseORM;

import com.github.insanusmokrassar.AbstractDatabaseORM.interfaces.utils.Action1;

public class ActionCaller<T> implements Runnable {
    protected final Action1<T> target;
    protected final T value;

    public ActionCaller(Action1<T> target, T value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public void run() {
        if (target != null) {
            target.call(value);
        }
    }
}
