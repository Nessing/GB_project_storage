package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Database database;
    private Connection connection;

    {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/accounts.db");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    private Statement statement;

    {
        try {
            statement = connection.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public static Database getInstance() {
        if (database == null) {
            return new Database();
        }
        return new Database();
    }

    public void createDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
    }
}
