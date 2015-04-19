package ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.tableprovider_commands;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.DbCommand;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.utils.TypeStringTranslator;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.exceptions.TableIsNotChosenException;

import java.io.PrintStream;

/**
 * Created by drack3800 on 08.12.2014.
 */
public class DescribeCommand extends DbCommand {
    public DescribeCommand(String[] args, TableProvider tableProvider) {
        super("describe", 1, args, tableProvider);
    }

    @Override
    protected void execChecked(final PrintStream out) throws TableIsNotChosenException {
        out.print("( ");
        for (int i = 0; i < currentTable.getColumnsCount(); ++i) {
            out.print(TypeStringTranslator.getStringNameByType(currentTable.getColumnType(i)) + " ");
        }
        out.println(")");
    }
}
