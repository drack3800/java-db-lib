package ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.exceptions.WrongTableNameException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class DbTableProviderTest {
    private DbTableProvider dbm;
    private File dbDir;
    private File tempFile;

    private List<Class<?>> signature;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        signature = new LinkedList<>();
        signature.add(String.class);
        dbDir = tempFolder.newFolder();
        dbm = new DbTableProvider(dbDir.toPath());
        tempFile = tempFolder.newFile();
    }

    @Test(expected = NullPointerException.class)
    public void createDbManagerWithNullArgument() {
        new DbTableProvider(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createDbManagerWithNonDirectoryArgument() {
        new DbTableProvider(tempFile.toPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingTableWithWrongName() throws IOException {
        dbm.createTable(null, signature);
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingTableWithWrongSignature() throws IOException {
        List<Class<?>> newSignature = new LinkedList<>();
        newSignature.add(String.class);
        newSignature.add(int[].class);  // unsupported type
        dbm.createTable("tableyeah", newSignature);
    }

    @Test
    public void testCorrectTableCreateForValidName() throws IOException {
        Table createdTable = dbm.createTable("table1", signature);
        assertFalse(createdTable == null);
    }

    @Test
    public void checkingEqualityOfReturningTablesByCreateAndGet() throws IOException {
        Table createdTable = dbm.createTable("table1", signature);
        Table gettedTable = dbm.getTable("table1");
        assertEquals(createdTable.getName(), gettedTable.getName());
        assertEquals(createdTable, gettedTable);
    }

    @Test
    public void creatingRemovingAndGettingItMustReturnNull() throws IOException {
        Table createdTable = dbm.createTable("table1", signature);
        dbm.removeTable("table1");
        assertTrue(dbm.getTable("table1") == null);
    }

    @Test
    public void createMustReturnNullWhenTheTableHasAlreadyExists() throws IOException {
        dbm.createTable("table1", signature);
        assertTrue(dbm.createTable("table1", signature) == null);
    }

    @Test(expected = IllegalStateException.class)
    public void removeUnexistedTableMustReturnException() {
        dbm.removeTable("wasssssup");
    }

    @Test
    public void testingRightTableDeleting() throws IOException, ParseException {
        Table t = dbm.createTable("table1", signature);
        t.put("key", dbm.deserialize(t, "[\"value\"]"));
        t.commit();
        dbm.removeTable("table1");
        TableProvider tableProvider = new DbTableProvider(dbDir.toPath());
        assertTrue(tableProvider.getTable("table1") == null);
    }

    @Test(expected = WrongTableNameException.class)
    public void createTableWithNullName() throws IOException {
        dbm.createTable(null, signature);
    }

    @Test(expected = WrongTableNameException.class)
    public void createTableWithInvalidName() throws IOException {
        dbm.createTable(".d23d2d3;.1'.", signature);
    }

    // Serialize tests.
    @Test
    public void correctSerialize() throws IOException {
        List<Class<?>> tableSignature = new LinkedList<>();
        tableSignature.add(String.class);   // Name
        tableSignature.add(String.class);   // Surname
        tableSignature.add(Integer.class);  // Age
        Table t = dbm.createTable("names", tableSignature);
        List<Object> values = new LinkedList<>();
        values.add("Dmitry");
        values.add("Persiyanov");
        values.add(19);
        Storeable value = dbm.createFor(t, values);
        assertEquals("[\"Dmitry\", \"Persiyanov\", 19]", dbm.serialize(t, value));
    }

    @Test
    public void correctSerializeWithNull() throws IOException {
        List<Class<?>> tableSignature = new LinkedList<>();
        tableSignature.add(String.class);   // Name
        tableSignature.add(String.class);   // Surname
        tableSignature.add(Integer.class);  // Age
        Table t = dbm.createTable("names", tableSignature);
        List<Object> values = new LinkedList<>();
        values.add("Dmitry");
        values.add(null);
        values.add(19);
        Storeable value = dbm.createFor(t, values);
        assertEquals("[\"Dmitry\", null, 19]", dbm.serialize(t, value));
    }

    // Deserialize
    @Test
    public void correctDeserialize() throws IOException, ParseException {
        List<Class<?>> tableSignature = new LinkedList<>();
        tableSignature.add(String.class);   // Name
        tableSignature.add(String.class);   // Surname
        tableSignature.add(Integer.class);  // Age
        Table t = dbm.createTable("names", tableSignature);
        List<Object> values = new LinkedList<>();
        values.add("Dmitry");
        values.add("Persiyanov");
        values.add(19);
        Storeable expected = dbm.createFor(t, values);
        Storeable actual = dbm.deserialize(t, "[\"Dmitry\", \"Persiyanov\", 19]");
        assertEquals(expected.getColumnAt(0), actual.getColumnAt(0));
        assertEquals(expected.getColumnAt(2), actual.getColumnAt(2));
        assertEquals(expected.getColumnAt(2), actual.getColumnAt(2));
    }

    @After
    public void tearDown() throws Exception {
        dbm.close();
    }
}
