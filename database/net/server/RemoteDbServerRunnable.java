package ru.fizteh.fivt.students.dmitry_persiyanov.database.net.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by drack3800 on 14.12.2014.
 */
class RemoteDbServerRunnable implements Runnable {
    private ServerSocket serverSocket;
    private RemoteDbServer server;

    public RemoteDbServerRunnable(ServerSocket serverSocket, RemoteDbServer server) {
        this.serverSocket = serverSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && !server.isStopped()) {
                Socket userSocket = serverSocket.accept();
                RemoteDbConnectionsHandler handler = new RemoteDbConnectionsHandler(userSocket, server);
                server.addUser(RemoteDbServer.getHostBySocket(userSocket), RemoteDbServer.getPortBySocket(userSocket));
                System.out.println("New user: " + RemoteDbServer.getHostBySocket(userSocket));
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            throw new RuntimeException("can't create socket: " + e.getMessage());
        }
    }
}
