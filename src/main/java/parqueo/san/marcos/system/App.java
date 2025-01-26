package parqueo.san.marcos.system;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import parqueo.san.marcos.system.util.FlowController;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        FlowController.getInstance().InitializeFlow(stage, null);
        stage.getIcons()
                .add(new Image(getClass().getResourceAsStream("/parqueo/san/marcos/system/resources/logo.jpeg")));
        stage.setTitle("Parqueo Parroquial San Marcos");
        FlowController.getInstance().goMain("MainView");

    }

    public static void main(String[] args) {
        launch();
    }

}