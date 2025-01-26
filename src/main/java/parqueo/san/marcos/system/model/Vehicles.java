/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package parqueo.san.marcos.system.model;

import java.io.Serializable;
import java.time.LocalDateTime;
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
 * s
 *
 * @author aletr
 */
@Entity
@Table(name = "PSM_VEHICLES")
@NamedQueries({
        @NamedQuery(name = "Vehicles.findAll", query = "SELECT p FROM Vehicles p"),
        @NamedQuery(name = "Vehicles.findByEstado", query = "SELECT v FROM Vehicles v WHERE v.status = :status"),
        @NamedQuery(name = "Vehicles.countByEstado", query = "SELECT COUNT(v) FROM Vehicles v WHERE v.status = :status")

/*
 * @NamedQuery(name = "PsmVehicles.findByVehId", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehId = :vehId"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehOwner", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehOwner = :vehOwner"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehReference", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehReference = :vehReference"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehPlate", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehPlate = :vehPlate"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehIngressDate", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehIngressDate = :vehIngressDate"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehEgressDate", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehEgressDate = :vehEgressDate"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehStatus", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehStatus = :vehStatus"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehTax", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehTax = :vehTax"),
 * 
 * @NamedQuery(name = "PsmVehicles.findByVehVersion", query =
 * "SELECT p FROM PsmVehicles p WHERE p.vehVersion = :vehVersion")
 */ })
public class Vehicles implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "GENERATOR_VEHICLES_SEQUENCE", sequenceName = "PSM_VEHICLES_SEQ01", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GENERATOR_VEHICLES_SEQUENCE")
    @Basic(optional = false)
    @Column(name = "VEH_ID")
    private Long id;
    @Basic(optional = false)
    @Column(name = "VEH_OWNER")
    private String owner;
    @Basic(optional = false)
    @Column(name = "VEH_REFERENCE")
    private String reference;
    @Basic(optional = false)
    @Column(name = "VEH_PLATE")
    private String plate;
    @Basic(optional = false)
    @Column(name = "VEH_INGRESS_DATE")
    private LocalDateTime ingress;
    @Basic(optional = false)
    @Column(name = "VEH_EGRESS_DATE")
    private LocalDateTime egress;
    @Basic(optional = false)
    @Column(name = "VEH_STATUS")
    private String status;
    @Basic(optional = false)
    @Column(name = "VEH_TAX")
    private String tax;
    @Version
    @Column(name = "VEH_VERSION")
    private Long version;

    public Vehicles() {
    }

    public Vehicles(VehiclesDto vehicles) {
        this.id = vehicles.getId();
        update(vehicles);
    }

    public void update(VehiclesDto vehicles) {
        this.owner = vehicles.getOwner();
        this.reference = vehicles.getReference();
        this.plate = vehicles.getPlate();
        this.ingress = vehicles.getIngress();
        this.egress = vehicles.getEgress();
        this.status = vehicles.getStatus();
        this.tax = vehicles.getTax();
        this.version = vehicles.getVersion();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public LocalDateTime getIngress() {
        return ingress;
    }

    public void setIngress(LocalDateTime ingress) {
        this.ingress = ingress;
    }

    public LocalDateTime getEgress() {
        return egress;
    }

    public void setEgress(LocalDateTime egress) {
        this.egress = egress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.id);
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
        final Vehicles other = (Vehicles) obj;
        return Objects.equals(this.id, other.id);
    }

}
