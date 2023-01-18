package psoft.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import psoft.util.Destroyable;

/* loaded from: hsphere.zip:psoft/db/Database.class */
public interface Database extends Destroyable {
    Connection getConnection() throws SQLException;

    Connection getConnection(String str) throws SQLException;

    Statement getStatement() throws SQLException;

    int getNewId() throws SQLException;

    long getNewIdAsLong() throws SQLException;

    long getNewIdAsLong(String str) throws SQLException;

    int getNewId(String str) throws SQLException;

    String getClobValue(ResultSet resultSet, int i) throws SQLException;

    String getClobValue(ResultSet resultSet, String str) throws SQLException;

    void setClobValue(PreparedStatement preparedStatement, int i, String str) throws SQLException;

    void setClobValue(PreparedStatement preparedStatement, int i, String str, String str2) throws SQLException;

    void close() throws SQLException;

    @Override // psoft.util.Destroyable
    void destroy();

    String quote(String str);

    String quote(Object obj);

    Connection getTransConnection() throws SQLException;

    void commitTransConnection(Connection connection) throws SQLException;
}
