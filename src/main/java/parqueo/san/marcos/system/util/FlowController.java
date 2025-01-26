/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parqueo.san.marcos.system.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import parqueo.san.marcos.system.App;
import parqueo.san.marcos.system.controller.Controller;
import io.github.palexdev.materialfx.css.themes.MFXThemeManager;
import io.github.palexdev.materialfx.css.themes.Themes;

public class FlowController {

    private static FlowController INSTANCE = null;
    private static Stage mainStage;
    private static ResourceBundle idioma;
    private static HashMap<String, FXMLLoader> loaders = new HashMap<>();
    private Controller LogInController;

    private FlowController() {
    }

    private static void createInstance() {
        if (INSTANCE == null) {
            synchronized (FlowController.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FlowController();
                }
            }
        }
    }

    public static FlowController getInstance() {
        if (INSTANCE == null) {
            createInstance();
        }
        return INSTANCE;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @SuppressWarnings("static-access")
    public void InitializeFlow(Stage stage, ResourceBundle idioma) {
        getInstance();
        this.mainStage = stage;
        this.idioma = idioma;
    }

    @SuppressWarnings("static-access")
    private FXMLLoader getLoader(String name) {
        FXMLLoader loader = loaders.get(name);
        if (loader == null) {
            synchronized (FlowController.class) {
                if (loader == null) {
                    try {
                        loader = new FXMLLoader(App.class.getResource("view/" + name + ".fxml"), this.idioma);
                        loader.load();
                        loaders.put(name, loader);
                    } catch (Exception ex) {
                        loader = null;
                        java.util.logging.Logger.getLogger(FlowController.class.getName()).log(Level.SEVERE,
                                "Creando loader [" + name + "].", ex);
                    }
                }
            }
        }
        if (!name.equals("LogInView")) {
            this.LogInController = loader.getController();
        }
        return loader;
    }

    @SuppressWarnings("static-access")
    public void goMain(String viewName) {
        try {
            this.mainStage.setScene(
                    new Scene(FXMLLoader.load(App.class.getResource("view/" + viewName + ".fxml"), this.idioma)));
            MFXThemeManager.addOn(this.mainStage.getScene(), Themes.DEFAULT, Themes.LEGACY);
            this.mainStage.show();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FlowController.class.getName()).log(Level.SEVERE,
                    "Error inicializando la vista base.", ex);
        }
    }

    public void goView(String viewName) {
        goView(viewName, "Center", null);
    }

    public void goView(String viewName, String accion) {
        goView(viewName, "Center", accion);
    }

    @SuppressWarnings("static-access")
    public void goView(String viewName, String location, String accion) {
        FXMLLoader loader = getLoader(viewName);
        Controller controller = loader.getController();
        controller.setAccion(accion);
        controller.initialize();

        // Obtener el Stage
        Stage stage = controller.getStage();
        if (stage == null) {
            stage = this.mainStage;
            controller.setStage(stage);
        }

        // Manejo de ubicación dentro del BorderPane
        switch (location) {
            case "Center":
                // Obtener el VBox del centro
                VBox vBox = ((VBox) ((BorderPane) stage.getScene().getRoot()).getCenter());
                vBox.getChildren().clear();

                // Cargar la nueva vista
                Parent loadedView = loader.getRoot();

                // Asegurar el crecimiento dinámico
                VBox.setVgrow(loadedView, Priority.ALWAYS);

                // Agregar un margen de 20 píxeles al contenido cargado
                VBox.setMargin(loadedView, new Insets(20));

                // Agregar la vista cargada al VBox
                vBox.getChildren().add(loadedView);
                break;
            case "Top":
                // Agregar lógica para la ubicación superior si es necesario
                break;
            case "Bottom":
                // Agregar lógica para la ubicación inferior si es necesario
                break;
            case "Right":
                // Agregar lógica para la ubicación derecha si es necesario
                break;
            case "Left":
                // Agregar lógica para la ubicación izquierda si es necesario
                break;
            default:
                System.err.println("Ubicación no válida: " + location);
                break;
        }
    }

    public void goViewInStage(String viewName, Stage stage) {
        FXMLLoader loader = getLoader(viewName);
        Controller controller = loader.getController();
        controller.setStage(stage);
        stage.getScene().setRoot(loader.getRoot());
        MFXThemeManager.addOn(stage.getScene(), Themes.DEFAULT, Themes.LEGACY);

    }

    public void goViewInWindow(String viewName) {
        FXMLLoader loader = getLoader(viewName);
        Controller controller = loader.getController();
        controller.initialize();
        Stage stage = new Stage();
        //stage.getIcons().add(new Image("cr/ac/una/unaplanilla/resources/LogoUNArojo.png"));
        stage.setOnHidden((WindowEvent event) -> {
            controller.getStage().getScene().setRoot(new Pane());
            controller.setStage(null);
        });
        controller.setStage(stage);
        Parent root = loader.getRoot();
        Scene scene = new Scene(root);
        MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public void goLoginWindowModal(Boolean rezisable) {
        goViewInWindowModal("LogInView", this.LogInController.getStage(), rezisable);
    }

    public void goViewInWindowModal(String viewName, Stage parentStage, Boolean resizable) {
        FXMLLoader loader = getLoader(viewName);
        Controller controller = loader.getController();
        controller.initialize();
        Stage stage = new Stage();
        //stage.getIcons().add(new Image("cr/ac/una/unaplanilla/resources/LogoUNArojo.png"));
        stage.setResizable(resizable);
        stage.setOnHidden((WindowEvent event) -> {
            controller.getStage().getScene().setRoot(new Pane());
            controller.setStage(null);
        });
        controller.setStage(stage);
        Parent root = loader.getRoot();
        Scene scene = new Scene(root);
        MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.centerOnScreen();
        stage.showAndWait();

    }

    public Controller getController(String viewName) {
        return getLoader(viewName).getController();
    }

    @SuppressWarnings("static-access")
    public void limpiarLoader(String view) {
        this.loaders.remove(view);
    }

    public static void setIdioma(ResourceBundle idioma) {
        FlowController.idioma = idioma;
    }

    @SuppressWarnings("static-access")
    public void initialize() {
        this.loaders.clear();
    }

    @SuppressWarnings("static-access")
    public void salir() {
        this.mainStage.close();
    }

}
