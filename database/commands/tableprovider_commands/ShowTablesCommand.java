package ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.tableprovider_commands;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.DbCommand;

import java.io.PrintStream;
import java.util.List;


public class ShowTablesCommand extends DbCommand {
    public ShowTablesCommand(final String[] args, final TableProvider tableProvider) {
        super("show tables", 0, args, tableProvider);
    }

    @Override
    protected void execChecked(final PrintStream out) {
        List<String> tableNames = tableProvider.getTableNames();
        out.println("table_name table_size");
        if (tableNames.size() != 0) {
            StringBuilder msgBuilder = new StringBuilder();
            for (String tableName : tableNames) {
                int tableSize = tableProvider.getTable(tableName).size();
                msgBuilder.append(tableName + " " + tableSize);
                msgBuilder.append(System.lineSeparator());
            }
            out.print(msgBuilder.toString());
        }
    }
}
