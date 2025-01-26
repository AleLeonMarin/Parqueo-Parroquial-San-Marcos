package parqueo.san.marcos.system.model;

import java.time.LocalDateTime;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class VehiclesDto {

    public SimpleStringProperty id;
    public SimpleStringProperty plate;
    public SimpleStringProperty owner;
    public SimpleStringProperty reference;
    public SimpleStringProperty tax;
    public ObjectProperty<LocalDateTime> ingress;
    public ObjectProperty<LocalDateTime> egress;
    public SimpleBooleanProperty status;
    public Long version;

    public VehiclesDto() {
        this.id = new SimpleStringProperty("");
        this.plate = new SimpleStringProperty("");
        this.owner = new SimpleStringProperty("");
        this.reference = new SimpleStringProperty("");
        this.tax = new SimpleStringProperty("");
        this.ingress = new SimpleObjectProperty<>(LocalDateTime.now());
        this.egress = new SimpleObjectProperty<>();
        this.status = new SimpleBooleanProperty(true);
    }

    public VehiclesDto(Vehicles vehicles) {
        this();
        this.id.set(vehicles.getId().toString());
        this.plate.set(vehicles.getPlate());
        this.owner.set(vehicles.getOwner());
        this.reference.set(vehicles.getReference());
        this.tax.set(vehicles.getTax());
        this.ingress.set(vehicles.getIngress());
        this.egress.set(vehicles.getEgress());
        this.status.set(vehicles.getStatus().equals("P"));
        this.version = vehicles.getVersion();
    }

    public Long getId() {
        if (id.get() != null && !id.get().isEmpty()) {
            return Long.valueOf(id.get());
        } else {
            return null;
        }
    }

    public void setId(Long id) {
        this.id.set(id.toString());
    }

    public String getPlate() {
        return plate.get();
    }

    public void setPlate(String plate) {
        this.plate.set(plate);
    }

    public String getOwner() {
        return owner.get();
    }

    public void setOwner(String owner) {
        this.owner.set(owner);
    }

    public String getReference() {
        return reference.get();
    }

    public void setReference(String reference) {
        this.reference.set(reference);
    }

    public String getTax() {
        return tax.get();
    }

    public void setTax(String tax) {
        this.tax.set(tax);
    }

    public LocalDateTime getIngress() {
        return ingress.get();
    }

    public void setIngress(LocalDateTime ingress) {
        this.ingress.set(ingress);
    }

    public LocalDateTime getEgress() {
        return egress.get();
    }

    public void setEgress(LocalDateTime egress) {
        this.egress.set(egress);
    }

    public String getStatus() {
        return status.get() ? "P" : "S";
    }

    public void setStatus(String status) {
        this.status.set(status.equals("P"));
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
