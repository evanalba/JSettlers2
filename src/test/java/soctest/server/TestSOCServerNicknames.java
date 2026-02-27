package soctest.server;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import soc.server.SOCServer;
import soc.message.SOCStatusMessage;
import soc.server.genericServer.Connection;
import soc.server.genericServer.StringConnection;

public class TestSOCServerNicknames {
    // Any nickname with a pipe (|) should be rejected since pipes are used in message parsing
    @Test
    public void testCheckPipeRejected() throws Exception
    {
        String nickname = "bad|name";  // Very bad; no pipes allowed D:

        // create server
        SOCServer server = new SOCServer("test", 0, null, null);

        // create local connections for test
        StringConnection serverSide = new StringConnection();
        StringConnection clientSide = new StringConnection(serverSide);
        serverSide.setAccepted();

        /**  Access private {@link SOCServer#checkNickname()} method for test
         * checkNickname() returns 0 for okay, -1 for okay and taking over connection, -2 for bad
         */
        Method method = SOCServer.class.getDeclaredMethod(
            "checkNickname",
            String.class,
            Connection.class,
            boolean.class,
            boolean.class
        );
        method.setAccessible(true);

        int rc = (Integer) method.invoke(server, nickname, clientSide, false, false);  // Invoke checkNickname on server object

        assertEquals("Piped nickname rejected", -2, rc);
    }
    
    /* Nicknames should not be duplicated unless they have reached timeout
     * The {@link SOCServer#checkNickname()} function should test for this as well
     */
    @Test
    public void testCheckNicknameDuplicateReturnsInUse() throws Exception
    {
        String nickname = "Exists";

        SOCServer server = new SOCServer("test", 0, null, null);

        // create local connection for test
        StringConnection serverSide = new StringConnection();
        StringConnection clientSide = new StringConnection(serverSide);
        serverSide.setAccepted();

        serverSide.setData(nickname);     // set nickname on the connection
        server.addConnection(serverSide); // add to server's connection list

        // new connection for attempted duplicate nickname
        StringConnection newServerSide = new StringConnection();
        StringConnection newClientSide = new StringConnection(newServerSide);
        newServerSide.setAccepted();

        Method method = SOCServer.class.getDeclaredMethod(
            "checkNickname",
            String.class,
            Connection.class,
            boolean.class,
            boolean.class
        );
        method.setAccessible(true);

        int rc = (Integer) method.invoke(server, nickname, newClientSide, false, false);

        // checkNickname() returns 0 if okay, -1 if taking over a nickname, or -2 if bad
        // either -1 or -2 is fine, just not 0
        assertTrue("Duplicate nickname rejected or timed out", rc != 0);
    }
}