package ru.fizteh.fivt.students.dmitry_persiyanov.database.net.server.commands_parser;

import ru.fizteh.fivt.students.dmitry_persiyanov.database.commands_parser.DbCommandsParser;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.DbTableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.InterpreterCommand;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.InterpreterCommandsParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by drack3800 on 22.12.2014.
 */
public class NetDbCommandsParser implements InterpreterCommandsParser {
    private final DbCommandsParser dbCommandsParser;

    public NetDbCommandsParser(final DbTableProvider dbTableProvider) {
        dbCommandsParser = new DbCommandsParser(dbTableProvider);
    }

    @Override
    public List<InterpreterCommand> parseAllInput(Scanner input) {
        String inputLine = input.nextLine();
        String[] strCommands = inputLine.trim().split(dbCommandsParser.COMMANDS_SEPARATOR);
        List<InterpreterCommand> commands = new LinkedList<>();
        for (String strCommand : strCommands) {
           // commands.add(parseCommand(strCommand));
        }
        return commands;
    }

//    private InterpreterCommand parseCommand(final String strCommand) {
//        try {
//            return dbCommandsParser.parseOneCommand(new Scanner(strCommand));
//        } catch (WrongCommandException e) {
//            String[] cmdChunks = strCommand.trim().split(dbCommandsParser.DELIMITER_REGEXP);
//            String[] commandArgs = Arrays.copyOfRange(cmdChunks, 1, cmdChunks.length);
//
//
//
//        }
//    }
}
