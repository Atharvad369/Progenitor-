package com.progenitor.ioc.factory;

import com.progenitor.ioc.definition.BeanDefination;
import com.progenitor.ioc.definition.ClassPathScanner;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IoCContainer implements BeanFactory {
    private final Map<Class<?>, BeanDefination> beanRegistry=new HashMap<>();
    private final Map<Class<?>,Object> singletoneRegistry=new ConcurrentHashMap<>();
    private final Set<Class<?>> creationStack = new LinkedHashSet<>();
    public IoCContainer(String basePackage)throws Exception{
        ClassPathScanner scanner = new ClassPathScanner();
        Set<BeanDefination> definations= scanner.scanAndGetBean(basePackage);

        for (BeanDefination def :definations){
            beanRegistry.put(def.getBeanClass(),def);
            for(Class<?> iface:def.getBeanClass().getInterfaces()){
                beanRegistry.put(iface,def);
            }
        }
        for (BeanDefination def : definations) {
            resolveAndCreate(def.getBeanClass());
        }
    }

    private Object resolveAndCreate(Class<?> type)throws Exception{
        BeanDefination def = beanRegistry.get(type);
        if(def == null){
            throw new IllegalStateException("there is no such Bean / POJO/Class");
        }
        Class<?> concreateClass = def.getBeanClass();
        if(singletoneRegistry.containsKey(concreateClass)){
            return singletoneRegistry.get(concreateClass);
        }
        if(!creationStack.add(concreateClass)){
            String cyclePath= String.join("->",creationStack.stream().map(Class::getSimpleName).toList())
                    +"->"+concreateClass.getSimpleName();
            throw new IllegalStateException("Circular Dependency problem"+cyclePath);
        }
        try {

            Class<?>[] defType = def.getRequiredDependencies();
            Object [] resolvedDependencies= new Object[defType.length];
            for(int i=0 ;i<defType.length;i++){
                resolvedDependencies[i]=resolveAndCreate(defType[i]);
            }
            Object instance = def.getClassConstructors().newInstance(resolvedDependencies);
            Method method=def.getPostConstruct();
            if(method!=null){
                method.invoke(instance);
            }
            singletoneRegistry.put(concreateClass,instance);
            return instance;
        }finally {
            creationStack.remove(concreateClass);
        }
    }
    @Override
    public <T> T getPojoObject(Class<T> type ){
        BeanDefination def =beanRegistry.get(type);
        if(def == null){
            throw new NoSuchElementException("there is no class of type this");
        }
        return type.cast(singletoneRegistry.get(def.getBeanClass()));
    }


}
