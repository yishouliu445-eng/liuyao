from psycopg import Connection, connect


def create_connection(dsn: str) -> Connection:
    return connect(dsn)
