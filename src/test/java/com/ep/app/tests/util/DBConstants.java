package com.ep.app.tests.util;

public final class DBConstants {

    private DBConstants() {} // prevent object creation

    public static final String DB_NAME = "apidb";
    public static final String TABLE_NAME = "api_data";

    public static final String URL =
            "jdbc:mysql://localhost:3306/" + DB_NAME;

    public static final String USER = "root";
    public static final String PASS = "Database@123";
}
