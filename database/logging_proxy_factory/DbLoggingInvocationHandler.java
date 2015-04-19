package ru.fizteh.fivt.students.dmitry_persiyanov.database.logging_proxy_factory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by drack3800 on 02.12.2014.
 */
class DbLoggingInvocationHandler implements InvocationHandler {
    private Writer writer;
    private Object object;
    private String objectClassStr;

    public DbLoggingInvocationHandler(Writer writer, Object object) {
        this.writer = writer;
        this.object = object;
        this.objectClassStr = this.object.getClass().getName();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!isLoggingMethod(method)) {
            return method.invoke(args);
        }
        XMLOutputFactory output = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = output.createXMLStreamWriter(writer);
        try {
            // <invoke ...>
            xmlWriter.writeStartElement("invoke");
            xmlWriter.writeAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
            xmlWriter.writeAttribute("class", objectClassStr);
            xmlWriter.writeAttribute("name", method.getName());

            // <arguments> or <arguments/>
            if (args == null || args.length == 0) {
                xmlWriter.writeEmptyElement("arguments");
            } else {
                xmlWriter.writeStartElement("arguments");
                for (Object arg : args) {
                    xmlWriter.writeStartElement("argument");
                    if (arg == null) {
                        xmlWriter.writeEmptyElement("null");
                        xmlWriter.writeEndElement();
                    } else if (arg instanceof Iterable) {
                        writeIterable(xmlWriter, (Iterable) arg);
                    } else {
                        xmlWriter.writeCharacters(arg.toString());
                    }
                    xmlWriter.writeEndElement();
                }
            }
            xmlWriter.writeEndElement();

            // method invocation
            Object methodInvocationResult = null;
            try {
                methodInvocationResult = method.invoke(object, args);
                xmlWriter.writeStartElement("return");
                xmlWriter.writeCharacters(methodInvocationResult.toString());
                xmlWriter.writeEndElement();

                // </invoke>
                xmlWriter.writeEndElement();
                return methodInvocationResult;
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                xmlWriter.writeStartElement("thrown");
                xmlWriter.writeCharacters(target.getClass().getName() + ": " + target.getMessage());
                xmlWriter.writeEndElement();
                // </invoke>
                xmlWriter.writeEndElement();
                throw target;
            }
        } catch (XMLStreamException e) {
            writer.write("can't log method invocation: " + e.getMessage());
            return null;
        } finally {
            xmlWriter.flush();
            xmlWriter.close();
        }
    }

    private boolean isLoggingMethod(Method method) {
        try {
            Object.class.getMethod(method.getName());
            return false;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }

    private void writeIterable(XMLStreamWriter xmlWriter, Iterable iterable) throws Throwable {
        xmlWriter.writeStartElement("list");
        for (Object inner : iterable) {
            xmlWriter.writeStartElement("value");
            if (inner == null) {
                xmlWriter.writeEmptyElement("null");
                xmlWriter.writeEndElement();
            } else if (inner instanceof Iterable) {
              writeIterable(xmlWriter, (Iterable) inner);
            } else {
                xmlWriter.writeCharacters(inner.toString());
            }
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndElement();
    }
}
