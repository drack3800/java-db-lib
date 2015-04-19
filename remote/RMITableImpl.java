package ru.fizteh.fivt.students.dmitry_persiyanov.remote;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by drack3800 on 20.12.2014.
 */
public class RMITableImpl implements RMITable {
    private final Table table;

    public RMITableImpl(final Table table) throws RemoteException {
        this.table = table;
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException, RemoteException {
        return table.put(key, value);
    }

    @Override
    public Storeable remove(String key) throws RemoteException {
        return table.remove(key);
    }

    @Override
    public int size() throws RemoteException {
        return table.size();
    }

    @Override
    public List<String> list() throws RemoteException {
        return table.list();
    }

    @Override
    public int commit() throws IOException, RemoteException {
        return table.commit();
    }

    @Override
    public int rollback() throws RemoteException {
        return table.rollback();
    }

    @Override
    public int getNumberOfUncommittedChanges() throws RemoteException {
        return table.getNumberOfUncommittedChanges();
    }

    @Override
    public int getColumnsCount() throws RemoteException {
        return table.getColumnsCount();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return table.getColumnType(columnIndex);
    }

    @Override
    public String getName() throws RemoteException {
        return table.getName();
    }

    @Override
    public Storeable get(String key) throws RemoteException {
        return table.get(key);
    }
}
