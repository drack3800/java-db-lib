package ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table.DbTable;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table.TableLoaderDumper;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table.TableRow;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.SyntaxCheckers;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.TypeStringTranslator;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.Utility;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.exceptions.WrongTableNameException;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DbTableProvider implements TableProvider, AutoCloseable {
    // Matches quoted string.
    private static final String STRING_REGEX = "\"([^\"]*)\"";
    // Matches one column in JSON input (JSON: ["John", "Doe", 42], columns: "John", "Doe", 42).
    private static final String ONE_COLUMN_TYPE_REGEX = "\\s*(" + STRING_REGEX
            + "|null|true|false|-?\\d+(\\.\\d+)?)\\s*";
    // Matches JSON input which consists of several columns.
    private static final String JSON_REGEX = "^\\[" + ONE_COLUMN_TYPE_REGEX + "(," + ONE_COLUMN_TYPE_REGEX + ")*\\]$";

    private Path rootDir;
    private DbTable currentTable;
    private ReadWriteLock tablesMapLock = new ReentrantReadWriteLock();
    private Map<String, DbTable> tables = new HashMap<>();

    private boolean closed = false;
    private ReadWriteLock closedLock = new ReentrantReadWriteLock();

    public DbTableProvider(final Path rootDir) {
        if (rootDir == null) {
            throw new NullPointerException();
        } else if (!Files.exists(rootDir)) {
            try {
                Files.createDirectories(rootDir);
            } catch (IOException e) {
                throw new RuntimeException("can't create directory: " + rootDir.toString());
            }
        } else if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException(rootDir.toString() + " isn't a directory");
        }
        this.rootDir = rootDir;
        try {
            loadTables();
        } catch (IOException e) {
            throw new RuntimeException("can't load tables from: " + rootDir.toString()
                    + ", [" + e.getMessage() + "]");
        }
    }

    private boolean containsTable(final String tableName) {
        closedLock.readLock().lock();
        tablesMapLock.readLock().lock();
        try {
            checkClosed();
            return tables.containsKey(tableName);
        } finally {
            tablesMapLock.readLock().unlock();
            closedLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        return new String(getClass().getSimpleName() + "[" + rootDir.toAbsolutePath().toString() + "]");
    }

    @Override
    public Table getTable(final String tableName) {
        closedLock.readLock().lock();
        tablesMapLock.readLock().lock();
        try {
            checkClosed();
            if (!SyntaxCheckers.checkCorrectnessOfTableName(tableName)) {
                throw new WrongTableNameException(tableName);
            } else if (!tables.containsKey(tableName)) {
                return null;
            } else {
                DbTable table = tables.get(tableName);
                if (table == null) {
                    tables.put(tableName, DbTable.loadExistingDbTable(getTablePath(tableName), this));
                }
                return tables.get(tableName);
            }
        } finally {
            tablesMapLock.readLock().unlock();
            closedLock.readLock().unlock();
        }
    }

    @Override
    public DbTable createTable(final String tableName, final List<Class<?>> columnTypes) throws IOException {
        closedLock.readLock().lock();
        try {
            checkClosed();
            if (!SyntaxCheckers.checkCorrectnessOfTableName(tableName)) {
                throw new WrongTableNameException(tableName);
            } else if (columnTypes == null) {
                throw new IllegalArgumentException("wrong column types signature");
            } else {
                tablesMapLock.writeLock().lock();
                try {
                    if (!containsTable(tableName)) {
                        Path tablePath = getTablePath(tableName);
                        Files.createDirectory(tablePath);
                        DbTable table = DbTable.createDbTable(tablePath, columnTypes, this);
                        tables.put(tableName, table);
                        return table;
                    } else {
                        return null;
                    }
                } finally {
                    tablesMapLock.writeLock().unlock();
                }
            }
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public void removeTable(final String tableName) {
        closedLock.readLock().lock();
        tablesMapLock.writeLock().lock();
        try {
            checkClosed();
            if (!SyntaxCheckers.checkCorrectnessOfTableName(tableName)) {
                throw new WrongTableNameException(tableName);
            } else if (!containsTable(tableName)) {
                throw new IllegalStateException("there is no tables with name \"" + tableName + "\"");
            } else {
                if (currentTable != null && currentTable.getName().equals(tableName)) {
                    currentTable = null;
                }
                tables.get(tableName).close();
                tables.remove(tableName);
                try {
                    purgeTable(getTablePath(tableName));
                } catch (IOException e) {
                    throw new RuntimeException("can't remove table: " + e.getMessage());
                }
            }
        } finally {
            tablesMapLock.writeLock().unlock();
            closedLock.readLock().unlock();
        }
    }

    @Override
    public Storeable deserialize(final Table table, final String value) throws ParseException {
        closedLock.readLock().lock();
        try {
            checkClosed();
            String str = value.trim();
            if (!str.matches(JSON_REGEX)) {
                throw new ParseException("value isn't in JSON format", 0);
            } else {
                try {
                    int leftBracket = str.indexOf('[');
                    int rightBracket = str.lastIndexOf(']');
                    List<Object> values = new LinkedList<>();
                    int i = leftBracket + 1;
                    while (i < rightBracket) {
                        char currChar = str.charAt(i);
                        if (currChar == '\"') {
                            // String argument. Finding end quote.
                            int endQuoteIndex = i + 1;
                            while (!(str.charAt(endQuoteIndex) == '\"' && str.charAt(endQuoteIndex - 1) != '\\')) {
                                endQuoteIndex++;
                            }
                            String strColumn = str.substring(i + 1, endQuoteIndex);
                            values.add(strColumn);
                            i = endQuoteIndex + 1;
                        } else if (Character.isSpaceChar(currChar) || currChar == ',') {
                            i++;
                        } else if (Character.isDigit(currChar) || currChar == '-') {
                            int nextComma = str.indexOf(',', i);
                            if (nextComma == -1) {
                                // Last column.
                                nextComma = rightBracket;
                            }
                            String number = str.substring(i, nextComma).trim();
                            Class<?> tableColType = table.getColumnType(values.size());
                            if (number.indexOf('.') != -1) {
                                if (tableColType.equals(Double.class)) {
                                    values.add(new Double(number));
                                } else if (tableColType.equals(Float.class)) {
                                    values.add(new Float(number));
                                }
                            } else {
                                if (tableColType.equals(Integer.class)) {
                                    values.add(new Integer(number));
                                } else if (tableColType.equals(Long.class)) {
                                    values.add(new Long(number));
                                } else if (tableColType.equals(Double.class)) {
                                    values.add(new Double(number));
                                } else if (tableColType.equals(Float.class)) {
                                    values.add(new Float(number));
                                }
                            }
                            i = nextComma + 1;
                        } else {
                            // Boolean or null
                            int nextComma = str.indexOf(',', i);
                            if (nextComma == -1) {
                                nextComma = rightBracket;
                            }
                            String boolOrNullValue = str.substring(i, nextComma).trim();
                            if (boolOrNullValue.equals("true")) {
                                values.add(true);
                            } else if (boolOrNullValue.equals("false")) {
                                values.add(false);
                            } else if (boolOrNullValue.equals("null")) {
                                values.add(null);
                            } else {
                                throw new ParseException("it's not possible, but there is a parse error!", 0);
                            }
                            i = nextComma + 1;
                        }
                    }
                    if (values.size() != table.getColumnsCount()) {
                        throw new ParseException("incompatible sizes of Storeable in the table and json you passed", 0);
                    }
                    return createFor(table, values);
                } catch (IndexOutOfBoundsException e) {
                    throw new ParseException("can't parse your json", 0);
                } catch (NumberFormatException e) {
                    throw new ParseException("types incompatibility", 0);
                }
            }
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public String serialize(final Table table, final Storeable value) throws ColumnFormatException {
        closedLock.readLock().lock();
        try {
            checkClosed();
            if (value == null) {
                return null;
            } else {
                List<String> strColumns = new LinkedList<>();
                for (int i = 0; i < table.getColumnsCount(); ++i) {
                    if (value.getColumnAt(i) == null) {
                        strColumns.add(null);
                    } else {
                        Class<?> tableColumnType = table.getColumnType(i);
                        Class<?> valueColumnType = value.getColumnAt(i).getClass();
                        if (!tableColumnType.equals(valueColumnType)) {
                            throw new ColumnFormatException();
                        } else if (valueColumnType.equals(String.class)) {
                            strColumns.add("\"" + value.getColumnAt(i).toString() + "\"");
                        } else {
                            strColumns.add(value.getColumnAt(i).toString());
                        }
                    }
                }
                StringBuilder b = new StringBuilder(String.join(", ", strColumns));
                b.insert(0, "[");
                b.append("]");
                return b.toString();
            }
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public Storeable createFor(final Table table) {
        closedLock.readLock().lock();
        try {
            checkClosed();
            return new TableRow(Arrays.asList(new Object[table.getColumnsCount()]));
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public Storeable createFor(final Table table, final List<?> values) throws ColumnFormatException,
            IndexOutOfBoundsException {
        closedLock.readLock().lock();
        try {
            checkClosed();
            List<Object> storeableValues = new LinkedList<>();
            for (int i = 0; i < values.size(); ++i) {
                if (values.get(i) != null && !table.getColumnType(i).equals(values.get(i).getClass())) {
                    throw new ColumnFormatException("types incompatibility, needed type: "
                            + TypeStringTranslator.getStringNameByType(table.getColumnType(i)) + ", passed type: "
                            + TypeStringTranslator.getStringNameByType(values.get(i).getClass()));
                } else {
                    storeableValues.add(values.get(i));
                }
            }
            return new TableRow(storeableValues);
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public List<String> getTableNames() {
        closedLock.readLock().lock();
        try {
            checkClosed();
            List<String> res = new LinkedList<>();
            res.addAll(tables.keySet());
            return res;
        } finally {
            closedLock.readLock().unlock();
        }
    }

    @Override
    public void close() throws Exception {
        closedLock.writeLock().lock();
        try {
            checkClosed();
            closed = true;
        } finally {
            closedLock.writeLock().unlock();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException();
        }
    }

    private void purgeTable(final Path tablePath) throws IOException {
        File[] files = new File(tablePath.toString()).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                purgeDir(file);
            } else {
                Files.delete(file.toPath());
            }
        }
        Files.delete(tablePath);
    }

    private void purgeDir(final File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            Files.delete(file.toPath());
        }
        Files.delete(dir.toPath());
    }

    private Path getTablePath(final String tableName) {
        return rootDir.resolve(tableName);
    }

    private void loadTables() throws IOException {
        tables.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDir)) {
            for (Path tableDir : stream) {
                TableLoaderDumper.checkTableForCorruptness(tableDir);
                tables.put(Utility.getNameByPath(tableDir), null);
            }
        }
    }
}
