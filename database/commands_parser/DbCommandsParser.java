package ru.fizteh.fivt.students.dmitry_persiyanov.database.commands_parser;

import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.DbCommand;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.table_commands.*;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands.tableprovider_commands.*;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.InterpreterCommand;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.InterpreterCommandsParser;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.exceptions.WrongCommandException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public final class DbCommandsParser implements InterpreterCommandsParser {
    public static final String COMMANDS_SEPARATOR = ";";
    public static final String DELIMITER_REGEXP = "\\s+";
    private TableProvider tableProvider;


    /**
     *
     * @param tableProvider  TableProvider which will be passed to commands' constructors
     */
    public DbCommandsParser(final TableProvider tableProvider) {
        this.tableProvider = tableProvider;
    }

    @Override
    public List<InterpreterCommand> parseAllInput(final Scanner input) {
        String inputLine = input.nextLine();
        String[] strCommands = inputLine.trim().split(COMMANDS_SEPARATOR);
        List<InterpreterCommand> commands = new LinkedList<>();
        for (String strCommand : strCommands) {
            commands.add(parseCommand(strCommand));
        }
        return commands;
    }

    private DbCommand parseCommand(final String strCommand) {
        String[] cmdChunks = strCommand.trim().split(DELIMITER_REGEXP);
        String[] commandArgs;
        if (cmdChunks.length >= 2 && cmdChunks[0].equals("show") && cmdChunks[1].equals("tables")) {
            commandArgs = Arrays.copyOfRange(cmdChunks, 2, cmdChunks.length);
            return new ShowTablesCommand(commandArgs, tableProvider);
        }
        commandArgs = Arrays.copyOfRange(cmdChunks, 1, cmdChunks.length);
        switch (cmdChunks[0]) {
            // Table commands.
            case "put":
                return new PutCommand(makeArgsForPut(commandArgs), tableProvider);
            case "get":
                return new GetCommand(commandArgs, tableProvider);
            case "remove":
                return new RemoveCommand(commandArgs, tableProvider);
            case "list":
                return new ListCommand(commandArgs, tableProvider);
            case "size":
                return new SizeCommand(commandArgs, tableProvider);
            case "commit":
                return new CommitCommand(commandArgs, tableProvider);
            case "rollback":
                return new RollbackCommand(commandArgs, tableProvider);
            case "describe":
                return new DescribeCommand(commandArgs, tableProvider);
            case "exit":
                return new ExitCommand(commandArgs, tableProvider);
            // Database commands.
            case "create":
                return new CreateCommand(makeArgsForCreate(commandArgs), tableProvider);
            case "drop":
                return new DropCommand(commandArgs, tableProvider);
            case "use":
                return new UseCommand(commandArgs, tableProvider);
            default:
                throw new WrongCommandException(strCommand);
        }
    }

    private static String[] makeArgsForPut(final String[] argsSplittedByDelimiter) {
        if (argsSplittedByDelimiter.length == 0) {
            return null;
        } else {
            List<String> args = new LinkedList<>();
            args.add(argsSplittedByDelimiter[0]);
            if (argsSplittedByDelimiter.length > 1) {
                args.add(String.join(" ",
                        Arrays.copyOfRange(argsSplittedByDelimiter, 1, argsSplittedByDelimiter.length)));
            }
            String[] res = new String[args.size()];
            return args.toArray(res);
        }
    }

    private static String[] makeArgsForCreate(final String[] argsSplittedByDelimiter) {
        if (argsSplittedByDelimiter.length == 0) {
            return null;
        } else {
            List<String> args = new LinkedList<>();
            args.add(argsSplittedByDelimiter[0]);
            if (argsSplittedByDelimiter.length > 1) {
                args.add(String.join(" ",
                        Arrays.copyOfRange(argsSplittedByDelimiter, 1, argsSplittedByDelimiter.length)));
            }
            String[] res = new String[args.size()];
            return args.toArray(res);
        }
    }
}
