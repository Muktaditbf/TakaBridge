package com.takabridge.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The single place where the Oracle database settings live.
 *
 * Nothing else in TakaBridge knows the URL, the user name or the password,
 * so moving the application to another machine means editing this one file.
 */
public final class DBConnection {

    // =======================================================================
    //  EDIT HERE - your Oracle Database 21c Express Edition (XE) settings
    // =======================================================================

    /** localhost = this computer, 1521 = the default Oracle port,
     *  XEPDB1  = the default pluggable database of Oracle Database 21c Express Edition (XE). */
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";

    /** The database user created by PART 1 of schema.sql. */
    private static final String USER = "takabridge";

    /** The password given to that user in PART 1 of schema.sql. */
    private static final String PASSWORD = "takabridge123";

    // =======================================================================
    //  Nothing below this line normally needs to change.
    // =======================================================================

    private static final String DRIVER = "oracle.jdbc.OracleDriver";

    /** The driver only has to be located once. */
    private static volatile boolean driverReady = false;

    /** Utility class - it must never be instantiated. */
    private DBConnection() {
    }

    /**
     * Opens a new connection to Oracle.
     *
     * The caller closes it, which every DAO does by opening the connection
     * inside a try-with-resources block.
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverIsPresent();
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Checks that ojdbc17.jar is on the classpath.
     *
     * A missing driver is reported as an ordinary SQLException so the servlets
     * can catch it like any other database problem and show a readable page,
     * instead of letting a raw error reach the visitor.
     */
    private static synchronized void ensureDriverIsPresent() throws SQLException {
        if (driverReady) {
            return;
        }
        try {
            Class.forName(DRIVER);
            driverReady = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "The Oracle JDBC driver was not found. Copy ojdbc17.jar into "
                  + "src/main/webapp/WEB-INF/lib, rebuild, and restart Tomcat.", e);
        }
    }
}
