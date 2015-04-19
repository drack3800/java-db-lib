package ru.fizteh.fivt.students.dmitry_persiyanov.database.net.server;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.DbTableProviderFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by drack3800 on 08.12.2014.
 */
public class RemoteDbServer {
    static final Path DEFAULT_DB_DIR = Paths.get(System.getProperty("user.dir"), "db");
    private final Path dbDir;
    private TableProvider db;
    // Map<host, port>.
    ConcurrentMap<String, Integer> connectedUsers = new ConcurrentHashMap<>();
    boolean stopped;
    Integer port;
    ServerSocket serverSocket;
    Thread serverThread;

    public RemoteDbServer(int port) throws IOException {
        this(port, DEFAULT_DB_DIR);
    }

    public RemoteDbServer(int port, final Path dbDir) throws IOException {
        this.dbDir = dbDir;
        this.port = port;
        this.db = new DbTableProviderFactory().create(dbDir.toString());
    }

    Map<String, Integer> listUsers() {
        return Collections.unmodifiableMap(connectedUsers);
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isStarted() {
        return serverSocket != null;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 100, InetAddress.getByAddress(new byte[]{0, 0, 0, 0}));
        serverThread = new Thread(new RemoteDbServerRunnable(serverSocket, this));
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void join() throws InterruptedException {
        serverThread.join();
        stopped = true;
    }

    public void stop() {
        stopped = true;
    }

    public void addUser(final String host, int port) {
        connectedUsers.put(host, port);
    }

    public void notifyOfClosedConnection(final Socket socket) {
        connectedUsers.remove(getHostBySocket(socket), getPortBySocket(socket));
    }

    public TableProvider getTableProvider() {
        return db;
    }

    public static String getHostBySocket(final Socket socket) {
        return socket.getInetAddress().getHostName();
    }

    public static Integer getPortBySocket(final Socket socket) {
        return socket.getPort();
    }
}
