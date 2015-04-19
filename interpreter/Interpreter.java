package ru.fizteh.fivt.students.dmitry_persiyanov.interpreter;

import ru.fizteh.fivt.students.dmitry_persiyanov.interpreter.exceptions.WrongCommandException;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public final class Interpreter {
    public static final String DEFAULT_PROMPT = "$ ";
    public static enum ExecutionMode {INTERACTIVE, BATCH};
    private final String prompt;
    private InterpreterCommandsParser commandsParser;
    private int exitStatus = 0;
    private String greetings = null;

    /**
     * @param commandsParser Commands parser.
     * @param prompt         Greetings sybmol. '$ ' by default.
     */
    public Interpreter(final InterpreterCommandsParser commandsParser, final String prompt) {
        this.commandsParser = commandsParser;
        this.prompt = prompt;
    }

    public Interpreter(final InterpreterCommandsParser commandsParser) {
        this(commandsParser, DEFAULT_PROMPT);
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public void run(final InputStream in, final PrintStream out, final PrintStream err, final ExecutionMode mode) {
        try (Scanner inputScanner = new Scanner(in)) {
            if (greetings != null) {
                out.println(greetings);
            }
            if (mode == ExecutionMode.INTERACTIVE) {
                InterpreterCommand currentCmd;
                while (true) {
                    try {
                        out.print(DEFAULT_PROMPT);
                        currentCmd = commandsParser.parseOneCommand(inputScanner);
                        currentCmd.exec(out, err);
                    } catch (WrongCommandException e) {
                        out.println(e.getMessage());
                    }
                }
            } else {
                for (InterpreterCommand currentCmd : commandsParser.parseAllInput(inputScanner)) {
                    currentCmd.exec(out, err);
                }
            }
        } catch (TerminateInterpeterException e) {
            exitStatus = e.getExitStatus();
        } catch (Exception e) {
            err.println(e.getMessage());
        }
    }

    public void run(final InputStream in, final PrintStream out) {
        run(in, out, System.err, ExecutionMode.INTERACTIVE);
    }

    public void run(final InputStream in, final ExecutionMode mode) {
        run(in, System.out, System.err, mode);
    }

    public void run(final ExecutionMode mode) {
        run(System.in, System.out, System.err, mode);
    }

    public void run(final InputStream in, final PrintStream out, final PrintStream err) {
        run(in, out, err, ExecutionMode.INTERACTIVE);
    }

    public void run() {
        run(ExecutionMode.INTERACTIVE);
    }

    public void setGreetings(final String greetings) {
        this.greetings = greetings;
    }

}
