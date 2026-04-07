package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class V7__add_book_chunk_embedding_vector_1024_index extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!"PostgreSQL".equalsIgnoreCase(readDatabaseProduct(connection))) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX IF EXISTS idx_book_chunk_embedding_vector_cosine");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_book_chunk_embedding_vector_1024_cosine " +
                            "ON book_chunk USING hnsw ((embedding_vector::vector(1024)) vector_cosine_ops) " +
                            "WHERE embedding_dim = 1024 AND embedding_vector IS NOT NULL"
            );
        }
    }

    private String readDatabaseProduct(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata == null ? "" : metadata.getDatabaseProductName();
    }
}
