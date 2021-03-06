package walkthrough.toolWindow;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import walkthrough.toolWindow.highlightingModel.HighlightingService;
import walkthrough.toolWindow.tutorialModel.TutorialService;
import walkthrough.toolWindow.tutorialModel.TutorialStep;
import walkthrough.toolWindow.utils.Constants;
import walkthrough.toolWindow.utils.Event;
import walkthrough.toolWindow.utils.Observer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author Daniela
 */
public class MyToolWindowFactory implements ToolWindowFactory, Observer {

    private Project project;
    private ToolWindow toolWindow;
    private HighlightingService highlightingService = ServiceManager.getService(HighlightingService.class);
    private TutorialService tutorialService = ServiceManager.getService(TutorialService.class);
    private TutorialView tutorialView;

    /**
     * register toolWindow to the manager
     * init content of tutorial
     * init highlighting windows
     * set content to toolWindow
     *
     * @param project    The project that is opened in the plugin IDE
     * @param toolWindow ToolWindow with the id "Walkthrough durch IntelliJ"
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        initView();
        initTutorialServices();
    }

    /**
     * Start observing TutorialService
     * Read tutorial JSON - location defined in Constants
     */
    private void initTutorialServices() {
        tutorialService.addListener(this);
        try {
            InputStream inputStream = getClass().getResourceAsStream(Constants.BASIC_TUTORIAL);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            String inputString;
            while ((inputString = br.readLine()) != null) {
                stringBuilder.append(inputString);
            }

            tutorialService.initTutorialFromFile(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when start button is clicked - shadowFrame is created and shown
     */
    private void initHighlightingService() {
        highlightingService.addListener(this);
        highlightingService.setupHighlighting(project, tutorialService.getTargetList());
        highlightingService.isShadowingRunning(true);
        highlightingService.loadAssets();
    }

    /**
     * TutorialView is initialised
     */
    private void initView() {
        tutorialView = new TutorialView();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(tutorialView.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Event management from observed classes
     */
    @Override
    public void onEvent(Event event) {
        TutorialStep step;
        if (event.msg.equals(Constants.TUTORIAL_LOADED)) {
            step = tutorialService.getIntroScreen();
            tutorialView.setContent(step);
            tutorialView.setInfo(tutorialService.getTutorialInfo());
        }
        if (event.msg.equals(Constants.TUTORIAL_STARTED)) {
            initHighlightingService();
        }
        if (event.msg.equals(Constants.ASSETS_LOADED)) {
            tutorialView.changeUI(Constants.ASSETS_LOADED);
            step = tutorialService.getFirstStep();

            highlightingService.setHighlightForArea(tutorialService.getCurrentStep());
            tutorialView.setContent(step);
        }
        if (event.msg.equals(Constants.NEXT_STEP)) {
            step = tutorialService.onNextStepSelected();

            highlightingService.setHighlightForArea(tutorialService.getCurrentStep());
            tutorialView.setContent(step);
        }
        if (event.msg.equals(Constants.PREVIOUS_STEP)) {
            step = tutorialService.onPreviousStepSelected();

            highlightingService.setHighlightForArea(tutorialService.getCurrentStep());
            tutorialView.setContent(step);
        }
        if (event.msg.equals(Constants.ADD_CODE)) {
            highlightingService.setCodeToEditor();
        }
        if (event.msg.equals(Constants.TUTORIAL_ENDING)) {
            tutorialView.changeUI(Constants.TUTORIAL_ENDING);
        }
        if (event.msg.equals(Constants.RESTART)) {
            highlightingService.updateAssets(true);

            step = tutorialService.onTutorialRestarted();
            tutorialView.setContent(step);
            tutorialView.changeUI(Constants.RESTART);
        }
        if (event.msg.equals(Constants.FINISH)) {
            toolWindow.hide(null);
            highlightingService.isShadowingRunning(false);

            step = tutorialService.onTutorialRestarted();
            tutorialView.setContent(step);
            tutorialView.changeUI(Constants.FINISH);
        }
    }

}
