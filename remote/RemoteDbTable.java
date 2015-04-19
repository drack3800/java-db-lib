package ru.fizteh.fivt.students.dmitry_persiyanov.remote;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by drack3800 on 15.12.2014.
 */
public class RemoteDbTable implements RMITable {
    @Override
    public String getName() throws RemoteException {
        return null;
    }

    @Override
    public Storeable get(String key) throws RemoteException {
        return null;
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException, RemoteException {
        return null;
    }

    @Override
    public Storeable remove(String key) throws RemoteException {
        return null;
    }

    @Override
    public int size() throws RemoteException {
        return 0;
    }

    @Override
    public List<String> list() throws RemoteException {
        return null;
    }

    @Override
    public int commit() throws IOException, RemoteException {
        return 0;
    }

    @Override
    public int rollback() throws RemoteException {
        return 0;
    }

    @Override
    public int getNumberOfUncommittedChanges() throws RemoteException {
        return 0;
    }

    @Override
    public int getColumnsCount() throws RemoteException {
        return 0;
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException, RemoteException {
        return null;
    }
}
