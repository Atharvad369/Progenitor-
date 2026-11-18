package com.progenitor.ioc.definition;

import com.progenitor.ioc.annotation.Component;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassPathScanner {
    private final ClassLoader classLoader =  Thread.currentThread().getContextClassLoader();
    public Set<BeanDefination> scanAndGetBean(String basepPackage)throws Exception{

        Set<BeanDefination> definations = new HashSet<>();
        String path= basepPackage.replace('.','/');
        Enumeration<URL> resources = classLoader.getResources(path);
        while(resources.hasMoreElements()){
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            if("file".equals(protocol)){
                    scanDirectory(new File(resource.toURI()),basepPackage,definations);
            }else if("jar".equals(protocol)){
                    scanJar(resource,basepPackage,definations);
            }
        }

        return definations;
    }
    private void scanDirectory(File file ,String basePackage,Set<BeanDefination> beanDefinations)throws Exception{
            File[] files= file.listFiles();
            if(files==null){
                return;
            }
            for (File f :files){
                if(f.isDirectory()){
                    scanDirectory(f,basePackage+"."+f.getName(),beanDefinations);
                } else if (f.getName().endsWith(".class")) {
                    String fgcn= basePackage+"."+f.getName().replace(".class","");
                    processClass(fgcn,beanDefinations);
                }
            }
    }
    private void scanJar(URL resource,String basePackage,Set<BeanDefination> beanDefinations)throws Exception{
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try(JarFile jarFile=connection.getJarFile()){
            Enumeration<JarEntry>entries = jarFile.entries();
            while(entries.hasMoreElements()){
                String name = entries.nextElement().getName();
                if(name.startsWith(basePackage)&&name.endsWith(".class")){
                    String fgcn=name.replace('/','.').replace(".class","");
                    processClass(fgcn,beanDefinations);
                }
            }
        }
    }
    private void processClass(String name , Set<BeanDefination> beanDefinations)throws ClassNotFoundException{
        Class<?> loadedClass= Class.forName(name ,false, classLoader);
        if(!loadedClass.isAnnotation()&&!loadedClass.isInterface()&&loadedClass.isAnnotationPresent(Component.class)){
            beanDefinations.add(new BeanDefination(loadedClass));
        }
    }
}
