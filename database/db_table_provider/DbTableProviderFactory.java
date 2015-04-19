package ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.storage.structured.TableProviderFactory;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.SyntaxCheckers;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by drack3800 on 12.11.2014.
 */

public class DbTableProviderFactory implements TableProviderFactory, AutoCloseable {
    private Set<DbTableProvider> createdProviders = new HashSet<>();
    private boolean closed = false;

    @Override
    public TableProvider create(final String dir) {
        checkClosed();
        if (!SyntaxCheckers.checkCorrectnessOfTableName(Paths.get(dir).getFileName().toString())) {
            throw new IllegalArgumentException("wrong tableprovider dir: " + dir);
        } else {
            try {
                DbTableProvider newDb = new DbTableProvider(Paths.get(dir));
                createdProviders.add(newDb);
                return newDb;
            } catch (RuntimeException e) {
                throw new RuntimeException("can't load database: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws Exception {
        checkClosed();
        for (DbTableProvider db : createdProviders) {
            db.close();
        }
        closed = true;
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException();
        }
    }
}
