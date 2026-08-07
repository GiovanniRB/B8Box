package com.b8box;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootApplication
public class B8boxApplication implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    public static void main(String[] args) {
        SpringApplication.run(B8boxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("🔍 Verificando entidades JPA...");
        
        // Verifica se a conexão com o banco está OK
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            System.out.println("✅ Conexão com o banco OK!");
        } catch (Exception e) {
            System.out.println("❌ Erro na conexão: " + e.getMessage());
        }

        // Mostra as tabelas existentes
        try {
            var result = entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            ).getResultList();
            System.out.println("📊 Tabelas encontradas: " + result);
        } catch (Exception e) {
            System.out.println("❌ Erro ao listar tabelas: " + e.getMessage());
        }
        System.out.println("=========================================");
    }
}