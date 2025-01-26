package parqueo.san.marcos.system.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.Respuesta;

public class EgressedController extends Controller implements Initializable {

    @FXML
    private MFXTableView<VehiclesDto> tbvCars;

    @FXML
    private MFXTextField txfFilter;

    VehiclesService vehiclesService;

    private ObservableList<VehiclesDto> vehiclesData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vehiclesService = new VehiclesService();
        populateTableView();
    }

    @Override
    public void initialize() {
        loadVehiclesWithStateS();
    }

    private void populateTableView() {

        // Formato de 12 horas con AM/PM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

        MFXTableColumn<VehiclesDto> colPlate = new MFXTableColumn<>("Placa");
        colPlate.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getPlate));

        MFXTableColumn<VehiclesDto> colOwner = new MFXTableColumn<>("Propietario");
        colOwner.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getOwner));

        MFXTableColumn<VehiclesDto> colIngress = new MFXTableColumn<>("Ingreso");
        colIngress.setRowCellFactory(vehicle -> new MFXTableRowCell<>(v -> {
            if (v.getIngress() != null) {
                return v.getIngress().format(formatter); // Formato 12 horas
            } else {
                return "N/A";
            }
        }));

        colIngress.setMinWidth(150);

        MFXTableColumn<VehiclesDto> colEgress = new MFXTableColumn<>("Salida");
        colEgress.setRowCellFactory(vehicle -> new MFXTableRowCell<>(v -> {
            if (v.getEgress() != null) {
                return v.getEgress().format(formatter); // Formato 12 horas
            } else {
                return "N/A";
            }
        }));

        colEgress.setMinWidth(150);

        MFXTableColumn<VehiclesDto> colReference = new MFXTableColumn<>("Referencia");
        colReference.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getReference));

        MFXTableColumn<VehiclesDto> colTotal = new MFXTableColumn<>("Total");
        colTotal.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getTax));

        colTotal.setMinWidth(200);

        tbvCars.getTableColumns()
                .addAll(Arrays.asList(colPlate, colOwner, colIngress, colEgress, colReference, colTotal));
    }

    private void applyFilter() {
        // Escuchar cambios en el campo de texto del filtro
        txfFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filtrar datos directamente desde vehiclesData
            List<VehiclesDto> filteredVehicles = vehiclesData.stream()
                    .filter(vehicle -> {
                        // Si el filtro está vacío, mostrar todos los elementos
                        if (newValue == null || newValue.isEmpty()) {
                            return true; // Mostrar todos los datos
                        }

                        String lowerCaseFilter = newValue.toLowerCase();

                        // Filtrar por placa, referencia o propietario
                        return vehicle.getPlate().toLowerCase().contains(lowerCaseFilter)
                                || vehicle.getReference().toLowerCase().contains(lowerCaseFilter)
                                || (vehicle.getOwner() != null
                                        && vehicle.getOwner().toLowerCase().contains(lowerCaseFilter));
                    })
                    .toList();

            // Actualizar manualmente la tabla con los datos filtrados
            tbvCars.getItems().clear();
            tbvCars.getItems().addAll(filteredVehicles);

            // Depuración: Mostrar el tamaño de la lista filtrada
            System.out.println("Cantidad de elementos filtrados: " + filteredVehicles.size());
        });

        // Establecer los datos iniciales en la tabla
        tbvCars.getItems().clear();
        tbvCars.getItems().addAll(vehiclesData);
    }

    private void loadVehiclesWithStateS() {
        try {
            Respuesta response = vehiclesService.getAllVehicles();

            if (response.getEstado()) {
                @SuppressWarnings("unchecked")
                List<VehiclesDto> allVehicles = (List<VehiclesDto>) response.getResultado("Vehicles");

                // Filtrar vehículos con estado "S" y con fecha actual
                LocalDate today = LocalDate.now();
                List<VehiclesDto> filteredVehicles = allVehicles.stream()
                        .filter(vehicle -> "S".equals(vehicle.getStatus())) // Estado "S"
                        .filter(vehicle -> isSameDay(vehicle.getIngress(), today)
                                || isSameDay(vehicle.getEgress(), today)) // Fecha actual
                        .toList();

                // Cargar los datos filtrados en la lista observable
                vehiclesData.setAll(filteredVehicles);

                // Establecer los datos iniciales en la tabla
                tbvCars.getItems().clear();
                tbvCars.getItems().addAll(filteredVehicles);
                applyFilter();

            } else {
                System.err.println("Error al obtener los vehículos: " + response.getMensaje());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSameDay(LocalDateTime dateTime, LocalDate today) {
        return dateTime != null && dateTime.toLocalDate().isEqual(today);
    }

}
