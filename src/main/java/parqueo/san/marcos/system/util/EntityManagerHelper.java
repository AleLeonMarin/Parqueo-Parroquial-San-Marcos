/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parqueo.san.marcos.system.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 *
 * @author ccarranza
 */
public class EntityManagerHelper {

    private static final EntityManagerHelper SINGLENTON = new EntityManagerHelper();
    private static EntityManagerFactory emf;
    private static EntityManager em;

    static {
        try {
            emf = Persistence.createEntityManagerFactory("PSM_PU");
            em = emf.createEntityManager();
        } catch (ExceptionInInitializerError e) {
            throw e;
        }
    }

    public static EntityManagerHelper getInstance() {

        return SINGLENTON;
    }

    public static EntityManager getManager() {
        if (em == null) {
            emf = Persistence.createEntityManagerFactory("PSM_PU");
            em = emf.createEntityManager();
        }
        return em;
    }
}