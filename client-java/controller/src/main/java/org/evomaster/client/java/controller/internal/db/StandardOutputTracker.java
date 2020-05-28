package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.databasespy.MongoSpyLogger;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.databasespy.P6SpyFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * We use p6spy to intercept SQL commands.
 * That can be done in 3 ways:
 * (1) to file,
 * (2) with a logger,
 * (3) to standard output.
 *
 * <p>
 * (1) is a recipe for disaster: handling generated files, and file locks on Windows.
 * (2) is a nightmare as well, as who knows what the SUT is configuring.
 * (3) is likely the cleanest, easiest option. But it means we need to
 * handle System.out
 *
 * <p>
 * This class can be used for any analyses of the SUT output
 */
public class StandardOutputTracker extends ByteArrayOutputStream {

    private static final PrintStream DEFAULT_OUT = System.out;

    private volatile SutController sutController;


    public static void setTracker(boolean on, SutController sutController) {
        if (on) {
            System.setOut(new PrintStream(new StandardOutputTracker(sutController), true));
        } else {
            System.setOut(DEFAULT_OUT);
        }
    }

    private StandardOutputTracker(SutController sutController) {
        super(2048);
        this.sutController = sutController;
    }

    @Override
    public void flush() throws IOException {

        /*
            Output is written to a buffer.
            Every time it is flushed, we do the actual printing
            on standard output, and analyze its content.
         */

        String data;

        synchronized (this) {
            super.flush();

            data = toString(); //get content of the buffer
            reset();

            DEFAULT_OUT.print(data);
        }

        if (data != null) {
            Arrays.stream(data.split("\n"))
                    .forEach(l -> {
                        if (l.startsWith(P6SpyFormatter.PREFIX)) {
                            handleSqlLine(sutController, l);
                        } else if (l.startsWith(MongoSpyLogger.PREFIX)) {
                            handleMongoLine(sutController, l);
                        }
                    });
        }
    }

    public static void handleMongoLine(SutController sc, String line) {
        Objects.requireNonNull(sc);
        Objects.requireNonNull(line);

        if (!line.startsWith(MongoSpyLogger.PREFIX)) {
            throw new IllegalArgumentException("No MongoSpy prefix");
        }

        String mongoLine = line.substring(MongoSpyLogger.PREFIX.length());

        try {
            sc.handleMongo(mongoLine);
        } catch (Exception | Error e) {
            SimpleLogger.error("Failed to handle Mongo command: '" + mongoLine + "'\n" + e.getMessage());
        }
    }

    public static void handleSqlLine(SutController sc, String line) {
        Objects.requireNonNull(sc);
        Objects.requireNonNull(line);

        if (!line.startsWith(P6SpyFormatter.PREFIX)) {
            throw new IllegalArgumentException("No P6Spy prefix");
        }

        String sql = line.substring(P6SpyFormatter.PREFIX.length());

        try {
            sc.handleSql(sql);
        } catch (Exception | Error e) {
            SimpleLogger.error("Failed to handle SQL command: '" + sql + "'\n" + e.getMessage());
        }
    }

}
