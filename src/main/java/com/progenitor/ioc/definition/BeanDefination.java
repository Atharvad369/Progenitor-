package com.progenitor.ioc.definition;

import com.progenitor.ioc.annotation.Autowired;
import com.progenitor.ioc.annotation.Postconstruct;
import com.progenitor.ioc.annotation.Predestroy;
import com.progenitor.ioc.annotation.Scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;

public class BeanDefination {
    private final Class<?> beanClass ;
    private final Constructor<?> classConstructors;
    private final Class<?>[] requiredDependencies;
    private final String scope;
    private final Method postConstruct;
    private final Method preDestroy;
    private static final String SCOPE_SINGLETON="singleton";
    private static final String SCOPE_PROTOTYPE="prototype";
    public BeanDefination(Class<?> providedClazz){
        this.beanClass=providedClazz;
        this.classConstructors=resolveConstructor(providedClazz);
        this.classConstructors.setAccessible(true);
        this.requiredDependencies=classConstructors.getParameterTypes();
        this.scope=resolveScope(providedClazz);
        this.postConstruct=resolveLifeCycleMethod( beanClass, Postconstruct.class);
        this.preDestroy=resolveLifeCycleMethod(beanClass, Predestroy.class);
    }
    private Constructor<?> resolveConstructor(Class<?> clazz)
    {
        Constructor<?>[] allConstructor = clazz.getDeclaredConstructors();
        if(allConstructor.length==1){
           return allConstructor[0];
        }
        return Arrays.stream(allConstructor)
                .filter(c->c.isAnnotationPresent(Autowired.class))
                .findFirst()
                .orElseThrow(()-> new IllegalStateException("ambigous Constructor "+clazz.getName()));
    }
    private String resolveScope(Class<?> beanClass){
        if(beanClass.isAnnotationPresent(Scope.class)){
            Scope annotation = beanClass.getAnnotation(Scope.class);
            String value=annotation.value().trim().toLowerCase();
            if(value.equals(SCOPE_SINGLETON)||value.equals(SCOPE_PROTOTYPE)){
                return value;
            }else{
                throw new IllegalStateException("Invalid @Scope value '" + value + "' on class " + beanClass.getName() + ". Only 'singleton' or 'prototype' are allowed.");
            }
        }
        return SCOPE_PROTOTYPE;
    }
    private Method resolveLifeCycleMethod(Class<?> beanClass, Class<? extends Annotation> annotatedMethod)
    {
            Method [] methods= beanClass.getDeclaredMethods();
            Method foundMethod = null;
            for(Method method :methods)
            {
                if(method.isAnnotationPresent(annotatedMethod))
                {

                    if(Modifier.isStatic(method.getModifiers()))
                    {
                        throw new IllegalStateException(" ADDED THE @POSTCONSTRUCT OR @PREDESTORY ON STATIC METHOD"+method.getName());
                    }
                  if(method.getParameterCount()==0)
                  {
                    if(foundMethod!=null)
                    {
                        throw new IllegalStateException("");
                    }else
                    {
                        method.setAccessible(true);
                        foundMethod=method;
                    }
                  }
                  else
                  {
                      throw new IllegalStateException("/"+method.getName());
                  }
                }
            }
            return foundMethod;
    }

    public Class<?> getBeanClass(){
        return beanClass;
    }
    public Constructor<?> getClassConstructors(){
        return classConstructors;
    }
    public Class<?>[] getRequiredDependencies(){
        return requiredDependencies;
    }
    public Method getPreDestroy(){
        return preDestroy;
    }
    public Method getPostConstruct(){
        return postConstruct;
    }
    public String getScope(){
        return scope;
    }
}
