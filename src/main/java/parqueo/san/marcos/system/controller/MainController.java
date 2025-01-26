package parqueo.san.marcos.system.controller;

import java.net.URL;
import java.util.ResourceBundle;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import parqueo.san.marcos.system.util.FlowController;

public class MainController extends Controller implements Initializable {

    @FXML
    private MFXButton bntExit;

    @FXML
    private MFXButton btnIngressEgress;

    @FXML
    private MFXButton btnParameters;

    @FXML
    private MFXButton btnReports;

    @FXML
    private Label lblTitle;

    @FXML
    private BorderPane root;

    @FXML
    private MFXButton bntEgress;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Platform.runLater(() -> FlowController.getInstance().goView("InitView"));

    }

    @FXML
    void onActionBtnExit(ActionEvent event) {
        FlowController.getInstance().salir();
    }

    @FXML
    void onActionBtnIngressEgress(ActionEvent event) {

        FlowController.getInstance().goView("IngressView");

    }

    @FXML
    void onActionBtnParameters(ActionEvent event) {
        FlowController.getInstance().goView("ParamtersView");
    }

    @FXML
    void onActionBtnReports(ActionEvent event) {

        FlowController.getInstance().goView("ReportsView");

    }

    @FXML
    void onMouseClickedLblTitle(MouseEvent event) {

        FlowController.getInstance().goView("InitView");

    }

    @FXML
    void onActionBtnEgress(ActionEvent event) {

        FlowController.getInstance().goView("EgressVehicules");
    }

    @Override
    public void initialize() {

    }

}
