package ru.fizteh.fivt.students.dmitry_persiyanov.database.net.server;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands_parser.DbCommandsParser;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.Interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by drack3800 on 09.12.2014.
 */
class RemoteDbConnectionsHandler extends Thread {
    private static final String GREETINGS_MESSAGE = "Welcome to remote database manager, %USERNAME%!";
    private Socket socket;
    private RemoteDbServer server;
    private TableProvider db;

    public RemoteDbConnectionsHandler(Socket socket, RemoteDbServer server) {
        this.socket = socket;
        this.server = server;
        this.db = server.getTableProvider();
    }

    @Override
    public void run() {
        try (
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
        ) {
            Interpreter interpreter = new Interpreter(new DbCommandsParser(db));
            interpreter.setGreetings(GREETINGS_MESSAGE);
            PrintStream shellOutAndErrStream = new PrintStream(socket.getOutputStream());
            interpreter.run(socket.getInputStream(), shellOutAndErrStream, shellOutAndErrStream);
            server.notifyOfClosedConnection(socket);
            System.out.println("User leaved: " + RemoteDbServer.getHostBySocket(socket));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
