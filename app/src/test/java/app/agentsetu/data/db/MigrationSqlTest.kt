package app.agentsetu.data.db

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hand-written migration SQL must create exactly the tables and indexes Room expects, or the
 * app would crash on first open after the update. Room exports the expected schema to app/schemas
 * during compilation (room.schemaLocation), which runs before this test.
 */
class MigrationSqlTest {

    private val schema = File("schemas/app.agentsetu.data.db.AgentSetuDatabase/${AgentSetuDatabase.VERSION}.json")

    private fun entity(table: String) = Json.parseToJsonElement(schema.readText()).jsonObject
        .getValue("database").jsonObject
        .getValue("entities").jsonArray
        .map { it.jsonObject }
        .single { it.getValue("tableName").jsonPrimitive.content == table }

    private fun createSql(table: String): String =
        entity(table).getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)

    private fun indexSql(table: String): List<String> =
        entity(table).getValue("indices").jsonArray.map {
            it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)
        }

    @Test
    fun schemaFileExported() {
        assertTrue("Room did not export ${schema.path}; check room.schemaLocation", schema.isFile)
    }

    @Test
    fun receiptTableMatchesRoom() {
        assertEquals(createSql("commission_receipt"), Migrations.V3.CREATE_RECEIPT)
        assertEquals(listOf(Migrations.V3.INDEX_RECEIPT_ENTRY), indexSql("commission_receipt"))
    }

    @Test
    fun statementTableMatchesRoom() {
        assertEquals(createSql("incentive_statement"), Migrations.V3.CREATE_STATEMENT)
        assertEquals(listOf(Migrations.V3.INDEX_STATEMENT_MONTH), indexSql("incentive_statement"))
    }

    @Test
    fun holdingHasNullableRefNumber() {
        val sql = createSql("holding")
        assertTrue(sql, "`refNumber` TEXT," in sql)
        assertFalse(sql, "`refNumber` TEXT NOT NULL" in sql)
        // ALTER TABLE adds a nullable TEXT column with no default, which is what Room expects here.
        assertEquals("ALTER TABLE `holding` ADD COLUMN `refNumber` TEXT", Migrations.V4.ADD_REF_NUMBER)
    }

    @Test
    fun everyVersionAboveTwoHasAMigration() {
        val covered = Migrations.ALL.map { it.startVersion to it.endVersion }
        for (v in 2 until AgentSetuDatabase.VERSION) {
            assertTrue("no migration from $v to ${v + 1}", (v to v + 1) in covered)
        }
    }
}
