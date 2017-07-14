import com.dataspec.cassandra.CassandraConfiguration;
import com.dataspec.cassandra.CassandraConnectionHandle;
import com.dataspec.connection.ConnectionHandle;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.UUID;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatabaseUtilTest {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 9042;
    private static final String KEYSPACE = "test_keyspace";
    private DatabaseUtil databaseUtil;

    @BeforeClass
    public static void initializeKeyspace() {
        final Session session = getCluster().connect();
        final String createKeyspaceStatement = format("CREATE KEYSPACE if not exists %s " +
                "WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};", KEYSPACE);

        session.execute(createKeyspaceStatement);
    }

    @Before
    public void initializeDatabaseUtil() {
        CassandraConfiguration config = new CassandraConfiguration(9042, KEYSPACE, "localhost");
        ConnectionHandle handle = new CassandraConnectionHandle(config);
        databaseUtil = new DatabaseUtil(handle);
    }

    @Test
    public void execute_shouldWriteToDatabase() {
        insertTestTable();

        final String randomValue = UUID.randomUUID().toString();
        databaseUtil.execute(format("insert into test_table (key, value) values('test-key', '%s');", randomValue));

        Row result = getCluster().connect(KEYSPACE).execute("select * from test_table").one();
        assertThat(result.getString("key"), is("test-key"));
        assertThat(result.getString("value"), is(randomValue));
    }

    @Ignore("todo")
    @Test
    public void execute_shouldReadFromDatabase() {
    }

    private void insertTestTable() {
        final Session session = getCluster().connect(KEYSPACE);
        session.execute("create table if not exists test_table(" +
                "key text," +
                "value text," +
                "primary key (key));");
        session.execute("truncate table test_table");
        session.close();
    }


    private static Cluster getCluster() {
        final InetSocketAddress cassandraUrl = new InetSocketAddress(SERVER_IP, PORT);
        return Cluster.builder().addContactPointsWithPorts(cassandraUrl).build();
    }
}