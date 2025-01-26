package parqueo.san.marcos.system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import parqueo.san.marcos.system.model.Parameters;
import parqueo.san.marcos.system.model.Vehicles;
import parqueo.san.marcos.system.model.VehiclesDto;
import parqueo.san.marcos.system.util.EntityManagerHelper;
import parqueo.san.marcos.system.util.Respuesta;

public class VehiclesService {

    @SuppressWarnings("static-access")
    private final EntityManager em = EntityManagerHelper.getInstance().getManager();
    private EntityTransaction et;

    public Respuesta registerVehicle(VehiclesDto vehicle) {
        try {
            // Asegúrate de que la transacción esté activa antes de comenzar
            et = em.getTransaction();
            et.begin();

            Vehicles vehicles;

            // Si el vehículo ya tiene un ID, intenta actualizarlo
            if (vehicle.getId() != null && vehicle.getId() > 0) {
                vehicles = em.find(Vehicles.class, vehicle.getId());

                if (vehicles == null) {
                    return new Respuesta(false, "Vehículo no encontrado para actualizar.", "registerVehicle");
                }

                vehicles.update(vehicle);
                vehicles = em.merge(vehicles);

            } else {
                // Si no tiene un ID, verifica si ya existe un vehículo con la misma placa
                List<Vehicles> existingVehicles = em.createQuery(
                        "SELECT v FROM Vehicles v WHERE v.plate = :plate", Vehicles.class)
                        .setParameter("plate", vehicle.getPlate())
                        .getResultList();

                if (!existingVehicles.isEmpty()) {
                    // Si existe un vehículo con la misma placa, retorna un error
                    return new Respuesta(false, "Ya existe un vehículo con esta placa.", "registerVehicle");
                }

                // Crea un nuevo vehículo si no existe conflicto
                vehicles = new Vehicles(vehicle);
                em.persist(vehicles);
            }

            et.commit();
            return new Respuesta(true, "Vehículo registrado exitosamente.", "", "Vehicle", new VehiclesDto(vehicles));

        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }

            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error registrando vehículo.", e);
            return new Respuesta(false, "Error registrando el vehículo: " + e.getMessage(), "registerVehicle");
        }
    }

    public Respuesta getVehiclesParked() {
        try {
            // Filtrar vehículos con estado 'P'
            List<Vehicles> vehicles = em.createNamedQuery("Vehicles.findByEstado", Vehicles.class)
                    .setParameter("status", "P")
                    .getResultList();

            List<VehiclesDto> vehiclesDto = new ArrayList<>();

            for (Vehicles vehicle : vehicles) {
                vehiclesDto.add(new VehiclesDto(vehicle));
            }

            return new Respuesta(true, "", "", "Vehicles", vehiclesDto);
        } catch (Exception e) {
            e.printStackTrace();
            return new Respuesta(false, "Error obteniendo los vehículos estacionados.", "getVehiclesParked");
        }
    }

    public Respuesta getAvailableSpaces() {
        try {
            // Obtener el total de espacios desde los parámetros
            Parameters parameters = em.find(Parameters.class, 1L);
            if (parameters == null) {
                return new Respuesta(false, "No se encontró el parámetro de espacios.", "getAvailableSpaces");
            }

            // Convertir el valor de totalSpaces de String a int
            int totalSpaces;
            try {
                totalSpaces = Integer.parseInt(parameters.getQuantity());
            } catch (NumberFormatException e) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                        "El valor de 'quantity' no es un número válido.", e);
                return new Respuesta(false, "El valor de 'quantity' en los parámetros no es un número válido.",
                        "getAvailableSpaces");
            }

            // Contar la cantidad de vehículos estacionados
            Long parkedVehiclesCount = em.createNamedQuery("Vehicles.countByEstado", Long.class)
                    .setParameter("status", "P")
                    .getSingleResult();

            // Calcular los espacios disponibles
            int availableSpaces = totalSpaces - parkedVehiclesCount.intValue();

            if (availableSpaces < 0) {
                availableSpaces = 0; // Evitar números negativos si hay inconsistencia
            }

            // Retornar la cantidad de espacios disponibles
            return new Respuesta(true, "", "", "AvailableSpaces", availableSpaces);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Ocurrió un error al calcular los espacios disponibles. ", e);
            return new Respuesta(false, "Ocurrió un error al calcular los espacios disponibles.", "getAvailableSpaces");
        }
    }

    public Respuesta getParkedVehiclesCount() {
        try {
            // Contar la cantidad de vehículos estacionados (estado 'P')
            Long parkedVehiclesCount = em.createNamedQuery("Vehicles.countByEstado", Long.class)
                    .setParameter("status", "P")
                    .getSingleResult();

            return new Respuesta(true, "", "", "ParkedVehiclesCount", parkedVehiclesCount.intValue());
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Ocurrió un error al contar los vehículos estacionados.", e);
            return new Respuesta(false, "Ocurrió un error al contar los vehículos estacionados.",
                    "getParkedVehiclesCount");
        }
    }

    public Respuesta getSumOfTaxForStateS() {
        try {
            LocalDate today = LocalDate.now();

            BigDecimal totalTax = em.createQuery(
                    "SELECT COALESCE(SUM(FUNCTION('TO_NUMBER', v.tax)), 0) " +
                            "FROM Vehicles v " +
                            "WHERE v.status = :status AND FUNCTION('TRUNC', v.ingress) = :today",
                    BigDecimal.class)
                    .setParameter("status", "S")
                    .setParameter("today", java.sql.Date.valueOf(today))
                    .getSingleResult();

            double totalTaxDouble = totalTax.doubleValue();

            return new Respuesta(true, "", "", "TotalTax", totalTaxDouble);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Ocurrió un error al calcular la suma de impuestos.", e);
            return new Respuesta(false, "Ocurrió un error al calcular la suma de impuestos.", "getSumOfTaxForStateS");
        }
    }

    public Respuesta getAllVehicles() {
        try {
            // Ejecutar la consulta nombrada para obtener todos los vehículos
            List<Vehicles> vehicles = em.createNamedQuery("Vehicles.findAll", Vehicles.class)
                    .getResultList();

            // Convertir la lista de Vehicles a una lista de VehiclesDto
            List<VehiclesDto> vehiclesDtoList = vehicles.stream()
                    .map(VehiclesDto::new) // Asumiendo que VehiclesDto tiene un constructor que acepta Vehicles
                    .toList();

            return new Respuesta(true, "Vehículos obtenidos correctamente.", "", "Vehicles", vehiclesDtoList);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error obteniendo todos los vehículos.", e);
            return new Respuesta(false, "Ocurrió un error al obtener los vehículos.", "getAllVehicles");
        }
    }

    public Respuesta deleteVehicle(Long id) {
        try {
            // Inicia una transacción
            et = em.getTransaction();
            et.begin();

            // Busca el vehículo por su ID
            Vehicles vehicle = em.find(Vehicles.class, id);
            if (vehicle == null) {
                return new Respuesta(false, "Vehículo no encontrado.", "deleteVehicle");
            }

            // Elimina el vehículo
            em.remove(vehicle);

            // Confirma la transacción
            et.commit();

            return new Respuesta(true, "Vehículo eliminado correctamente.", "");
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback(); // Realiza un rollback si ocurre un error
            }
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error al eliminar el vehículo.", e);
            return new Respuesta(false, "Ocurrió un error al eliminar el vehículo.", "deleteVehicle");
        }
    }

    public Respuesta getVehiclesByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            // Convierte las fechas al rango completo
            LocalDateTime startDateTime = startDate.atStartOfDay(); // 00:00:00
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59); // 23:59:59

            // Consulta que incluye vehículos con estado 'S' y maneja posibles valores NULL
            // en egress
            List<Vehicles> vehicles = em.createQuery(
                    "SELECT v FROM Vehicles v " +
                            "WHERE v.ingress >= :startDate " +
                            "AND (v.egress <= :endDate OR v.egress IS NULL) " +
                            "AND v.status = :status",
                    Vehicles.class)
                    .setParameter("startDate", startDateTime)
                    .setParameter("endDate", endDateTime)
                    .setParameter("status", "S")
                    .getResultList();

            // Convierte la lista de resultados a DTO
            List<VehiclesDto> vehiclesDtoList = vehicles.stream()
                    .map(VehiclesDto::new)
                    .toList();

            return new Respuesta(true, "", "", "Vehicles", vehiclesDtoList);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Error obteniendo vehículos en el rango de fechas.", e);
            return new Respuesta(false, "Error al obtener vehículos.", "getVehiclesByDateRange");
        }
    }

    public Respuesta getVehiclesByPlate(String partialPlate) {
        try {
            // Consulta JPQL para buscar vehículos cuyo número de placa contenga el texto
            // ingresado y que su estado sea 'S'
            List<Vehicles> vehicles = em.createQuery(
                    "SELECT v FROM Vehicles v " +
                            "WHERE v.plate LIKE :partialPlate " +
                            "AND v.status = :status " + // Filtrar por estado 'S'
                            "ORDER BY v.ingress DESC",
                    Vehicles.class)
                    .setParameter("partialPlate", "%" + partialPlate + "%")
                    .setParameter("status", "S") // Estado requerido
                    .getResultList();

            // Convierte la lista de resultados a DTO
            List<VehiclesDto> vehiclesDtoList = vehicles.stream()
                    .map(VehiclesDto::new) // Asegúrate de que VehiclesDto tenga un constructor que reciba Vehicles
                    .toList();

            // Retornar la respuesta con los vehículos encontrados
            return new Respuesta(true, "", "", "Vehicles", vehiclesDtoList);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Error buscando vehículos por placa.", e);
            return new Respuesta(false, "Error al buscar vehículos.", "getVehiclesByPlate");
        }
    }

    public Respuesta getAllReferences() {
        try {
            // Consulta para obtener todas las referencias en la base de datos
            List<String> references = em.createQuery(
                    "SELECT v.reference FROM Vehicles v",
                    String.class).getResultList();

            return new Respuesta(true, "", "", "References", references);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error obteniendo todas las referencias.", e);
            return new Respuesta(false, "Error al obtener las referencias.", "getAllReferences");
        }
    }

    public Respuesta getVehicleByPlate(String plate) {
        try {
            // Buscar el vehículo con la placa especificada (sin filtrar por estado)
            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.plate = :plate", Vehicles.class)
                    .setParameter("plate", plate.trim())
                    .getSingleResult();

            return new Respuesta(true, "", "", "Vehicle", new VehiclesDto(vehicle));
        } catch (jakarta.persistence.NoResultException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No se encontró un vehículo con la placa: {0}",
                    plate);
            return new Respuesta(false, "No se encontró ningún vehículo con la placa especificada.",
                    "getVehicleByPlate");
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo por placa.", e);
            return new Respuesta(false, "Error buscando vehículo por placa.", "getVehicleByPlate");
        }
    }

    public Respuesta getVehicleByPlateAllStates(String plate) {
        try {
            // Buscar el vehículo con la placa especificada sin importar su estado
            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.plate = :plate", Vehicles.class)
                    .setParameter("plate", plate.trim())
                    .getSingleResult();

            return new Respuesta(true, "", "", "Vehicle", new VehiclesDto(vehicle));
        } catch (jakarta.persistence.NoResultException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No se encontró un vehículo con la placa: {0}",
                    plate);
            return new Respuesta(false, "No se encontró ningún vehículo con la placa especificada.",
                    "getVehicleByPlateAllStates");
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo por placa.", e);
            return new Respuesta(false, "Error buscando vehículo por placa.", "getVehicleByPlateAllStates");
        }
    }

    public Respuesta getVehicleByOwner(String owner) {
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buscando vehículo con propietario: {0}",
                    owner);

            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.owner = :owner AND v.status = :status", Vehicles.class)
                    .setParameter("owner", owner.trim())
                    .setParameter("status", "P")
                    .getSingleResult();

            if (vehicle != null) {
                return new Respuesta(true, "", "", "Vehicle", new VehiclesDto(vehicle));
            } else {
                return new Respuesta(false,
                        "No se encontró ningún vehículo con el propietario especificado y estado P.",
                        "getVehicleByOwner");
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo por propietario.",
                    e);
            return new Respuesta(false, "Error buscando vehículo por propietario.", "getVehicleByOwner");
        }
    }

    public Respuesta getVehicleByReference(String reference) {
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buscando vehículo con referencia: {0}",
                    reference);

            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.reference = :reference AND v.status = :status", Vehicles.class)
                    .setParameter("reference", reference.trim())
                    .setParameter("status", "P")
                    .getSingleResult();

            if (vehicle != null) {
                return new Respuesta(true, "", "", "Vehicle", new VehiclesDto(vehicle));
            } else {
                return new Respuesta(false, "No se encontró ningún vehículo con la referencia especificada y estado P.",
                        "getVehicleByReference");
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo por referencia.", e);
            return new Respuesta(false, "Error buscando vehículo por referencia.", "getVehicleByReference");
        }
    }

    public Respuesta getVehicleByPlateEgress(String plate) {
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buscando vehículo con placa: {0}", plate);

            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.plate = :plate AND v.status = :status", Vehicles.class)
                    .setParameter("plate", plate.trim())
                    .setParameter("status", "P")
                    .getSingleResult();

            if (vehicle != null) {
                return new Respuesta(true, "", "", "Vehicle", new VehiclesDto(vehicle));
            } else {
                return new Respuesta(false, "No se encontró ningún vehículo con la placa especificada y estado P.",
                        "getVehicleByPlate");
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo por placa.", e);
            return new Respuesta(false, "Error buscando vehículo por placa.", "getVehicleByPlate");
        }
    }

    public Respuesta getVehicleForReprint(String plate) {
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "Buscando vehículo para reimpresión con placa: {0}", plate);

            // Buscar el vehículo en estado 'P' o 'S' (puedes ajustar los estados según lo
            // necesario)
            Vehicles vehicle = em.createQuery(
                    "SELECT v FROM Vehicles v WHERE v.plate = :plate", Vehicles.class)
                    .setParameter("plate", plate.trim())
                    .getSingleResult();

            if (vehicle != null) {
                return new Respuesta(true, "Vehículo encontrado correctamente.", "", "Vehicle",
                        new VehiclesDto(vehicle));
            } else {
                return new Respuesta(false, "No se encontró ningún vehículo con la placa especificada.",
                        "getVehicleForReprint");
            }
        } catch (jakarta.persistence.NoResultException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No se encontró un vehículo con la placa: {0}",
                    plate);
            return new Respuesta(false, "No se encontró ningún vehículo con la placa especificada.",
                    "getVehicleForReprint");
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error buscando vehículo para reimpresión.",
                    e);
            return new Respuesta(false, "Error buscando vehículo para reimpresión.", "getVehicleForReprint");
        }
    }

}
