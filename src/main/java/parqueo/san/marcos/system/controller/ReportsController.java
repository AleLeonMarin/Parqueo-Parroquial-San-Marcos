package parqueo.san.marcos.system.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

public class ReportsController extends Controller implements Initializable {

    @FXML
    private MFXButton btnExcel;

    @FXML
    private MFXButton btnPdf;

    @FXML
    private MFXDatePicker dpFinalDate;

    @FXML
    private MFXDatePicker dpInitialDate;

    @FXML
    private TableView<VehiclesDto> tbvAbstract;

    @FXML
    private Label lblMoney;

    @FXML
    private Label lblParked;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateTableView();

    }

    private void populateTableView() {
        TableColumn<VehiclesDto, String> colPlate = new TableColumn<>("Placa");
        colPlate.setCellValueFactory(cd -> cd.getValue().plate);

        TableColumn<VehiclesDto, String> colOwner = new TableColumn<>("Propietario");
        colOwner.setCellValueFactory(cd -> cd.getValue().owner);

        TableColumn<VehiclesDto, String> colReference = new TableColumn<>("Referencia");
        colReference.setCellValueFactory(cd -> cd.getValue().reference);

        TableColumn<VehiclesDto, String> colEntryDate = new TableColumn<>("Fecha de ingreso");
        colEntryDate.setCellValueFactory(vehicle -> {
            if (vehicle.getValue().getIngress() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                return new SimpleStringProperty(vehicle.getValue().getIngress().format(formatter));
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        colEntryDate.setPrefWidth(200);

        TableColumn<VehiclesDto, String> colExitDate = new TableColumn<>("Fecha de salida");
        colExitDate.setCellValueFactory(vehicle -> {
            if (vehicle.getValue().getEgress() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                return new SimpleStringProperty(vehicle.getValue().getEgress().format(formatter));
            } else {
                return new SimpleStringProperty("N/A");
            }
        });
        colExitDate.setPrefWidth(200);

        TableColumn<VehiclesDto, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cd -> cd.getValue().tax);

        colTotal.setPrefWidth(120);

        tbvAbstract.getColumns()
                .addAll(Arrays.asList(colPlate, colOwner, colReference, colEntryDate, colExitDate, colTotal));
    }

    @FXML
    void onActionBtnExcel(ActionEvent event) {
        try {
            // Crear el libro de Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Reporte de Vehículos");

            // Crear estilos para el encabezado y celdas
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle columnHeaderStyle = workbook.createCellStyle();
            Font columnFont = workbook.createFont();
            columnFont.setBold(true);
            columnHeaderStyle.setFont(columnFont);
            columnHeaderStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Fila del título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Parqueo Parroquial San Marcos");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5)); // Combina las celdas

            // Fila de fechas
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            LocalDate startDate = dpInitialDate.getValue();
            LocalDate endDate = dpFinalDate.getValue();
            dateCell.setCellValue("Fechas: " + (startDate != null ? startDate : "N/A") + " - "
                    + (endDate != null ? endDate : "N/A"));
            dateCell.setCellStyle(cellStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5)); // Combina las celdas

            // Nombres de columnas
            Row headerRow = sheet.createRow(3);
            String[] columns = { "Placa", "Propietario", "Referencia", "Fecha de ingreso", "Fecha de salida", "Total" };
            for (int i = 0; i < columns.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columns[i]);
                headerCell.setCellStyle(columnHeaderStyle);
            }

            // Agregar datos de la tabla
            int rowNum = 4;
            double totalSum = 0; // Inicializa el total en 0

            // Itera sobre los vehículos
            for (VehiclesDto vehicle : tbvAbstract.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vehicle.getPlate());
                row.createCell(1).setCellValue(vehicle.getOwner());
                row.createCell(2).setCellValue(vehicle.getReference());
                row.createCell(3).setCellValue(vehicle.getIngress() != null
                        ? vehicle.getIngress().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        : "N/A");
                row.createCell(4).setCellValue(vehicle.getEgress() != null
                        ? vehicle.getEgress().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        : "N/A");

                // Validación y suma del total
                double tax = 0;
                try {
                    if (vehicle.getTax() != null && !vehicle.getTax().isBlank()) {
                        tax = Double.parseDouble(vehicle.getTax());
                        System.out.println("Tax válido encontrado: " + tax); // Depuración
                    } else {
                        System.out.println("Tax inválido o vacío para vehículo: " + vehicle.getPlate());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error al convertir el tax del vehículo con placa " + vehicle.getPlate());
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                            "Error al convertir tax del vehículo con placa: " + vehicle.getPlate(), e);
                }

                row.createCell(5).setCellValue(tax); // Escribe el valor de tax en el Excel
                totalSum += tax; // Suma el valor de tax al total
            }

            // Fila del total
            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabelCell = totalRow.createCell(4);
            totalLabelCell.setCellValue("Total:");
            totalLabelCell.setCellStyle(columnHeaderStyle);

            Cell totalValueCell = totalRow.createCell(5);
            totalValueCell.setCellValue(totalSum);
            totalValueCell.setCellStyle(columnHeaderStyle);

            // Autoajustar columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar el archivo
            String fileName = "Reporte_Vehiculos.xlsx";
            File file = new File(fileName);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            workbook.close();

            // Abrir el archivo automáticamente
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

            new Mensaje().showModal(Alert.AlertType.INFORMATION, "Exportar Excel", getStage(),
                    "El archivo Excel se ha generado y abierto correctamente: " + fileName);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error exportando a Excel.", e);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Exportar Excel", getStage(),
                    "Ocurrió un error al exportar el archivo Excel.");
        }
    }

    @FXML
    void onActionBtnPdf(ActionEvent event) {
        try {
            // Crear el documento PDF
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4); // A4 en orientación vertical
            document.addPage(page);

            // Configuración del contenido
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Encabezado
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(200, 800); // Posición del texto (centrado en ancho)
            contentStream.showText("Parqueo Parroquial San Marcos");
            contentStream.endText();

            // Fechas de búsqueda
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 770); // Posición debajo del título
            LocalDate startDate = dpInitialDate.getValue();
            LocalDate endDate = dpFinalDate.getValue();
            contentStream.showText(
                    "Fechas: " + (startDate != null ? startDate : "N/A") + " - " + (endDate != null ? endDate : "N/A"));
            contentStream.endText();

            // Nombres de columnas
            String[] columns = { "Placa", "Propietario", "Referencia", "Fecha de ingreso", "Fecha de salida", "Total" };
            int startX = 50; // Posición inicial en X
            int startY = 730; // Posición inicial en Y
            int columnSpacing = 85; // Espaciado entre columnas

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            for (int i = 0; i < columns.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(startX + (i * columnSpacing), startY);
                contentStream.showText(columns[i]);
                contentStream.endText();
            }

            // Datos de la tabla
            int rowY = startY - 20; // Espaciado entre filas
            double totalSum = 0;
            contentStream.setFont(PDType1Font.HELVETICA, 10);

            for (VehiclesDto vehicle : tbvAbstract.getItems()) {
                double tax = 0;
                try {
                    if (vehicle.getTax() != null && !vehicle.getTax().isBlank()) {
                        tax = Double.parseDouble(vehicle.getTax());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error al convertir el tax del vehículo con placa: " + vehicle.getPlate());
                }

                String[] rowData = {
                        safeText(vehicle.getPlate()),
                        safeText(vehicle.getOwner()),
                        safeText(vehicle.getReference()),
                        vehicle.getIngress() != null
                                ? vehicle.getIngress().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                                : "N/A",
                        vehicle.getEgress() != null
                                ? vehicle.getEgress().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                                : "N/A",
                        String.valueOf(tax)
                };

                for (int i = 0; i < rowData.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(startX + (i * columnSpacing), rowY);
                    contentStream.showText(rowData[i]);
                    contentStream.endText();
                }

                totalSum += tax;
                rowY -= 15; // Espaciado entre filas
            }

            // Total
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(350, rowY - 20);
            contentStream.showText("Total:");
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(450, rowY - 20);
            contentStream.showText(String.valueOf(totalSum));
            contentStream.endText();

            contentStream.close();

            // Guardar el archivo
            String fileName = "Reporte_Vehiculos.pdf";
            File file = new File(fileName);
            document.save(file);
            document.close();

            // Abrir el archivo automáticamente
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

            new Mensaje().showModal(Alert.AlertType.INFORMATION, "Exportar PDF", getStage(),
                    "El archivo PDF se ha generado y abierto correctamente: " + fileName);
        } catch (IOException e) {
            System.err.println("Error al generar el archivo PDF: " + e.getMessage());
            e.printStackTrace();
            new Mensaje().showModal(Alert.AlertType.ERROR, "Exportar PDF", getStage(),
                    "Ocurrió un error al exportar el archivo PDF.");
        }
    }

    // Método auxiliar para manejar valores nulos
    private String safeText(String text) {
        return text != null ? text : "N/A";
    }

    private void loadVehiclesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return;
        }

        try {
            VehiclesService vehiclesService = new VehiclesService();
            Respuesta respuesta = vehiclesService.getVehiclesByDateRange(startDate, endDate);

            if (!respuesta.getEstado()) {
                new Mensaje().showModal(Alert.AlertType.ERROR, "Cargar Vehículos", getStage(), respuesta.getMensaje());
                return;
            }

            @SuppressWarnings("unchecked")
            List<VehiclesDto> vehiclesList = (List<VehiclesDto>) respuesta.getResultado("Vehicles");

            tbvAbstract.getItems().clear();
            tbvAbstract.getItems().addAll(vehiclesList);

            updateLabels(); // Actualizar las etiquetas después de cargar los datos
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error cargando vehículos en el rango.", e);
        }
    }

    private void updateLabels() {
        double totalMoney = 0;
        int totalVehicles = tbvAbstract.getItems().size();

        // Iterar sobre los elementos de la tabla y calcular la suma del dinero
        for (VehiclesDto vehicle : tbvAbstract.getItems()) {
            try {
                if (vehicle.getTax() != null && !vehicle.getTax().isBlank()) {
                    totalMoney += Double.parseDouble(vehicle.getTax());
                }
            } catch (NumberFormatException e) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                        "Error al convertir tax del vehículo con placa: " + vehicle.getPlate(), e);
            }
        }

        // Redondear totalMoney a un número entero
        int roundedTotalMoney = (int) Math.round(totalMoney);

        // Actualizar las etiquetas
        lblMoney.setText(String.format("₡%d", roundedTotalMoney));
        lblParked.setText(String.valueOf(totalVehicles));
    }

    @FXML
    void onActiondpFinalDate(ActionEvent event) {
        LocalDate startDate = dpInitialDate.getValue();
        LocalDate endDate = dpFinalDate.getValue();

        if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
            loadVehiclesByDateRange(startDate, endDate);
        } else {
            new Mensaje().showModal(Alert.AlertType.WARNING, "Rango de Fechas", getStage(),
                    "La fecha final debe ser posterior o igual a la fecha inicial.");
        }
    }

    @Override
    public void initialize() {
        tbvAbstract.getItems().clear();
        dpInitialDate.setValue(LocalDate.now()); // Fecha inicial predeterminada
        dpFinalDate.setValue(null); // Sin fecha final predeterminada
        updateLabels();

        dpFinalDate.setOnAction(this::onActiondpFinalDate); // Conectar el evento al listener
    }

}
