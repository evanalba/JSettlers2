package soctest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import soc.client.SOCPlayerClient;
import soc.client.SwingMainDisplay;

// Alternatively, extend a AssertJ Swing test case
//
// https://assertj-swing.readthedocs.io/en/latest/assertj-swing-getting-started/#putting-everything-together

public class TestGuiSmoke extends AssertJSwingJUnitTestCase {
    private FrameFixture window;

    // AssertJ Swing test setup
    @Override
    protected void onSetUp() {
        JFrame frame = GuiActionRunner.execute(() -> {
            SOCPlayerClient client = new SOCPlayerClient();

            JFrame f = new JFrame("JSettlers Smoke");
            f.setName("mainFrame");

            int displayScale = 1;

            SwingMainDisplay mainDisplay = new SwingMainDisplay(true, client, displayScale);
            client.setMainDisplay(mainDisplay);

            // CRIT: build the UI (buttons, panels, etc.)
            mainDisplay.initVisualElements();

            f.getContentPane().setLayout(new BorderLayout());
            f.getContentPane().add(mainDisplay, BorderLayout.CENTER);

            return f;
        });

        window = new FrameFixture(robot(), frame);
        window.show(new Dimension(800, 600));
    }

    @Override
    protected void onTearDown() {
        if (window != null) window.cleanUp();
    }

    // App launches and main window is visible
    @Test
    public void appLaunches_mainWindowVisible() throws InterruptedException {
        window.requireVisible();
        window.requireTitle("JSettlers Smoke");
        assertThat(window.target().getTitle()).contains("JSettlers");
    }
    
    
    // Clicking the Practice button should open the NewGameOptionsFrame
    @Test
    public void practice_opensNewGameOptions() throws InterruptedException {
        JButtonFixture practice = window.button(
            new GenericTypeMatcher<JButton>(JButton.class) {
                @Override
                protected boolean isMatching(JButton b) {
                    return b.isShowing()
                            && b.getText() != null
                            && b.getText().equals("Practice");
                }
            }
        );

        practice.requireEnabled();
        practice.click();

        robot().waitForIdle();
        
        // Find correct dialog window of New Game
        DialogFixture ngof = WindowFinder.findDialog(
            new GenericTypeMatcher<JDialog>(JDialog.class) {
                @Override
                protected boolean isMatching(JDialog d) {
                    return d.isShowing()
                            && d.getClass().getSimpleName().equals("NewGameOptionsFrame");
                }
            }
        ).withTimeout(10, TimeUnit.SECONDS)
         .using(robot());

        ngof.requireVisible();
        ngof.close();
    }
    
   

    // App exits cleanly
    @Test
    public void appExits_cleanly() {
        window.close();
        window.requireNotVisible();
    }
}