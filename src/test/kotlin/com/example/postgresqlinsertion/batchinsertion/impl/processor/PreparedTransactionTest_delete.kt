package com.example.postgresqlinsertion.batchinsertion.impl.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.util.*
import javax.persistence.EntityManager
import javax.sql.DataSource

@SpringBootTest
@ContextConfiguration(initializers = [PreparedTransactionTest_delete.Initializer::class])
@Suppress("UNCHECKED_CAST")
internal class PreparedTransactionTest_delete {

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var em: EntityManager

    companion object {
        @ClassRule
        @JvmField
        val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14.6-alpine")
            .withCommand("postgres", "-c", "max_prepared_transactions=100")

    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            postgreSQLContainer.start()
            TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.jdbcUrl,
                "spring.datasource.username=" + postgreSQLContainer.username,
                "spring.datasource.password=" + postgreSQLContainer.password
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @Test
    fun `save data with two connection in one prepared transaction`() {
        val conn1 = getConnectionWithTransaction()
        val conn2 = getConnectionWithTransaction()
        val conn3 = dataSource.connection
        val transactionId1 = UUID.randomUUID().toString()
        val transactionId2 = UUID.randomUUID().toString()
        val prop20 = "PrepTransaction"
        insertWithPreparedTransactionAndCheckResult(conn1, transactionId1, "111", "222", prop20)
        insertWithPreparedTransactionAndCheckResult(conn2, transactionId2, "333", "444", prop20)

        conn3.createStatement().use { stmt ->
            stmt.execute("commit prepared '$transactionId1'")
            stmt.execute("commit prepared '$transactionId2'")
        }

        val savedDoc = em.createNativeQuery("select prop_10, prop_15, prop_20 from payment_document where prop_20 = '$prop20'").resultList as List<Array<Any>>
        val preparedData1 = em.createNativeQuery("select gid from pg_prepared_xacts where gid = '$transactionId1'").resultList as List<Array<Any>>
        val preparedData2 = em.createNativeQuery("select gid from pg_prepared_xacts where gid = '$transactionId2'").resultList as List<Array<Any>>
        assertThat(savedDoc.size).isEqualTo(2)
        assertThat(savedDoc[0][0]).isEqualTo("111")
        assertThat(savedDoc[0][1]).isEqualTo("222")
        assertThat(savedDoc[1][0]).isEqualTo("333")
        assertThat(savedDoc[1][1]).isEqualTo("444")
        assertThat(preparedData1.size).isEqualTo(0)
        assertThat(preparedData2.size).isEqualTo(0)

        conn1.close()
        conn2.close()
        conn3.close()
    }

    private fun getConnectionWithTransaction(): Connection {
        val conn = dataSource.connection
        conn.autoCommit = false
        return conn
    }

    private fun insertWithPreparedTransactionAndCheckResult(
        conn: Connection,
        transactionId: String,
        prop10: String,
        prop15: String,
        prop20: String
    ) {
        conn.createStatement().use { stmt ->
            stmt.execute("insert into payment_document (prop_10, prop_15, prop_20) values ('$prop10', '$prop15', '$prop20')")
            stmt.execute("prepare transaction '$transactionId'")
        }

        val savedDoc = em.createNativeQuery("select prop_10, prop_15, prop_20 from payment_document where prop_20 = '$prop20'").resultList as List<Array<Any>>
        val preparedDate = em.createNativeQuery("select gid from pg_prepared_xacts where gid = '$transactionId'").resultList as List<Array<Any>>
        assertThat(savedDoc.size).isEqualTo(0)
        assertThat(preparedDate.size).isEqualTo(1)

    }

}