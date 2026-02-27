/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2026 Ricky Sparks
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soctest.db;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import soc.server.database.BCrypt;
import soc.server.database.SOCDBHelper;

/**
 * Tests for {@link SOCDBHelper} covering account-path validation,
 * password-length checks at both schema levels, and property-validation
 * edge cases in {@link SOCDBHelper#initialize(String, String, Properties)}.
 * <P>
 * Running these tests does not require a live database or JDBC driver.
 *
 * @since 2.7.00
 */
public class TestSOCDBHelper
{
    private SOCDBHelper db;

    @Before
    public void setUp()
    {
        db = new SOCDBHelper();
    }

    @Test
    public void testSchemaVersionOrdering()
    {
        assertTrue("ORIGINAL < 1200",
            SOCDBHelper.SCHEMA_VERSION_ORIGINAL < SOCDBHelper.SCHEMA_VERSION_1200);
        assertTrue("1200 < 2000",
            SOCDBHelper.SCHEMA_VERSION_1200 < SOCDBHelper.SCHEMA_VERSION_2000);
        assertEquals("LATEST == 2000",
            SOCDBHelper.SCHEMA_VERSION_2000, SOCDBHelper.SCHEMA_VERSION_LATEST);
    }

    @Test
    public void testPasswordLengthOK_nullPassword()
    {
        assertFalse(db.isPasswordLengthOK(null));
    }

    @Test
    public void testPasswordLengthOK_emptyPassword()
    {
        assertFalse(db.isPasswordLengthOK(""));
    }

    @Test
    public void testPasswordLengthOK_shortPassword_originalSchema()
    {
        assertTrue(db.isPasswordLengthOK("abc"));
    }

