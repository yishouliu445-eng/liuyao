package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class V6__add_book_chunk_pgvector_support extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String databaseProduct = readDatabaseProduct(connection);
        if ("PostgreSQL".equalsIgnoreCase(databaseProduct)) {
            migratePostgres(connection);
            return;
        }
        migrateFallback(connection);
    }

    private void migratePostgres(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS embedding_vector vector");
        }
    }

    private void migrateFallback(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS embedding_vector VARCHAR(16384)");
        }
    }

    private String readDatabaseProduct(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata == null ? "" : metadata.getDatabaseProductName();
    }
}
