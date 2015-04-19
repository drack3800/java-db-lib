package ru.fizteh.fivt.students.dmitry_persiyanov.database.commands_parser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.students.dmitry_persiyanov.database.db_table_provider.DbTableProvider;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.InterpreterCommand;
import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.exceptions.WrongCommandException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by drack3800 on 19.12.2014.
 */
public class DbCommandsParserTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private DbTableProvider db;
    private Path tmpDir;
    private DbCommandsParser parser;

    @Before
    public void setUp() throws Exception {
        tmpDir = tmpFolder.newFolder().toPath();
        db = new DbTableProvider(tmpDir);
        parser = new DbCommandsParser(db);
    }

    @Test
    public void testParsingWholeInputWithManyCommands() {
        Scanner input = new Scanner("create t1 (String, int); use t1; size; put k [\"John Doe\", 42]");
        List<InterpreterCommand> commandList = parser.parseAllInput(input);
        List<String> commandNames = commandList.stream().map(InterpreterCommand::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("create", "use", "size", "put"), commandNames);
    }

    @Test
    public void testParsingOneCommand() {
        Scanner input = new Scanner("show tables");
        assertEquals("show tables", parser.parseOneCommand(input).getName());
    }

    @Test(expected = WrongCommandException.class)
    public void testExceptionThrowingForIllegalCommandInParseOneCommand() {
        Scanner input = new Scanner("killeveryone");
        parser.parseOneCommand(input);
    }

    @Test(expected = WrongCommandException.class)
    public void testExceptionThrowingForIllegalCommandInParseAllInput() {
        Scanner input = new Scanner("create table (int, int, int); killeveryone");
        parser.parseAllInput(input);
    }

}
