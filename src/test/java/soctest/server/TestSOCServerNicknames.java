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
    
}