    @Test
    public void testPasswordLengthOK_atMaxLen_originalSchema()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SOCDBHelper.PW_MAX_LEN_SCHEME_NONE; i++)
            sb.append('x');
        assertTrue("exactly at max for original schema", db.isPasswordLengthOK(sb.toString()));
    }

    @Test
    public void testPasswordLengthOK_tooLong_originalSchema()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= SOCDBHelper.PW_MAX_LEN_SCHEME_NONE; i++)
            sb.append('x');
        assertFalse("one past max for original schema", db.isPasswordLengthOK(sb.toString()));
    }

    @Test
    public void testPasswordLengthOK_bcryptSchema_shortPassword()
        throws Exception
    {
        setSchemaVersion(db, SOCDBHelper.SCHEMA_VERSION_1200);
        assertTrue(db.isPasswordLengthOK("hello"));
    }

    @Test
    public void testPasswordLengthOK_bcryptSchema_atMax()
        throws Exception
    {
        setSchemaVersion(db, SOCDBHelper.SCHEMA_VERSION_1200);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SOCDBHelper.PW_MAX_LEN_SCHEME_BCRYPT; i++)
            sb.append('a');
        assertTrue("exactly at max for bcrypt schema", db.isPasswordLengthOK(sb.toString()));
    }

    @Test
    public void testPasswordLengthOK_bcryptSchema_tooLong()
        throws Exception
    {
        setSchemaVersion(db, SOCDBHelper.SCHEMA_VERSION_1200);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= SOCDBHelper.PW_MAX_LEN_SCHEME_BCRYPT; i++)
            sb.append('a');
        assertFalse("one past max for bcrypt schema", db.isPasswordLengthOK(sb.toString()));
    }

    @Test
    public void testGetMaxPasswordLength_originalSchema()
    {
        assertEquals(SOCDBHelper.PW_MAX_LEN_SCHEME_NONE, db.getMaxPasswordLength());
    }

    @Test
    public void testGetMaxPasswordLength_bcryptSchema()
        throws Exception
    {
        setSchemaVersion(db, SOCDBHelper.SCHEMA_VERSION_1200);
        assertEquals(SOCDBHelper.PW_MAX_LEN_SCHEME_BCRYPT, db.getMaxPasswordLength());
    }

    @Test
    public void testGetMaxPasswordLength_latestSchema()
        throws Exception
    {
        setSchemaVersion(db, SOCDBHelper.SCHEMA_VERSION_2000);
        assertEquals(SOCDBHelper.PW_MAX_LEN_SCHEME_BCRYPT, db.getMaxPasswordLength());
    }

    @Test
    public void testNotInitializedByDefault()
    {
        assertFalse(db.isInitialized());
    }

    @Test
    public void testSchemaVersionDefault()
    {
        assertEquals(0, db.getSchemaVersion());
    }

    @Test
    public void testInitialize_nullUser_returnsQuietly()
        throws Exception
    {
        db.initialize(null, null, null);
        assertFalse(db.isInitialized());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_urlWithoutDriver_throws()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_URL, "jdbc:othertype:...");
        db.initialize("u", "p", props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_driverWithoutUrl_throws()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_DRIVER, "com.example.othertype");
        db.initialize("u", "p", props);
    }

    @Test(expected = SQLException.class)
    public void testInitialize_missingDriverClass_throwsSQLException()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_URL, "jdbc:othertype:...");
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_DRIVER, "com.example.notexist");
        db.initialize("u", "p", props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_bcryptWorkFactor_nonInteger()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_BCRYPT_WORK__FACTOR, "12.5");
        db.initialize("u", "p", props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_bcryptWorkFactor_belowMin()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_BCRYPT_WORK__FACTOR,
            String.valueOf(SOCDBHelper.BCRYPT_MIN_WORK_FACTOR - 1));
        db.initialize("u", "p", props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_bcryptWorkFactor_aboveMax()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_BCRYPT_WORK__FACTOR,
            String.valueOf(BCrypt.GENSALT_MAX_LOG2_ROUNDS + 1));
        db.initialize("u", "p", props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitialize_settingsNotWrite()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_SETTINGS, "xyz");
        db.initialize("u", "p", props);
    }

    @Test(expected = SQLException.class)
    public void testInitialize_postgresUrl_autoDetectsDriver()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_URL, "jdbc:postgresql://localhost/socdata");
        db.initialize("u", "p", props);
        // reaches JDBC load which throws SQLException because the driver jar is absent;
        // the important thing is no IllegalArgumentException about missing driver
    }

    @Test(expected = SQLException.class)
    public void testInitialize_sqliteUrl_autoDetectsDriver()
        throws Exception
    {
        Properties props = new Properties();
        props.put(SOCDBHelper.PROP_JSETTLERS_DB_URL, "jdbc:sqlite:testdb.sqlite");
        db.initialize("u", "p", props);
    }

    @Test
    public void testPasswordSchemeConstants()
    {
        assertTrue("BCRYPT scheme > NONE scheme",
            SOCDBHelper.PW_SCHEME_BCRYPT > SOCDBHelper.PW_SCHEME_NONE);
        assertEquals(0, SOCDBHelper.PW_SCHEME_NONE);
        assertEquals(1, SOCDBHelper.PW_SCHEME_BCRYPT);
    }

    @Test
    public void testBCryptWorkFactorRange()
    {
        assertTrue("min work factor >= 9",
            SOCDBHelper.BCRYPT_MIN_WORK_FACTOR >= 9);
        assertTrue("default >= min",
            SOCDBHelper.BCRYPT_DEFAULT_WORK_FACTOR >= SOCDBHelper.BCRYPT_MIN_WORK_FACTOR);
        assertTrue("default <= max",
            SOCDBHelper.BCRYPT_DEFAULT_WORK_FACTOR <= BCrypt.GENSALT_MAX_LOG2_ROUNDS);
    }

    /**
     * Use reflection to set the private {@code schemaVersion} field
     * so we can test password-length logic at different schema levels
     * without connecting to a real database.
     */
    private static void setSchemaVersion(SOCDBHelper helper, int version)
        throws Exception
    {
        Field f = SOCDBHelper.class.getDeclaredField("schemaVersion");
        f.setAccessible(true);
        f.setInt(helper, version);
    }

    public static void main(String[] args)
    {
        org.junit.runner.JUnitCore.main("soctest.db.TestSOCDBHelper");
    }

}
