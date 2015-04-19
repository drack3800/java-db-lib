package ru.fizteh.fivt.students.dmitry_persiyanov.database.logging_proxy_factory;

import ru.fizteh.fivt.proxy.LoggingProxyFactory;

import java.io.Writer;
import java.lang.reflect.Proxy;

/**
 * Created by drack3800 on 30.11.2014.
 */
public class DbLoggingProxyFactory implements LoggingProxyFactory {


    @Override
    public Object wrap(Writer writer, Object implementation, Class<?> interfaceClass) {
        checkArguments(writer, implementation, interfaceClass);
        return Proxy.newProxyInstance(implementation.getClass().getClassLoader(),
                new Class<?>[]{interfaceClass}, new DbLoggingInvocationHandler(writer, implementation));
    }

    private void checkArguments(Writer writer, Object implementation, Class<?> interfaceClass) {
        if (writer == null || implementation == null || interfaceClass == null || !interfaceClass.isInterface()) {
            throw new IllegalArgumentException();
        }
    }
}
