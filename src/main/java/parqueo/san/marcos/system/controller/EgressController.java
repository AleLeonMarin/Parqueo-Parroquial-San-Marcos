package parqueo.san.marcos.system.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Desktop;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import parqueo.san.marcos.system.model.ParametersDto;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.service.ParametersService;
import parqueo.san.marcos.system.service.VehiclesService;
import parqueo.san.marcos.system.util.AppContext;
import parqueo.san.marcos.system.util.Mensaje;
import parqueo.san.marcos.system.util.Respuesta;

public class EgressController extends Controller implements Initializable {

    @FXML
    private MFXButton btnEgress;

    @FXML
    private MFXButton btnReceipt;

    @FXML
    private MFXDatePicker dpEgressDate;

    @FXML
    private MFXDatePicker dpIngressDate;

    @FXML
    private MFXTextField txfOwner;

    @FXML
    private MFXTextField txfPlate;

    @FXML
    private MFXTextField txfReference;

    @FXML
    private MFXTextField txfTax;

    private VehiclesDto selectedVehicle;
    private VehiclesDto vehicle;
    ParametersDto parameters;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDatePickerListener();
        vehicle = new VehiclesDto();
        parameters = new ParametersDto();
        chargeParameters();

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

    private void setupDatePickerListener() {
        dpEgressDate.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (selectedVehicle != null && newValue != null) {
                LocalDateTime ingressDateTime = selectedVehicle.getIngress();
                LocalDateTime egressDateTime = getHour(dpEgressDate);

                updateFare(ingressDateTime, egressDateTime);
            }
        });
    }

    private LocalDateTime getHour(MFXDatePicker dpDate) {

        LocalDate date = dpDate.getValue();

        if (date == null) {
            return null;
        }

        LocalTime time = LocalTime.now();
        return LocalDateTime.of(date, time);

    }

    private int calculateFare(LocalDateTime ingressDateTime, LocalDateTime egressDateTime) {
        if (ingressDateTime == null || egressDateTime == null) {
            throw new IllegalArgumentException("Las fechas de ingreso y egreso no pueden ser nulas.");
        }

        // Validar que la fecha de egreso sea posterior a la de ingreso
        if (!egressDateTime.isAfter(ingressDateTime)) {
            throw new IllegalArgumentException("La fecha de egreso debe ser posterior a la fecha de ingreso.");
        }

        // Validar y cargar la tarifa base desde el parámetro
        if (parameters.getTax() == null || parameters.getTax().isBlank()) {
            throw new IllegalArgumentException("Error: El parámetro de tarifa base (Tax) no está configurado.");
        }

        int baseTax;
        try {
            baseTax = Integer.parseInt(parameters.getTax()); // Convertir a entero
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error: El parámetro de tarifa base (Tax) debe ser un número válido.");
        }

        // Calcula la diferencia en minutos
        long totalMinutes = java.time.Duration.between(ingressDateTime, egressDateTime).toMinutes();
        System.out.println("Total minutos: " + totalMinutes);

        // Caso: Si los minutos son menores o iguales a 60 (primera hora)
        if (totalMinutes <= 60) {
            int roundedFare = roundSelective(baseTax);
            System.out.println("Tarifa base aplicada y redondeada: " + roundedFare);
            return roundedFare;
        }

        // Caso: Minutos adicionales después de la primera hora
        long extraMinutes = totalMinutes - 60;
        int additionalFare = (int) (extraMinutes * 10); // 10 colones por minuto adicional
        int totalFare = baseTax + additionalFare;

        // Redondear el resultado total
        int roundedFare = roundSelective(totalFare);
        System.out.println("Tarifa calculada y redondeada: " + roundedFare);

        return roundedFare;
    }

    private int roundSelective(int amount) {
        int lastTwoDigits = amount % 100; // Obtiene las decenas y unidades
        if (lastTwoDigits == 10 || lastTwoDigits == 20 || lastTwoDigits == 30 || lastTwoDigits == 40
                || lastTwoDigits == 60 ||
                lastTwoDigits == 70 || lastTwoDigits == 80 || lastTwoDigits == 90) {
            // Redondea al múltiplo superior de 50
            return ((amount / 50) + 1) * 50;
        }
        return amount; // Si no coincide con las condiciones, no redondea
    }

    private void updateFare(LocalDateTime ingressDateTime, LocalDateTime egressDateTime) {
        try {
            if (ingressDateTime != null && egressDateTime != null) {
                int fare = calculateFare(ingressDateTime, egressDateTime);
                txfTax.setText(String.valueOf(fare));
            }
        } catch (Exception e) {
            txfTax.setText("Error");
        }
    }

    public static void egressTiquet(String owner, String reference, String plate,
            String ingressDate, String egressDate, String fare, String imagePath) {
        // Convertir mm a puntos (1 mm = 2.83465 puntos)
        float widthInMm = 58;
        float heightInMm = 297;
        float widthInPoints = widthInMm * 2.83465f;
        float heightInPoints = heightInMm * 2.83465f;

        // Definir tamaño personalizado del documento
        PDRectangle customSize = new PDRectangle(widthInPoints, heightInPoints);
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(customSize);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Centrar contenido horizontalmente
            float centerX = widthInPoints / 2;

            // Encabezado: Parqueo Parroquial (línea 1) y San Marcos (línea 2)
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            String line1 = "Parqueo Parroquial";
            String line2 = "San Marcos";

            contentStream.beginText();
            float line1Width = (float) PDType1Font.HELVETICA_BOLD.getStringWidth(line1) / 1000 * 12;
            contentStream.newLineAtOffset(centerX - line1Width / 2, heightInPoints - 30);
            contentStream.showText(line1);
            contentStream.endText();

            contentStream.beginText();
            float line2Width = (float) PDType1Font.HELVETICA_BOLD.getStringWidth(line2) / 1000 * 12;
            contentStream.newLineAtOffset(centerX - line2Width / 2, heightInPoints - 50);
            contentStream.showText(line2);
            contentStream.endText();

            // Imagen debajo del título
            InputStream imageStream = IngressController.class.getResourceAsStream(imagePath);
            if (imageStream == null) {
                throw new IOException("No se pudo cargar la imagen desde el recurso empaquetado.");
            }
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageStream.readAllBytes(),
                    "receipts.png");
            float imageWidth = 40;
            float imageHeight = 40;
            float imageX = (widthInPoints - imageWidth) / 2;
            float imageY = heightInPoints - 100;
            contentStream.drawImage(pdImage, imageX, imageY, imageWidth, imageHeight);

            // Información del negocio (líneas separadas)
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            String[] businessInfo = {
                    "Genesis Brenes Calvo",
                    "Cédula: 115810605",
                    "Teléfono: 8449-6373"
            };

            float textYOffset = heightInPoints - 120; // Reducir espacio antes del nombre

            for (String line : businessInfo) {
                contentStream.beginText();
                float textWidth = (float) PDType1Font.HELVETICA.getStringWidth(line) / 1000 * 10;
                contentStream.newLineAtOffset(centerX - textWidth / 2, textYOffset);
                contentStream.showText(line);
                contentStream.endText();
                textYOffset -= 12; // Espaciado entre líneas
            }

            // Título: Tiquete de Salida
            String tiqueteTitle = "Tiquete de Salida";
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.beginText();
            float tiqueteWidth = (float) PDType1Font.HELVETICA_BOLD.getStringWidth(tiqueteTitle) / 1000 * 12;
            contentStream.newLineAtOffset(centerX - tiqueteWidth / 2, textYOffset - 10);
            contentStream.showText(tiqueteTitle);
            contentStream.endText();

            // Detalles del vehículo
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            String[][] vehicleDetails = {
                    { "Propietario", owner },
                    { "Referencia", reference },
                    { "Número de Placa", plate },
                    { "Ingreso", ingressDate },
                    { "Egreso", egressDate },
                    { "Monto por cancelar", fare + " colones" }
            };

            textYOffset -= 40; // Ajustar espacio antes de los detalles
            for (String[] detail : vehicleDetails) {
                contentStream.beginText();
                float labelWidth = (float) PDType1Font.HELVETICA.getStringWidth(detail[0]) / 1000 * 10;
                contentStream.newLineAtOffset(centerX - labelWidth / 2, textYOffset);
                contentStream.showText(detail[0]);
                contentStream.endText();
                textYOffset -= 12;

                contentStream.beginText();
                float valueWidth = (float) PDType1Font.HELVETICA.getStringWidth(detail[1]) / 1000 * 10;
                contentStream.newLineAtOffset(centerX - valueWidth / 2, textYOffset);
                contentStream.showText(detail[1]);
                contentStream.endText();
                textYOffset -= 12; // Espaciado entre líneas
            }

            // Pie de página
            String[] footer = {
                    "De lunes a viernes",
                    "8:00 am a 6:00 pm",
                    "¡Gracias por preferirnos!"
            };

            textYOffset -= 20; // Ajustar espacio antes del pie de página
            for (String line : footer) {
                contentStream.beginText();
                float textWidthFooter = (float) PDType1Font.HELVETICA_OBLIQUE.getStringWidth(line) / 1000 * 8;
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                contentStream.newLineAtOffset(centerX - textWidthFooter / 2, textYOffset);
                contentStream.showText(line);
                contentStream.endText();
                textYOffset -= 10; // Espaciado entre líneas
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Guardar el archivo PDF
        String fileName = "Tiquete_Salida_58x297.pdf";
        File pdfFile = new File(fileName);
        try {
            document.save(pdfFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Abrir el archivo PDF automáticamente
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(pdfFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Tiquete generado y abierto correctamente.");
    }

    @FXML
    void onActionBtnEgress(ActionEvent event) {
        egressCar();
        this.getStage().close();

    }

    @FXML
    void onActionBtnReceipt(ActionEvent event) {
        try {
            // Formateador para las fechas en formato día-mes-año hora:minutos
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

            // Validar datos del vehículo
            if (selectedVehicle == null) {
                throw new IllegalArgumentException("No se ha seleccionado un vehículo para egreso.");
            }

            String owner = selectedVehicle.getOwner() == null ? " " : selectedVehicle.getOwner();
            String reference = selectedVehicle.getReference() == null ? " " : selectedVehicle.getReference();
            String plate = selectedVehicle.getPlate();
            String fare = txfTax.getText();

            if (plate == null || plate.isBlank()) {
                throw new IllegalArgumentException("La placa del vehículo no puede estar vacía.");
            }
            if (fare == null || fare.isBlank()) {
                throw new IllegalArgumentException("La tarifa no puede estar vacía.");
            }

            // Validar fechas de ingreso y egreso
            String ingressDate = selectedVehicle.getIngress().format(formatter); // Formatear fecha de ingreso
            LocalDateTime egressDateTime = getHour(dpEgressDate);
            if (egressDateTime == null) {
                throw new IllegalArgumentException("La fecha de egreso no puede estar vacía.");
            }
            String egressDate = egressDateTime.format(formatter); // Formatear fecha de egreso

            // Generar el recibo
            egressTiquet(
                    owner,
                    reference,
                    plate,
                    ingressDate,
                    egressDate,
                    fare,
                    "/parqueo/san/marcos/system/resources/receipts.png");

            // Registrar el egreso del vehículo
            egressCar();
            this.getStage().close();

        } catch (IllegalArgumentException e) {
            new Mensaje().showModal(Alert.AlertType.WARNING, "Tiquete de Egreso", getStage(), e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error generando tiquete de egreso.", e);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Tiquete de Egreso", getStage(),
                    "Ocurrió un error al generar el tiquete de egreso.");
        }

    }

    private void bindIngress() {
        txfPlate.textProperty().bindBidirectional(vehicle.plate);
        txfOwner.textProperty().bindBidirectional(vehicle.owner);
    }

    private void unbindIngress() {
        txfPlate.textProperty().unbindBidirectional(vehicle.plate);
        txfOwner.textProperty().unbindBidirectional(vehicle.owner);
    }

    private void egressCar() {
        try {
            // Asignar valores al vehículo actual para egreso
            vehicle.setTax(txfTax.getText());
            vehicle.setStatus("S");
            vehicle.setReference(txfReference.getText());
            vehicle.setEgress(getHour(dpEgressDate));

            // Validar el propietario
            if (txfOwner.getText() == null || txfOwner.getText().isBlank()) {
                vehicle.setOwner(" "); // Asigna un espacio si está vacío o nulo
            } else {
                vehicle.setOwner(txfOwner.getText());
            }

            // Registrar los cambios del vehículo
            VehiclesService service = new VehiclesService();
            Respuesta respuesta = service.registerVehicle(vehicle);

            if (respuesta.getEstado()) {
                unbindIngress(); // Desenlaza las propiedades
                this.vehicle = (VehiclesDto) respuesta.getResultado("Vehicle");
                bindIngress(); // Vuelve a enlazar las propiedades

                new Mensaje().showModal(Alert.AlertType.INFORMATION, "Vehículo en salida", getStage(),
                        "Egreso exitoso.");
                txfTax.clear(); // Limpiar el campo de tarifa
                txfOwner.clear(); // Limpiar el campo de propietario
                txfReference.clear(); // Limpiar el campo de referencia
                txfPlate.clear(); // Limpiar el campo de placa
            } else {
                new Mensaje().showModal(Alert.AlertType.ERROR, "Vehículo en salida", getStage(),
                        respuesta.getMensaje());
            }

        } catch (Exception ex) {
            Logger.getLogger(IngressController.class.getName()).log(Level.SEVERE, "Error dando egreso", ex);
            new Mensaje().showModal(Alert.AlertType.ERROR, "Error dando egreso", getStage(),
                    "Ocurrió un error dando egreso al vehículo.");
        }
    }

    private void checkAppContext() {
        Object contextVehicle = AppContext.getInstance().get("vehicle");

        if (contextVehicle instanceof VehiclesDto vehicleDto) {
            System.out.println("Vehículo encontrado en AppContext: " + vehicleDto);

            // Configura los campos con el vehículo seleccionado
            setVehicleData(vehicleDto);

            // Limpia el AppContext
            AppContext.getInstance().set("vehicle", null);
        } else {
            System.err.println("El objeto en AppContext no es de tipo VehiclesDto.");
        }
    }

    private void setVehicleData(VehiclesDto vehicleDto) {
        System.out.println("Rellenando datos del vehículo seleccionado...");

        selectedVehicle = vehicleDto;

        // Actualiza los campos del vehículo
        vehicle.setId(vehicleDto.getId());
        vehicle.setPlate(vehicleDto.getPlate());
        vehicle.setOwner(vehicleDto.getOwner());
        vehicle.setReference(vehicleDto.getReference());
        vehicle.setIngress(vehicleDto.getIngress());
        vehicle.setEgress(vehicleDto.getEgress());
        vehicle.setTax(vehicleDto.getTax());
        vehicle.setStatus(vehicleDto.getStatus());
        vehicle.setVersion(vehicleDto.getVersion());

        // Actualiza los campos de la interfaz
        txfPlate.setText(vehicleDto.getPlate());
        txfOwner.setText(vehicleDto.getOwner());
        txfReference.setText(vehicleDto.getReference());
        dpIngressDate.setValue(vehicleDto.getIngress().toLocalDate());

        // Calcula la tarifa si hay una fecha de egreso seleccionada
        LocalDateTime ingressDateTime = vehicleDto.getIngress();
        LocalDateTime egressDateTime = getHour(dpEgressDate);

        if (ingressDateTime != null && egressDateTime != null) {
            updateFare(ingressDateTime, egressDateTime);
        }

        System.out.println("Datos del vehículo configurados correctamente.");
    }

    @Override
    public void initialize() {
        dpEgressDate.setValue(LocalDate.now());
        checkAppContext();
    }

}
