package ru.fizteh.fivt.students.dmitry_persiyanov.remote;

import ru.fizteh.fivt.storage.structured.RemoteTableProvider;
import ru.fizteh.fivt.storage.structured.RemoteTableProviderFactory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by drack3800 on 15.12.2014.
 */
public class RemoteDbTableProviderFactory implements RemoteTableProviderFactory {
    @Override
    public RemoteTableProvider connect(String hostname, int port) throws IOException {
        Registry registry = LocateRegistry.getRegistry(hostname, port);
        try {
            return (RemoteTableProvider) registry.lookup(RemoteDbTableProvider.NAME_IN_REGISTRY);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
    }
}
