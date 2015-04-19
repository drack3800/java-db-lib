package ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.TypeStringTranslator;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.Utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class DbTable implements Table, AutoCloseable {
    class Diff {
        // Important invariant: uncommittedChangesMap doesn't intersect uncommittedDeletionsSet!
        public Map<String, String> changesMap = new HashMap<>();
        public Set<String> deletionsSet = new HashSet<>();
    }

    private final Path tableDir;
    private final List<Class<?>> columnTypes;
    private final TableProvider tableProvider;

    private static final int MAX_DIRS_FOR_TABLE = 16;
    private static final int MAX_FILES_FOR_DIR = 16;

    private ThreadLocal<Integer> size = ThreadLocal.withInitial(() -> 0);
    private ThreadLocal<Diff> diff = ThreadLocal.withInitial(Diff::new);

    private ReadWriteLock tableMapLock = new ReentrantReadWriteLock(true);
    private List<List<Map<String, String>>> lastCommitTableMap;
    private Integer lastCommitTableMapSize;

    private ReadWriteLock wholeTableLock = new ReentrantReadWriteLock(true);
    private boolean closed = false;


    /**
     * Creates empty table. tableDir must be empty directory.
     * @param tableDir
     * @param columnTypes
     * @param tableProvider
     * @return Empty table.
     */
    public static DbTable createDbTable(final Path tableDir,
                                        final List<Class<?>> columnTypes,
                                        final TableProvider tableProvider) {
        return new DbTable(tableDir, columnTypes, tableProvider);
    }

    public static DbTable loadExistingDbTable(final Path tableDir, final TableProvider tableProvider) {
        return new DbTable(tableDir, tableProvider);
    }

    // This ctor CREATES non-existent table.
    private DbTable(final Path tableDir, final List<Class<?>> columnTypes, final TableProvider tableProvider) {
        if (!Files.isDirectory(tableDir)) {
            throw new IllegalArgumentException("is not a directory: " + tableDir.toString());
        } else {
            this.tableProvider = tableProvider;
            this.columnTypes = new ArrayList<>();
            this.columnTypes.addAll(columnTypes);
            this.tableDir = tableDir;
            initHashMaps();
            try {
                TableLoaderDumper.createTable(this.tableDir, columnTypes);
            } catch (IOException e) {
                throw new RuntimeException("can't create table from \'" + tableDir.toString() + "\'"
                        + ", [" + e.getMessage() + "]");
            }
            setSize(0);
            lastCommitTableMapSize = getSize();
        }
    }

    // This ctor LOADS existent table.
    private DbTable(final Path tableDir, final TableProvider tableProvider) {
        if (!Files.isDirectory(tableDir)) {
            throw new IllegalArgumentException("is not a directory: " + tableDir.toString());
        } else {
            this.tableProvider = tableProvider;
            this.columnTypes = new ArrayList<>();
            this.tableDir = tableDir;
            initHashMaps();
            try {
                TableLoaderDumper.loadTable(this.tableDir, lastCommitTableMap, columnTypes);
            } catch (IOException e) {
                throw new RuntimeException("can't load table from \'" + tableDir.toString() + "\'"
                        + ", [" + e.getMessage() + "]");
            }
            calculateTableSize();
            lastCommitTableMapSize = getSize();
        }
    }

    @Override
    public String toString() {
        return new String(getClass().getSimpleName() + "[" + tableDir.toAbsolutePath().toString() + "]");
    }

    @Override
    public int getColumnsCount() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            return columnTypes.size();
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            if (columnIndex < 0 || columnIndex >= columnTypes.size()) {
                throw new IndexOutOfBoundsException();
            } else {
                return columnTypes.get(columnIndex);
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public Storeable get(final String key) {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            if (key == null) {
                throw new IllegalArgumentException();
            }
            if (getDiffDeletionsSet().contains(key)) {
                return null;
            } else if (getDiffChangesMap().containsKey(key)) {
                return deserializeWrapper(getDiffChangesMap().get(key));
            } else {
                tableMapLock.readLock().lock();
                try {
                    return deserializeWrapper(getTablePartByKey(key).get(key));
                } finally {
                    tableMapLock.readLock().unlock();
                }
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public Storeable put(final String key, final Storeable value) {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            if (key == null || value == null) {
                throw new IllegalArgumentException();
            }
            checkStoreableValueValidity(value);
            if (getDiffChangesMap().containsKey(key)) {   // Was changed/added in current commit.
                String uncommitedValue = getDiffChangesMap().get(key);
                getDiffChangesMap().put(key, serializeWrapper(value));
                return deserializeWrapper(uncommitedValue);
            } else if (getDiffDeletionsSet().contains(key)) { // Was deleted in current commit.
                getDiffDeletionsSet().remove(key);
                getDiffChangesMap().put(key, serializeWrapper(value));
                incSize();
                return null;
            } else {    // It hasn't been deleted or changed yet. We change/add this key-value pair now.
                Storeable oldValue;
                tableMapLock.readLock().lock();
                try {
                    oldValue = deserializeWrapper(getTablePartByKey(key).get(key));
                } finally {
                    tableMapLock.readLock().unlock();
                }
                if (oldValue != null) { // Changing.
                    getDiffChangesMap().put(key, serializeWrapper(value));
                    return oldValue;
                } else {    // Adding.
                    getDiffChangesMap().put(key, serializeWrapper(value));
                    incSize();
                    return null;
                }
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public List<String> list() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            // Append old/changed keys (and not deleted) to list.
            List<String> keysList = new LinkedList<>();
            tableMapLock.readLock().lock();
            try {
                for (List<Map<String, String>> list : lastCommitTableMap) {
                    for (Map<String, String> map : list) {
                        keysList.addAll(
                                map
                                .keySet()
                                .stream()
                                .filter(key -> !getDiffDeletionsSet().contains(key))
                                .collect(Collectors.toList()));
                    }
                }

                // Append NEW keys to list.
                keysList.addAll(
                        getDiffChangesMap()
                        .keySet()
                        .stream()
                        .filter(key -> !lastCommitTableMap.contains(key))
                        .collect(Collectors.toList()));
                return keysList;
            } finally {
                tableMapLock.readLock().unlock();
            }
        } finally {
            wholeTableLock.readLock().lock();
        }
    }

    @Override
    public Storeable remove(final String key) {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            if (key == null) {
                throw new IllegalArgumentException();
            }
            Storeable prevCommitValue;
            tableMapLock.readLock().lock();
            try {
                prevCommitValue = deserializeWrapper(getTablePartByKey(key).get(key));
                // This pair was deleted in this commit or hasn't been
                // changed in this commit and was absent in previous commit.
                if ((prevCommitValue == null && !getDiffChangesMap().containsKey(key))
                        || getDiffDeletionsSet().contains(key)) {
                    return null;
                } else {
                    getDiffDeletionsSet().add(key);
                    decSize();
                    if (!getDiffChangesMap().containsKey(key)) {   // Then prevCommitValue != null.
                        return prevCommitValue;
                    } else {
                        Storeable oldValue = deserializeWrapper(getDiffChangesMap().get(key));
                        getDiffChangesMap().remove(key);
                        return oldValue;
                    }
                }
            } finally {
                tableMapLock.readLock().unlock();
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public int commit() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            if (getDiffDeletionsSet().size() + getDiffChangesMap().size() == 0) {
                return 0;
            }
            tableMapLock.writeLock().lock();
            try {
                commitChangesToTableMap();
                dump();
                int res = getDiffChangesMap().size() + getDiffDeletionsSet().size();
                getDiffChangesMap().clear();
                getDiffDeletionsSet().clear();
                lastCommitTableMapSize = getSize();
                return res;
            } catch (IOException e) {
                throw new RuntimeException("can't dump table on commit: " + e.getMessage());
            } finally {
                tableMapLock.writeLock().unlock();
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public int rollback() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            tableMapLock.writeLock().lock();
            try {
                return rollbackWithoutLock();
            } finally {
                tableMapLock.writeLock().unlock();
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    private int rollbackWithoutLock() {
        if (getDiffDeletionsSet().size() + getDiffChangesMap().size() == 0) {
            return 0;
        }
        int cancelledChanges = getDiffChangesMap().size() + getDiffDeletionsSet().size();
        getDiffDeletionsSet().clear();
        getDiffChangesMap().clear();
        setSize(lastCommitTableMapSize);
        return cancelledChanges;
    }


    @Override
    public String getName() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            return Utility.getNameByPath(tableDir);
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            return getSize();
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    @Override
    public int getNumberOfUncommittedChanges() {
        wholeTableLock.readLock().lock();
        try {
            checkClosed();
            tableMapLock.readLock().lock();
            try {
                return getDiffChangesMap().size() + getDiffDeletionsSet().size();
            } finally {
                tableMapLock.readLock().unlock();
            }
        } finally {
            wholeTableLock.readLock().unlock();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void close() {
        wholeTableLock.writeLock().lock();
        try {
            checkClosed();
            rollbackWithoutLock();
            closed = true;
        } finally {
            wholeTableLock.writeLock().unlock();
        }
    }

    private String serializeWrapper(final Storeable value) {
        return tableProvider.serialize(this, value);
    }

    private Storeable deserializeWrapper(final String value) {
        if (value == null) {
            return null;
        } else {
            try {
                return tableProvider.deserialize(this, value);
            } catch (ParseException e) {
                throw new IllegalArgumentException("error while deserializing value \""
                        + value + "\": " + e.getMessage());
            }
        }
    }

    private void commitChangesToTableMap() {
        getDiffChangesMap().forEach((key, value) -> getTablePartByKey(key).put(key, value));
        getDiffDeletionsSet().forEach(deletedKey -> getTablePartByKey(deletedKey).remove(deletedKey));
    }

    private Map<String, String> getTablePartByKey(final String key) {
        int dir = getDirNumByKey(key);
        int file = getFileNumByKey(key);
        return lastCommitTableMap.get(dir).get(file);
    }

    private void calculateTableSize() {
        for (List<Map<String, String>> list : lastCommitTableMap) {
            for (Map<String, String> map : list) {
                incSize(map.size());
            }
        }
    }

    private boolean checkStoreableValueValidity(final Storeable value) {
        for (int i = 0; i < columnTypes.size(); ++i) {
            if (value.getColumnAt(i) != null && !columnTypes.get(i).equals(value.getColumnAt(i).getClass())) {
                throw new ColumnFormatException("types incompatibility: column index " + i
                        + ", table type: " + TypeStringTranslator.getStringNameByType(columnTypes.get(i))
                        + ", passed type: "
                        + TypeStringTranslator.getStringNameByType(value.getColumnAt(i).getClass()));
            }
        }
        return true;
    }

    private void dump() throws IOException {
        TableLoaderDumper.dumpTable(tableDir, lastCommitTableMap);
    }

    private static int getDirNumByKey(final String key) {
        char b = key.charAt(0);
        return b % MAX_DIRS_FOR_TABLE;
    }

    private static int getFileNumByKey(final String key) {
        char b = key.charAt(0);
        return b / MAX_DIRS_FOR_TABLE % MAX_FILES_FOR_DIR;
    }

    private void initHashMaps() {
        lastCommitTableMap = new ArrayList<>(MAX_DIRS_FOR_TABLE);
        for (int i = 0; i < MAX_DIRS_FOR_TABLE; ++i) {
            lastCommitTableMap.add(new ArrayList<>(MAX_FILES_FOR_DIR));
            for (int j = 0; j < MAX_FILES_FOR_DIR; ++j) {
                lastCommitTableMap.get(i).add(new HashMap<String, String>());
            }
        }
    }

    private Map<String, String> getDiffChangesMap() {
        return diff.get().changesMap;
    }

    private Set<String> getDiffDeletionsSet() {
        return diff.get().deletionsSet;
    }

    private Integer getSize() {
        return size.get();
    }

    private void setSize(Integer value) {
        size.set(value);
    }

    private void incSize() {
        setSize(getSize() + 1);
    }

    private void incSize(Integer incBy) {
        setSize(getSize() + incBy);
    }

    private void decSize() {
        setSize(getSize() - 1);
    }
}
