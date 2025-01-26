/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package parqueo.san.marcos.system.model;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 *
 * @author aletr
 */
@Entity
@Table(name = "PSM_PARAMETERS", schema = "PSM")
@NamedQueries({
        @NamedQuery(name = "Parameters.findAll", query = "SELECT p FROM Parameters p"),
/*
 * @NamedQuery(name = "PsmParameters.findByParId", query =
 * "SELECT p FROM PsmParameters p WHERE p.parId = :parId"),
 * 
 * @NamedQuery(name = "PsmParameters.findByParQuantity", query =
 * "SELECT p FROM PsmParameters p WHERE p.parQuantity = :parQuantity"),
 * 
 * @NamedQuery(name = "PsmParameters.findByParTax", query =
 * "SELECT p FROM PsmParameters p WHERE p.parTax = :parTax"),
 * 
 * @NamedQuery(name = "PsmParameters.findByVersion", query =
 * "SELECT p FROM PsmParameters p WHERE p.version = :version")
 */ })
public class Parameters implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "GENERATOR_PARAMETERS_SEQUENCE", sequenceName = "PSM_PARAMETERS_SEQ01", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GENERATOR_PARAMETERS_SEQUENCE")
    @Basic(optional = false)
    @Column(name = "PAR_ID")
    private Long id;
    @Basic(optional = false)
    @Column(name = "PAR_QUANTITY")
    private String quantity;
    @Basic(optional = false)
    @Column(name = "PAR_TAX")
    private String tax;
    @Version
    @Column(name = "VERSION")
    private Long version;

    public Parameters() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Parameters(ParametersDto parameters) {
        this.id = parameters.getId();
        update(parameters);
    }

    public void update(ParametersDto parameters) {
        this.quantity = parameters.getQuantity();
        this.tax = parameters.getTax();
        this.version = parameters.getVersion();
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getTax() {
        return tax;
    }

    public void setTax(String tax) {
        this.tax = tax;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Parameters other = (Parameters) obj;
        return Objects.equals(this.id, other.id);
    }

}
