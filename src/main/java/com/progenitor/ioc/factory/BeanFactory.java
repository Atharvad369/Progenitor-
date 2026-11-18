package com.progenitor.ioc.factory;

public interface BeanFactory {


    public <T> T getPojoObject(Class<T> type);
}
