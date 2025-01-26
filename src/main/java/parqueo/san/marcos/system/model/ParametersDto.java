package parqueo.san.marcos.system.model;

import javafx.beans.property.SimpleStringProperty;

public class ParametersDto {

    public SimpleStringProperty id;
    public SimpleStringProperty quantity;
    public SimpleStringProperty tax;
    public Long version;

    public ParametersDto() {
        this.id = new SimpleStringProperty("");
        this.quantity = new SimpleStringProperty("");
        this.tax = new SimpleStringProperty("");
    }

    public ParametersDto(Parameters parameters) {
        this();
        this.id.set(parameters.getId().toString());
        this.quantity.set(parameters.getQuantity());
        this.tax.set(parameters.getTax());
        this.version = parameters.getVersion();
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

    public String getQuantity() {
        return quantity.get();
    }

    public void setQuantity(String quantity) {
        this.quantity.set(quantity);
    }

    public String getTax() {
        return tax.get();
    }

    public void setTax(String tax) {
        this.tax.set(tax);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
