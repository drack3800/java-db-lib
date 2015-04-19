package ru.fizteh.fivt.students.dmitry_persiyanov;

import ru.fizteh.fivt.students.dmitry_persiyanov.database.net.server.RemoteDbServer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by drack3800 on 14.12.2014.
 */
public class TelnetServerMain {
    private static final int DEFAULT_PORT = 10001;
    private static Path rootDir;
    private static int port;
    public static void main(final String[] args) {
        String dbdir = System.getProperty("fizteh.db.dir");
        if (dbdir == null) {
            System.out.println("You must specify a variable \"fizteh.db.dir\" via JVM parameter.");
            System.exit(1);
        } else {
            rootDir = Paths.get(dbdir);
            if (args.length != 1) {
                port = DEFAULT_PORT;
            } else {
                port = Integer.parseInt(args[0]);
            }
        }
        try {
            RemoteDbServer server = new RemoteDbServer(port, rootDir);
            server.start();
            System.out.println("Started server at localhost:" + server.getPort());
            server.join();
        } catch (Exception e) {
            System.err.println("Cannot open database: " + e.getMessage());
        }
    }
}
