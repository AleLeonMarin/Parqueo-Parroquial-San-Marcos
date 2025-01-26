package parqueo.san.marcos.system.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import io.github.palexdev.materialfx.controls.MFXTextField;

import java.awt.print.PrinterJob;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.AppContext;
import parqueo.san.marcos.system.util.FlowController;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

public class InitController extends Controller implements Initializable {
    @FXML
    private Label lblAvailable;

    @FXML
    private Label lblMoney;

    @FXML
    private Label lblParked;

    @FXML
    private MFXTextField txfFilter;

    @FXML
    private TableView<VehiclesDto> tbvVehicules;

    VehiclesService vehiclesService;

    private long totalTax = 0; // Suma de impuestos actual

    private ScheduledExecutorService scheduler;

    private ObservableList<VehiclesDto> vehiclesData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        vehiclesService = new VehiclesService();
        populateTableView();

    }

    private void updateAvailableSpaces() {
        try {
            Respuesta respuesta = vehiclesService.getAvailableSpaces();

            if (!respuesta.getEstado()) {
                // Mostrar mensaje de error en el log
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, respuesta.getMensaje());
                lblAvailable.setText("Error");
                return;
            }

            // Obtener el dato de los espacios disponibles y mostrarlo en el Label
            Object availableSpacesObj = respuesta.getResultado("AvailableSpaces");

            if (availableSpacesObj instanceof Integer) {
                int availableSpaces = (int) availableSpacesObj;
                lblAvailable.setText(String.valueOf(availableSpaces));
            } else {
                lblAvailable.setText("N/A"); // Manejo en caso de que el resultado no sea válido
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error actualizando espacios disponibles.",
                    e);
            lblAvailable.setText("Error");
        }
    }

    private void updateParkedVehicles() {
        try {
            Respuesta respuesta = vehiclesService.getParkedVehiclesCount();

            if (!respuesta.getEstado()) {
                // Mostrar mensaje de error en el log
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, respuesta.getMensaje());
                lblParked.setText("Error");
                return;
            }

            // Obtener el dato de vehículos estacionados y mostrarlo en el Label
            Object parkedVehiclesObj = respuesta.getResultado("ParkedVehiclesCount");

            if (parkedVehiclesObj instanceof Integer) {
                int parkedVehicles = (int) parkedVehiclesObj;
                lblParked.setText(String.valueOf(parkedVehicles));
            } else {
                lblParked.setText("N/A"); // Manejo en caso de que el resultado no sea válido
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error actualizando vehículos estacionados.",
                    e);
            lblParked.setText("Error");
        }
    }

    private void scheduleMidnightReset() {
        scheduler = Executors.newScheduledThreadPool(1);
        long delay = calculateTimeUntilMidnight();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Actualizando y reiniciando la cuenta de dinero a 0.");
            updateTotalTax(); // Sumar los totales correspondientes al día actual
            totalTax = 0; // Reiniciar la cuenta
            lblMoney.setText("0");
        }, delay, 24, TimeUnit.HOURS); // Ejecutar cada 24 horas
    }

    private long calculateTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return java.time.Duration.between(now, midnight).getSeconds();
    }

    private void updateTotalTax() {
        try {
            Respuesta respuesta = vehiclesService.getSumOfTaxForStateS();

            if (!respuesta.getEstado()) {
                lblMoney.setText("Error");
                return;
            }

            Object totalTaxObj = respuesta.getResultado("TotalTax");
            if (totalTaxObj instanceof Double) {
                // Redondear y convertir a entero
                totalTax = Math.round((Double) totalTaxObj);
                lblMoney.setText(String.valueOf(totalTax)); // Convertir a String
            } else {
                lblMoney.setText("N/A");
            }
        } catch (Exception e) {
            lblMoney.setText("Error");
        }
    }

    @Override
    public void finalize() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void populateTableView() {
        TableColumn<VehiclesDto, String> colPlate = new TableColumn<>("Placa");
        colPlate.setCellValueFactory(vehicle -> vehicle.getValue().plate);

        TableColumn<VehiclesDto, String> colReference = new TableColumn<>("Referencia");
        colReference.setCellValueFactory(vehicle -> vehicle.getValue().reference);
        colReference.setPrefWidth(200);

        TableColumn<VehiclesDto, String> colOwner = new TableColumn<>("Propietario");
        colOwner.setCellValueFactory(vehicle -> vehicle.getValue().owner);
        colOwner.setPrefWidth(200);

        TableColumn<VehiclesDto, LocalDateTime> colDate = new TableColumn<>("Fecha de ingreso");
        colDate.setCellValueFactory(vehicle -> new SimpleObjectProperty<>(vehicle.getValue().getIngress()));
        colDate.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? " " : date.format(formatter));
            }
        });
        colDate.setPrefWidth(200);

        TableColumn<VehiclesDto, Void> actionColumn = new TableColumn<>("Eliminar");
        actionColumn.setCellFactory(col -> new ButtonCell(tbvVehicules, vehiclesService, this::updateCounts));
        actionColumn.setPrefWidth(200);

        TableColumn<VehiclesDto, Void> simpleActionColumn = new TableColumn<>("Dar salida");
        simpleActionColumn.setCellFactory(col -> new SimpleButtonCell(() -> {
            VehiclesDto selectedVehicle = tbvVehicules.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                System.out.println("Seleccionando vehículo para dar egreso: " + selectedVehicle);
            }
        }));
        simpleActionColumn.setPrefWidth(200);

        TableColumn<VehiclesDto, Void> colReprint = new TableColumn<>("Reimpresión");
        colReprint.setCellFactory(col -> new PrintButtonCell());
        colReprint.setPrefWidth(200);

        tbvVehicules.setStyle("-fx-font-size: 18px; -fx-font-family: Arial;");
        colPlate.setStyle("-fx-alignment: CENTER; -fx-font-size: 18px;");
        colReference.setStyle("-fx-alignment: CENTER; -fx-font-size: 18px;");
        colOwner.setStyle("-fx-alignment: CENTER; -fx-font-size: 18px;");
        colDate.setStyle("-fx-alignment: CENTER; -fx-font-size: 18px;");

        // Agregar todas las columnas a la tabla
        tbvVehicules.getColumns().setAll(Arrays.asList(colPlate, colReference, colOwner, colDate, actionColumn,
                simpleActionColumn, colReprint));

        // Ordenar automáticamente por fecha de ingreso (más reciente primero)
        tbvVehicules.setItems(vehiclesData.sorted((v1, v2) -> v2.getIngress().compareTo(v1.getIngress())));

        tbvVehicules.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
    }

    private void updateCounts() {
        updateAvailableSpaces(); // Actualiza los espacios disponibles
        updateParkedVehicles(); // Actualiza la cantidad de vehículos estacionados
    }

    private void loadParkedVehicles() {
        try {
            Respuesta respuesta = vehiclesService.getVehiclesParked();
            if (!respuesta.getEstado()) {
                new Mensaje().showModal(Alert.AlertType.ERROR, "Cargar Vehículos", getStage(), respuesta.getMensaje());
                return;
            }
            @SuppressWarnings("unchecked")
            List<VehiclesDto> vehiclesDtoList = (List<VehiclesDto>) respuesta.getResultado("Vehicles");

            vehiclesData.clear();
            if (vehiclesDtoList != null && !vehiclesDtoList.isEmpty()) {
                vehiclesData.addAll(vehiclesDtoList);
            }

            applyFilter(); // Aplica el filtro a la tabla
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error cargando vehículos estacionados.", e);
        }
    }

    private void applyFilter() {
        // Envuelve la lista observable en una lista filtrada
        FilteredList<VehiclesDto> filteredData = new FilteredList<>(vehiclesData, p -> true);

        // Escucha los cambios en el TextField
        txfFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(vehicle -> {
                // Si el filtro está vacío, mostrar todos los elementos
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Filtrar por placa, referencia o propietario
                if (vehicle.getPlate().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Coincide con placa
                } else if (vehicle.getReference().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Coincide con referencia
                } else if (vehicle.getOwner().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Coincide con propietario
                }

                return false; // No coincide
            });
        });

        // Envolver en una SortedList para mantener orden si es necesario
        SortedList<VehiclesDto> sortedData = new SortedList<>(filteredData);

        // Enlazar el orden de la tabla a la lista ordenada
        sortedData.comparatorProperty().bind(tbvVehicules.comparatorProperty());

        // Aplicar los datos filtrados a la tabla
        tbvVehicules.setItems(sortedData);
    }

    @Override
    public void initialize() {

        updateAvailableSpaces();
        updateParkedVehicles();
        updateTotalTax();
        scheduleMidnightReset();
        loadParkedVehicles();
        applyFilter();
        txfFilter.requestFocus();
        txfFilter.clear();

    }

    public class ButtonCell extends TableCell<VehiclesDto, Void> {

        private final Button deleteButton;
        @SuppressWarnings("unused")
        private final VehiclesService vehiclesService;
        @SuppressWarnings("unused")
        private final Runnable updateCounts; // Acción para actualizar contadores

        public ButtonCell(TableView<VehiclesDto> tableView, VehiclesService vehiclesService, Runnable updateCounts) {
            this.vehiclesService = vehiclesService;
            this.updateCounts = updateCounts;

            deleteButton = new Button("Eliminar");
            deleteButton.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: red; " +
                            "-fx-border-color: red; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px;" +
                            "-fx-cursor: hand;" // Cambia el cursor al estilo "mano"
            );

            deleteButton.setOnAction(event -> {
                if (new Mensaje().showConfirmation("Eliminar Vehículo", getStage(),
                        "¿Está seguro de eliminar el vehículo?")) {

                    int currentIndex = getIndex();
                    if (currentIndex >= 0 && currentIndex < tableView.getItems().size()) {
                        VehiclesDto item = tableView.getItems().get(currentIndex);

                        // Llamar al método deleteVehicle del servicio
                        Respuesta respuesta = vehiclesService.deleteVehicle(item.getId());
                        if (respuesta.getEstado()) {
                            // Asegurarse de usar una lista modificable
                            ObservableList<VehiclesDto> modifiableList = FXCollections
                                    .observableArrayList(tableView.getItems());
                            modifiableList.remove(item); // Eliminar de la lista
                            tableView.setItems(modifiableList); // Actualizar el TableView

                            System.out.println("Vehículo eliminado correctamente.");

                            // Actualizar contadores de vehículos estacionados y espacios disponibles
                            updateCounts.run();
                        } else {
                            new Mensaje().showModal(Alert.AlertType.ERROR, "Eliminar Vehículo", getStage(),
                                    respuesta.getMensaje());
                        }
                    }
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(deleteButton);
            }
        }
    }

    public class SimpleButtonCell extends TableCell<VehiclesDto, Void> {

        private final Button actionButton;

        public SimpleButtonCell(Runnable action) {
            actionButton = new Button("Dar salida");
            actionButton.setStyle(
                    "-fx-background-color: white; " + // Fondo blanco
                            "-fx-text-fill: #006400; " + // Texto verde oscuro
                            "-fx-border-color: #006400; " + // Borde verde oscuro
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;" // Cambia el cursor al estilo "mano"
            );

            actionButton.setOnAction(event -> {
                int currentIndex = getIndex();
                if (currentIndex >= 0 && currentIndex < getTableView().getItems().size()) {
                    VehiclesDto selectedVehicle = getTableView().getItems().get(currentIndex); // Obtén el vehículo
                                                                                               // directamente
                    System.out.println("Seleccionando vehículo para dar egreso: " + selectedVehicle);
                    AppContext.getInstance().set("vehicle", selectedVehicle);
                    FlowController.getInstance().goViewInWindowModal("EgressView", getStage(), true);
                    loadParkedVehicles();
                    updateAvailableSpaces();
                    updateParkedVehicles();
                    updateTotalTax();
                    tbvVehicules.refresh();
                } else {
                    System.err.println("No se seleccionó ningún vehículo.");
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(actionButton);
            }
        }
    }

    public class PrintButtonCell extends TableCell<VehiclesDto, Void> {

        private final Button actionButton;

        public PrintButtonCell() {
            actionButton = new Button("Reimpresión");
            actionButton.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: #006400; " +
                            "-fx-border-color: #006400; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;");

            actionButton.setOnAction(event -> {
                int currentIndex = getIndex();
                if (currentIndex >= 0 && currentIndex < getTableView().getItems().size()) {
                    VehiclesDto selectedVehicle = getTableView().getItems().get(currentIndex);

                    // Buscar vehículo en la base de datos por la placa
                    VehiclesService vehiclesService = new VehiclesService();
                    Respuesta respuesta = vehiclesService.getVehicleForReprint(selectedVehicle.getPlate());

                    if (respuesta.getEstado()) {
                        VehiclesDto vehicle = (VehiclesDto) respuesta.getResultado("Vehicle");
                        System.out.println("Reimprimiendo tiquete para: " + vehicle);

                        // Llamar al método para generar e imprimir el tiquete
                        IngressTicket(
                                vehicle.getOwner(),
                                vehicle.getReference(),
                                vehicle.getPlate(),
                                vehicle.getIngress().toString(),
                                "/parqueo/san/marcos/system/resources/receipts.png" // Ruta de la imagen
                        );
                    } else {
                        System.err.println("Error al obtener el vehículo: " + respuesta.getMensaje());
                    }
                } else {
                    System.err.println("No se seleccionó ningún vehículo.");
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(actionButton);
            }
        }
    }

    public static void IngressTicket(String owner, String reference, String plate, String ingressDate,
            String imagePath) {
        float widthInMm = 58; // Ancho del ticket
        float heightInMm = 150; // Alto del ticket
        float widthInPoints = widthInMm * 2.83465f; // Convertir a puntos
        float heightInPoints = heightInMm * 2.83465f;

        // Crear documento y página con tamaño exacto
        PDRectangle customSize = new PDRectangle(widthInPoints, heightInPoints);
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(customSize);
        page.setMediaBox(customSize); // Asegurar sin márgenes
        page.setCropBox(customSize);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float centerX = widthInPoints / 2; // Centrar horizontalmente
            float yOffset = heightInPoints - 10; // Comenzar pegado arriba

            // Encabezado
            drawCenteredText(contentStream, "Parqueo Parroquial", PDType1Font.HELVETICA_BOLD, 10, centerX, yOffset);
            yOffset -= 12;
            drawCenteredText(contentStream, "San Marcos", PDType1Font.HELVETICA_BOLD, 10, centerX, yOffset);
            yOffset -= 15;

            // Imagen
            InputStream imageStream = InitController.class.getResourceAsStream(imagePath);
            if (imageStream != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageStream.readAllBytes(),
                        "logo");
                contentStream.drawImage(pdImage, centerX - 15, yOffset - 20, 30, 30);
                yOffset -= 40;
            }

            // Información del negocio
            String[] businessInfo = { "Genesis Brenes Calvo", "Cédula: 115810605", "Teléfono: 8449-6373" };
            for (String line : businessInfo) {
                drawCenteredText(contentStream, line, PDType1Font.HELVETICA, 8, centerX, yOffset);
                yOffset -= 10;
            }

            // Título y detalles
            yOffset -= 10;
            drawCenteredText(contentStream, "Tiquete de Entrada", PDType1Font.HELVETICA_BOLD, 10, centerX, yOffset);
            yOffset -= 12;

            String[] details = { "Nombre", owner, "Referencia", reference, "Número de Placa", plate, "Ingreso",
                    formatIngressDate(ingressDate) };
            for (int i = 0; i < details.length; i += 2) {
                drawCenteredText(contentStream, details[i], PDType1Font.HELVETICA_BOLD, 8, centerX, yOffset);
                yOffset -= 10;
                drawCenteredText(contentStream, details[i + 1], PDType1Font.HELVETICA, 8, centerX, yOffset);
                yOffset -= 12;
            }

            // Footer
            yOffset -= 5; // Espacio mínimo antes del footer
            drawCenteredText(contentStream, "¡Gracias por preferirnos!", PDType1Font.HELVETICA_OBLIQUE, 7, centerX,
                    yOffset);
            yOffset -= 10;
            drawCenteredText(contentStream, "De lunes a viernes", PDType1Font.HELVETICA_OBLIQUE, 7, centerX, yOffset);
            yOffset -= 10;
            drawCenteredText(contentStream, "8:00 am a 6:00 pm", PDType1Font.HELVETICA_OBLIQUE, 7, centerX, yOffset);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Guardar PDF
        String fileName = "Tiquete_Entrada_58x150.pdf";
        File pdfFile = new File(fileName);
        try {
            document.save(pdfFile);
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Impresión automática
        printPDF(pdfFile);
    }

    private static void printPDF(File pdfFile) {
        PDDocument document = null;
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService != null) {
                document = PDDocument.load(pdfFile);
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintService(printService);

                // Imprimir en tamaño real
                job.setPrintable(new PDFPrintable(document, Scaling.ACTUAL_SIZE));

                System.out.println("Enviando trabajo de impresión...");
                job.print();
                System.out.println("Impresión finalizada correctamente.");
            } else {
                System.out.println("No se encontró una impresora predeterminada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Método auxiliar para centrar texto
    private static void drawCenteredText(PDPageContentStream contentStream, String text, PDType1Font font, int fontSize,
            float centerX, float yOffset) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(centerX - textWidth / 2, yOffset);
        contentStream.showText(text);
        contentStream.endText();
    }

    // Método para formatear fecha
    private static String formatIngressDate(String ingressDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Date date = inputFormat.parse(ingressDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return ingressDate;
        }
    }

}
