package parqueo.san.marcos.system.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import java.awt.print.PrinterJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import parqueo.san.marcos.system.model.ParametersDto;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.ParametersService;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.Formato;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

public class IngressController extends Controller implements Initializable {

    @FXML
    private MFXButton btnClean;

    @FXML
    private MFXButton btnPrint;

    @FXML
    private MFXDatePicker dpDIngressDate;

    @FXML
    private MFXTableView<VehiclesDto> tbvRecents;

    @FXML
    private MFXTextField txfDueno;

    @FXML
    private MFXTextField txfPlaca;

    VehiclesDto vehicle;

    List<Node> requerieds = new ArrayList<>();
    ParametersDto parameters;

    @SuppressWarnings("unused")
    private static final Set<Integer> usedReferences = new HashSet<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txfPlaca.delegateSetTextFormatter(Formato.getInstance().anyCharacterFormatWithMaxLength(8));
        txfDueno.delegateSetTextFormatter(Formato.getInstance().letrasFormat(50));
        vehicle = new VehiclesDto();
        parameters = new ParametersDto();
        newVehicle();
        setupPlateSearchListener(); // Configura el listener de búsqueda de placas
        setupRecentTableSelectionListener();
        populateRecentTable();
        chargeParameters();
    }

    private void setupRecentTableSelectionListener() {
        tbvRecents.getSelectionModel().selectionProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.isEmpty()) {
                VehiclesDto selectedVehicle = newSelection.values().iterator().next();

                // Asegúrate de que no sean nulos antes de asignarlos
                txfPlaca.setText(selectedVehicle.getPlate() != null ? selectedVehicle.getPlate() : "");
                txfDueno.setText(selectedVehicle.getOwner() != null ? selectedVehicle.getOwner() : "");
            }
        });
    }

    private void setupPlateSearchListener() {
        txfPlaca.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isBlank()) {
                tbvRecents.getItems().clear(); // Si el campo está vacío, limpia la tabla
            } else {
                filterVehiclesByPlate(newValue); // Realiza la búsqueda con el texto ingresado
            }
        });
    }

    private void chargeParameters() {
        ParametersService service = new ParametersService();
        Respuesta respuesta = service.getParameter();
        if (respuesta.getEstado()) {
            parameters = (ParametersDto) respuesta.getResultado("Parameter");
            // txfTax.setText(parameters.getTax());
        } else {
            new Mensaje().showModal(Alert.AlertType.ERROR, "Cargar Parametros", getStage(), respuesta.getMensaje());
        }
    }

    private void filterVehiclesByPlate(String platePart) {
        try {
            VehiclesService vehicleService = new VehiclesService();
            Respuesta respuesta = vehicleService.getVehiclesByPlate(platePart);

            if (!respuesta.getEstado()) {
                tbvRecents.getItems().clear(); // Limpiar tabla si hay error
                return;
            }

            // Obtener resultados y llenar la tabla
            @SuppressWarnings("unchecked")
            List<VehiclesDto> vehicles = (List<VehiclesDto>) respuesta.getResultado("Vehicles");
            tbvRecents.getItems().clear();
            tbvRecents.getItems().addAll(vehicles);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículos por placa.", e);
            tbvRecents.getItems().clear();
        }
    }

    private void populateRecentTable() {
        MFXTableColumn<VehiclesDto> colPlate = new MFXTableColumn<>("Placa");
        colPlate.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getPlate));

        MFXTableColumn<VehiclesDto> colOwner = new MFXTableColumn<>("Propietario");
        colOwner.setRowCellFactory(vehicle -> new MFXTableRowCell<>(VehiclesDto::getOwner));

        tbvRecents.getTableColumns().addAll(Arrays.asList(colPlate, colOwner));
    }

    private void newVehicle() {
        unbindIngress();
        vehicle = new VehiclesDto();
        bindIngress();
        txfPlaca.clear();
        txfPlaca.requestFocus();

    }

    private void bindIngress() {
        txfPlaca.textProperty().bindBidirectional(vehicle.plate);
        txfDueno.textProperty().bindBidirectional(vehicle.owner);
    }

    private void unbindIngress() {
        txfPlaca.textProperty().unbindBidirectional(vehicle.plate);
        txfDueno.textProperty().unbindBidirectional(vehicle.owner);
    }

    @SuppressWarnings("unused")
    private void addRequerieds() {
        requerieds.clear();
        requerieds.add(txfPlaca);
        requerieds.add(dpDIngressDate);
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

    private LocalDateTime getHour(MFXDatePicker dpDate) {

        LocalDate date = dpDate.getValue();

        if (date == null) {
            return null;
        }

        LocalTime time = LocalTime.now();
        return LocalDateTime.of(date, time);

    }

    @SuppressWarnings("unchecked")
    private String generateUniqueReference() {
        int reference;
        boolean isUnique;
        List<String> existingReferences = new ArrayList<>();

        try {
            // Llama al servicio para obtener todas las referencias actuales
            VehiclesService vehiclesService = new VehiclesService();
            Respuesta respuesta = vehiclesService.getAllReferences();

            if (respuesta.getEstado()) {
                existingReferences = (List<String>) respuesta.getResultado("References");
            } else {
                System.err.println("Error obteniendo referencias: " + respuesta.getMensaje());
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error cargando referencias existentes.", e);
        }

        do {
            // Genera una referencia de 4 dígitos
            reference = (int) (Math.random() * 10000);

            // Verifica si la referencia generada ya existe
            isUnique = !existingReferences.contains(String.format("%04d", reference));
        } while (!isUnique);

        return String.format("%04d", reference); // Asegura que tenga 4 dígitos
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

    @FXML
    void onActionBtnClean(ActionEvent event) {
        if (new Mensaje().showConfirmation("Limpiar Campos", getStage(), "Desea limpiar los campos?")) {
            txfPlaca.clear();
            txfDueno.clear();
            txfPlaca.requestFocus();
        }

    }

    @FXML
    void onActionBtnPrint(ActionEvent event) {
        saveVehicle();

    }

    private void saveVehicle() {
        try {
            // Validar campos requeridos
            String valid = validarRequeridos();
            if (!valid.isBlank()) {
                new Mensaje().showModal(Alert.AlertType.ERROR, "Guardar Vehículo", getStage(), valid);
                return;
            }

            VehiclesService service = new VehiclesService();

            // Buscar el vehículo por placa (en cualquier estado)
            Respuesta respuesta = service.getVehicleByPlateAllStates(txfPlaca.getText());
            if (respuesta.getEstado() && respuesta.getResultado("Vehicle") != null) {
                VehiclesDto existingVehicle = (VehiclesDto) respuesta.getResultado("Vehicle");

                // Asegurar que el propietario nunca sea nulo, reemplazar por espacio en blanco
                // si es el caso
                String newOwner = txfDueno.getText().isBlank() ? " " : txfDueno.getText();
                String existingOwner = existingVehicle.getOwner() == null ? " " : existingVehicle.getOwner();

                // Si el vehículo ya existe y está en estado 'S', permitir el reingreso
                if ("S".equals(existingVehicle.getStatus())) {
                    if (!existingOwner.equals(newOwner)) {
                        new Mensaje().showModal(Alert.AlertType.WARNING, "Reingresar Vehículo", getStage(),
                                "El propietario del vehículo no coincide con el registro en la base de datos. No se puede reingresar.");
                        return;
                    }

                    // Actualizar los datos del vehículo y cambiar estado a 'P'
                    existingVehicle.setStatus("P");
                    existingVehicle.setIngress(getHour(dpDIngressDate));
                    existingVehicle.setTax(parameters.getTax());

                    Respuesta updateRespuesta = service.registerVehicle(existingVehicle);
                    if (updateRespuesta.getEstado()) {
                        VehiclesDto updatedVehicle = (VehiclesDto) updateRespuesta.getResultado("Vehicle");

                        new Mensaje().showModal(Alert.AlertType.INFORMATION, "Reingresar Vehículo", getStage(),
                                "El vehículo con placa " + txfPlaca.getText() + " ha sido reingresado correctamente.");

                        // Imprimir tiquete de ingreso
                        printIngressTicket(updatedVehicle);
                    } else {
                        new Mensaje().showModal(Alert.AlertType.ERROR, "Reingresar Vehículo", getStage(),
                                updateRespuesta.getMensaje());
                    }
                } else {
                    new Mensaje().showModal(Alert.AlertType.WARNING, "Guardar Vehículo", getStage(),
                            "El vehículo ya está registrado y en estado estacionado.");
                }
            } else {
                // Crear un nuevo objeto de vehículo para evitar referencias anteriores
                VehiclesDto newVehicle = new VehiclesDto();
                newVehicle.setPlate(txfPlaca.getText());
                newVehicle.setOwner(txfDueno.getText().isBlank() ? " " : txfDueno.getText());
                newVehicle.setIngress(getHour(dpDIngressDate));
                newVehicle.setTax(parameters.getTax());
                newVehicle.setReference(generateUniqueReference());
                newVehicle.setStatus("P");

                Respuesta createRespuesta = service.registerVehicle(newVehicle);
                if (createRespuesta.getEstado()) {
                    VehiclesDto createdVehicle = (VehiclesDto) createRespuesta.getResultado("Vehicle");

                    new Mensaje().showModal(Alert.AlertType.INFORMATION, "Ingresar Vehículo", getStage(),
                            "El vehículo con placa " + txfPlaca.getText() + " ha sido ingresado correctamente.");

                    // Imprimir tiquete de ingreso
                    printIngressTicket(createdVehicle);

                } else {
                    new Mensaje().showModal(Alert.AlertType.ERROR, "Ingresar Vehículo", getStage(),
                            createRespuesta.getMensaje());
                }
            }

            // Limpiar campos
            txfDueno.clear();
            txfPlaca.clear();
            txfPlaca.requestFocus();
            newVehicle();

        } catch (Exception ex) {
            Logger.getLogger(IngressController.class.getName()).log(Level.SEVERE, "Error ingresando el vehículo.",
                    ex);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Ingresar Vehículo", getStage(),
                    "Ocurrió un error ingresando el vehículo.");
        }
    }

    private void printIngressTicket(VehiclesDto vehicle) {
        if (vehicle == null) {
            new Mensaje().showModal(Alert.AlertType.ERROR, "Impresión de Tiquete", getStage(),
                    "No se pudo imprimir el tiquete porque el vehículo no es válido.");
            return;
        }

        try {
            // Asegurar que los datos necesarios estén presentes
            String owner = vehicle.getOwner() == null ? " " : vehicle.getOwner();
            String plate = vehicle.getPlate();
            String reference = vehicle.getReference();
            String ingressDate = vehicle.getIngress().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"));
            String tax = vehicle.getTax();

            // Validar que no haya campos obligatorios nulos
            if (plate == null || reference == null || ingressDate == null || tax == null) {
                throw new IllegalArgumentException("Datos insuficientes para imprimir el tiquete.");
            }

            IngressTicket(
                    owner,
                    reference,
                    plate,
                    ingressDate,
                    "/parqueo/san/marcos/system/resources/receipts.png");
        } catch (Exception e) {
            Logger.getLogger(IngressController.class.getName()).log(Level.SEVERE, "Error imprimiendo tiquete.",
                    e);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Impresión de Tiquete", getStage(),
                    "Ocurrió un error al intentar imprimir el tiquete.");
        }
    }

    @Override
    public void initialize() {
        dpDIngressDate.setValue(LocalDate.now());
    }

}
