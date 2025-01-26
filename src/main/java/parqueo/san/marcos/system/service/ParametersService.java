package parqueo.san.marcos.system.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import parqueo.san.marcos.system.model.Parameters;
import parqueo.san.marcos.system.model.ParametersDto;
import parqueo.san.marcos.system.util.EntityManagerHelper;
import parqueo.san.marcos.system.util.Respuesta;

public class ParametersService {

    @SuppressWarnings("static-access")
    EntityManager em = EntityManagerHelper.getInstance().getManager();
    private EntityTransaction et;

    public Respuesta saveParameters(ParametersDto parameters) {
        try {
            et = em.getTransaction();
            et.begin();
            Parameters parameter;
            if (parameters.getId() != null && parameters.getId() > 0) {
                parameter = em.find(Parameters.class, parameters.getId());
                if (parameter == null) {
                    return new Respuesta(false, "No se encontró el parámetro a modificar", "saveParameters");
                }

                parameter.update(parameters);
                parameter = em.merge(parameter);
            } else {
                parameter = new Parameters(parameters);
                em.persist(parameter);
            }
            et.commit();
            return new Respuesta(true, "", "", "Parameter", new ParametersDto(parameter));
        } catch (Exception e) {
            et.rollback();
            Logger.getLogger(ParametersService.class.getName()).log(Level.SEVERE,
                    "Ocurrió un error al guardar el parámetro. ", e);
            return new Respuesta(false, "Ocurrió un error al guardar el parámetro.", "saveParameters");
        }
    }

    public Respuesta getParameter() {
        try {
            Parameters parameters = em.find(Parameters.class, 1L);
            if (parameters == null) {
                return new Respuesta(false, "No se encontró el parámetro", "getParameter");
            }
            return new Respuesta(true, "", "", "Parameter", new ParametersDto(parameters));
        } catch (Exception e) {
            Logger.getLogger(ParametersService.class.getName()).log(Level.SEVERE,
                    "Ocurrió un error al obtener el parámetro. ", e);
            return new Respuesta(false, "Ocurrió un error al obtener el parámetro.", "getParameter");
        }
    }

}
