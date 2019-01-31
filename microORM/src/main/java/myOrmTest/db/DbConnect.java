package myOrmTest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbConnect implements AutoCloseable{
    private static final String CREATE_IF_NO_EXIST = "?createDatabaseIfNotExist=true";
    private static final String MYSQL_CONFIG = "&useSSL=false";

    private static Connection connection = null;













    /** Формирование строки подключения к базе данных или ее создания, если она отсутствует **/
    public static void initConnection(String driver,String username,
                                      String password, String host,
                                      String port, String dbName) throws SQLException {
        Properties connectionProp = new Properties();
        connectionProp.put("user", username);
        connectionProp.put("password", password);
        switch (driver) {
            case "mysql":
                connection = DriverManager.getConnection("jdbc:" + driver + "://" + host + ":"
                        + port + "/" + dbName + CREATE_IF_NO_EXIST + MYSQL_CONFIG +
                        "&requireSSL=false" +
                        "&useLegacyDatetimeCode=false" +
                        "&amp" +
                        "&serverTimezone=UTC", connectionProp);
                break;
            case "sqlserver":
                connection = DriverManager.getConnection("jdbc:" + driver + "://" + host + ":"
                        + port + ";databaseName=" + dbName, connectionProp);
                break;
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
