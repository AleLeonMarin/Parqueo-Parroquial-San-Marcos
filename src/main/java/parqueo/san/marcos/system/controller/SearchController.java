package parqueo.san.marcos.system.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

import java.util.Arrays;
import java.util.List;

public class SearchController extends Controller implements Initializable {

    @FXML
    private MFXButton btnClean;

    @FXML
    private MFXButton btnFilter;

    @FXML
    private MFXButton btnLoad;

    @FXML
    private MFXDatePicker dpDate;

    @FXML
    private TableView<VehiclesDto> tbvResult;

    @FXML
    private MFXTextField txfOwner;

    @FXML
    private MFXTextField txfPlate;

    @FXML
    private MFXTextField txfReference;

    Object result;

    VehiclesService vehiclesService;

    ObservableList<VehiclesDto> vehicles;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vehiclesService = new VehiclesService();
        vehicles = FXCollections.observableArrayList();
        populateFields();

        tbvResult.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Detecta doble clic
                VehiclesDto selectedItem = tbvResult.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    result = selectedItem;
                    this.getStage().close(); // Cierra la ventana automáticamente
                }
            }
        });
    }

    @FXML
    void onActionBtnClean(ActionEvent event) {
        txfOwner.clear();
        txfPlate.clear();
        txfReference.clear();
        dpDate.setValue(LocalDate.now());
    }

    @FXML
    void onActionBtnFilter(ActionEvent event) {

        filter(txfOwner.getText(), txfPlate.getText(), txfReference.getText(), dpDate.getValue());

    }

    @FXML
    void onActionBtnLoad(ActionEvent event) {

        result = tbvResult.getSelectionModel().getSelectedItem();
        this.getStage().close();

    }

    private void populateFields() {
        // Columna de Propietario
        TableColumn<VehiclesDto, String> colOwner = new TableColumn<>("Propietario");
        colOwner.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOwner()));

        // Columna de Placa
        TableColumn<VehiclesDto, String> colPlate = new TableColumn<>("Placa");
        colPlate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPlate()));

        // Columna de Referencia
        TableColumn<VehiclesDto, String> colReference = new TableColumn<>("Referencia");
        colReference.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getReference()));

        // Columna de Fecha de Ingreso
        TableColumn<VehiclesDto, LocalDate> colDate = new TableColumn<>("Fecha de Ingreso");
        colDate.setCellValueFactory(cd -> {
            LocalDateTime ingress = cd.getValue().getIngress();
            return ingress != null ? new SimpleObjectProperty<>(ingress.toLocalDate()) : null;
        });

        // Agregar las columnas a la tabla
        tbvResult.getColumns().clear();
        tbvResult.getColumns().addAll(Arrays.asList(colOwner, colPlate, colReference, colDate));
    }

    @SuppressWarnings("unchecked")
    private void filter(String owner, String plate, String reference, LocalDate date) {
        try {
            Respuesta response = vehiclesService.getAllVehicles();

            if (response.getEstado()) {
                // Obtener la lista inicial de vehículos con estado "P"
                ObservableList<VehiclesDto> dto = FXCollections.observableArrayList(
                        ((List<VehiclesDto>) response.getResultado("Vehicles"))
                                .stream()
                                .filter(v -> "P".equalsIgnoreCase(v.getStatus())) // Filtrar solo estado "P"
                                .toList());

                vehicles.clear(); // Limpiar la lista actual

                // Aplicar filtros condicionalmente
                if (owner != null && !owner.isEmpty()) {
                    String ownerSearched = owner.toLowerCase();
                    dto = dto.filtered(v -> v.getOwner().toLowerCase().contains(ownerSearched));
                }

                if (plate != null && !plate.isEmpty()) {
                    String plateSearched = plate.toLowerCase();
                    dto = dto.filtered(v -> v.getPlate().toLowerCase().contains(plateSearched));
                }

                if (reference != null && !reference.isEmpty()) {
                    String referenceSearched = reference.toLowerCase();
                    dto = dto.filtered(v -> v.getReference().toLowerCase().contains(referenceSearched));
                }

                if (date != null) {
                    dto = dto.filtered(v -> {
                        LocalDate vehicleDate = v.getIngress().toLocalDate();
                        return vehicleDate.equals(date);
                    });
                }

                // Actualizar la tabla con los resultados filtrados
                vehicles.addAll(dto);
                tbvResult.setItems(vehicles);
                tbvResult.refresh();

            } else {
                // Mostrar mensaje de error en caso de falla del servicio
                new Mensaje().showModal(AlertType.ERROR, "Error al cargar los vehículos", this.getStage(),
                        response.getMensaje());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Object getResult() {
        return result;
    }

    @Override
    public void initialize() {

    }

}
