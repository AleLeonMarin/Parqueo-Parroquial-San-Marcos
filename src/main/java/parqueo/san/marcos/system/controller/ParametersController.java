package parqueo.san.marcos.system.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import parqueo.san.marcos.system.model.ParametersDto;
import parqueo.san.marcos.system.service.ParametersService;
import parqueo.san.marcos.system.util.Formato;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

public class ParametersController extends Controller implements Initializable {

    @FXML
    private MFXButton btnSave;

    @FXML
    private MFXTextField txfQuantity;

    @FXML
    private MFXTextField txfValue;

    ParametersDto parameter;

    List<Node> requerieds = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txfQuantity.delegateSetTextFormatter(Formato.getInstance().integerFormat());
        txfValue.delegateSetTextFormatter(Formato.getInstance().integerFormat());
        parameter = new ParametersDto();
        addRequerieds();
    }

    private void bindParameters() {
        txfQuantity.textProperty().bindBidirectional(parameter.quantity);
        txfValue.textProperty().bindBidirectional(parameter.tax);
    }

    private void unbindParameters() {
        txfQuantity.textProperty().unbindBidirectional(parameter.quantity);
        txfValue.textProperty().unbindBidirectional(parameter.tax);
    }

    private void addRequerieds() {
        requerieds.clear();
        requerieds.add(txfQuantity);
        requerieds.add(txfValue);
    }

    @SuppressWarnings("rawtypes")
    public String validarRequeridos() {
        Boolean validos = true;
        String invalidos = "";
        for (Node node : requerieds) {
            if (node instanceof MFXTextField
                    && (((MFXTextField) node).getText() == null || ((MFXTextField) node).getText().isBlank())) {
                if (validos) {
                    invalidos += ((MFXTextField) node).getFloatingText();
                } else {
                    invalidos += "," + ((MFXTextField) node).getFloatingText();
                }
                validos = false;
            } else if (node instanceof MFXPasswordField
                    && (((MFXPasswordField) node).getText() == null || ((MFXPasswordField) node).getText().isBlank())) {
                if (validos) {
                    invalidos += ((MFXPasswordField) node).getFloatingText();
                } else {
                    invalidos += "," + ((MFXPasswordField) node).getFloatingText();
                }
                validos = false;
            } else if (node instanceof MFXDatePicker && ((MFXDatePicker) node).getValue() == null) {
                if (validos) {
                    invalidos += ((MFXDatePicker) node).getFloatingText();
                } else {
                    invalidos += "," + ((MFXDatePicker) node).getFloatingText();
                }
                validos = false;
            } else if (node instanceof MFXComboBox && ((MFXComboBox) node).getSelectionModel().getSelectedIndex() < 0) {
                if (validos) {
                    invalidos += ((MFXComboBox) node).getFloatingText();
                } else {
                    invalidos += "," + ((MFXComboBox) node).getFloatingText();
                }
                validos = false;
            }
        }
        if (validos) {
            return "";
        } else {
            return "Campos requeridos o con problemas de formato [" + invalidos + "].";
        }
    }

    @FXML
    void onActionBtnSave(ActionEvent event) {

        try {
            String validacion = validarRequeridos();
            if (!validacion.isBlank()) {
                new Mensaje().showModal(Alert.AlertType.ERROR, "Guardar Empleado", getStage(), validacion);
            } else {
                parameter.setQuantity(txfQuantity.getText());
                parameter.setTax(txfValue.getText());
                ParametersService service = new ParametersService();
                Respuesta respuesta = service.saveParameters(parameter);

                if (respuesta.getEstado()) {
                    unbindParameters();
                    this.parameter = (ParametersDto) respuesta.getResultado("Parameter");
                    bindParameters();
                    new Mensaje().showModal(Alert.AlertType.INFORMATION, "Guardar Parámetros", getStage(),
                            "Parámetros guardados correctamente.");
                } else {
                    new Mensaje().showModal(Alert.AlertType.ERROR, "Guardar Parámetros", getStage(),
                            respuesta.getMensaje());
                }

            }
        } catch (Exception ex) {
            Logger.getLogger(ParametersController.class.getName()).log(Level.SEVERE, "Error guardando los parametros.",
                    ex);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Guardar Parametros", getStage(),
                    "Ocurrio un error guardando los parametros.");
        }

    }

    private void chargeParameters() {
        ParametersService service = new ParametersService();
        Respuesta respuesta = service.getParameter();

        if (respuesta.getEstado() && respuesta.getResultado("Parameter") != null) {
            unbindParameters();
            this.parameter = (ParametersDto) respuesta.getResultado("Parameter");
            bindParameters();
        } else {
            // Muestra el error como información, pero no detiene la ejecución
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Parámetros no encontrados o vacíos.");
        }
    }

    @Override
    public void initialize() {
        chargeParameters();
    }

}
