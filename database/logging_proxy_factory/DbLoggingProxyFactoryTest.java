package ru.fizteh.fivt.students.dmitry_persiyanov.database.logging_proxy_factory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.DbTableProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DbLoggingProxyFactoryTest {
    private static final String TIMESTAMP_REGEX = "timestamp=\"\\d*\"";
    private static final String PATH_REGEX = "\\[.*\\]";

    TableProvider db;
    StringWriter proxyWriter = new StringWriter();
    List<Class<?>> tableSignature;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        tableSignature = Arrays.asList(String.class, Boolean.class);
        db = (TableProvider) new DbLoggingProxyFactory().wrap(proxyWriter,
                new DbTableProvider(tmpFolder.newFolder().toPath()),
                TableProvider.class);
    }

    @Test
    public void testLoggingCorrectnessWithoutThrownException() throws IOException {
        db.createTable("table1", tableSignature);
        compareLogs("<invoke timestamp=\"\" class=\"ru.fizteh.fivt.students.dmitry_persiyanov."
                + "database.db_table_provider.DbTableProvider\" name=\"createTable\">"
                + "<arguments>"
                + "<argument>table1</argument>"
                + "<argument>"
                + "<list>"
                + "<value>class java.lang.String</value>"
                + "<value>class java.lang.Boolean</value>"
                + "</list>"
                + "</argument>"
                + "</arguments>"
                + "<return>DbTable[]</return>"
                + "</invoke>", proxyWriter.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testLoggingCorrectnessWithThrownException() throws IOException {
        try {
            db.removeTable("non-existent");
        } finally {
            compareLogs("<invoke timestamp=\"\" class=\"ru.fizteh.fivt.students.dmitry_persiyanov."
                    + "database.db_table_provider.DbTableProvider\" name=\"removeTable\">"
                    + "<arguments>"
                    + "<argument>non-existent</argument>"
                    + "</arguments>"
                    + "<thrown>java.lang.IllegalStateException: there "
                    + "is no tables with name \"non-existent\"</thrown>"
                    + "</invoke>", proxyWriter.toString());
        }
    }

    private void compareLogs(final String log1, final String log2) {
        String log1Edited = log1.replaceAll(TIMESTAMP_REGEX, "timestamp=\"\"").replaceAll(PATH_REGEX, "[]");
        String log2Edited = log2.replaceAll(TIMESTAMP_REGEX, "timestamp=\"\"").replaceAll(PATH_REGEX, "[]");
        assertEquals(log1Edited, log2Edited);
    }
}
