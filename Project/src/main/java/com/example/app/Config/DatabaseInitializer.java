package com.example.app.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Self-contained database initializer that handles SQL script errors gracefully.
 * This component runs after the application context is loaded and executes
 * the import.sql script with complete error handling - no additional configuration needed.
 * 
 * Features:
 * - Works with existing application-db.properties (no new files needed)
 * - Graceful error handling - never crashes the application
 * - Database connectivity checks
 * - Duplicate initialization prevention
 * - Detailed logging and error reporting
 */
@Component
@Profile("db") // Only run in database mode
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting custom database initialization...");
        
        try {
            // Check if database connection is available
            if (!isDatabaseAvailable()) {
                logger.warn("Database not available. Skipping initialization. " +
                          "Check your database connection configuration.");
                return;
            }
            
            // Check if we should run initialization (e.g., check if data already exists)
            if (shouldSkipInitialization()) {
                logger.info("Database already initialized, skipping init script.");
                return;
            }
            
            // Load and execute the SQL script
            executeSqlScript();
            
            logger.info("Database initialization completed successfully.");
            
        } catch (Exception e) {
            // Log the error but don't crash the application
            logger.error("Database initialization failed, but application will continue: {}", e.getMessage(), e);
            logger.warn("The application is running without initial test data.");
            logger.info("To fix this issue check: " +
                       "1. Database connection is working " +
                       "2. Database tables exist (run with spring.jpa.hibernate.ddl-auto=create-drop first) " +
                       "3. The import.sql script syntax is correct");
        }
    }

    private boolean isDatabaseAvailable() {
        try {
            // Simple test query to check database connectivity
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            logger.debug("Database connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean shouldSkipInitialization() {
        try {
            // Check if users already exist (simple check to avoid duplicate initialization)
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBERS WHERE username = 'u1'", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.debug("Could not check existing data (probably first run): {}", e.getMessage());
            return false;
        }
    }

    private void executeSqlScript() throws Exception {
        logger.info("Loading import.sql script...");
        
        // Load the SQL script from classpath
        ClassPathResource resource = new ClassPathResource("import.sql");
        String sqlScript = FileCopyUtils.copyToString(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        
        // Split the script into individual statements
        String[] statements = sqlScript.split(";");
        
        int successCount = 0;
        int errorCount = 0;
        
        for (String statement : statements) {
            String trimmedStatement = statement.trim();
            
            // Skip empty statements and comments
            if (trimmedStatement.isEmpty() || trimmedStatement.startsWith("--")) {
                continue;
            }
            
            try {
                jdbcTemplate.execute(trimmedStatement);
                successCount++;
                logger.debug("Successfully executed SQL statement: {}", 
                    trimmedStatement.substring(0, Math.min(50, trimmedStatement.length())) + "...");
                
            } catch (Exception e) {
                errorCount++;
                logger.warn("Failed to execute SQL statement: {} - Error: {}", 
                    trimmedStatement.substring(0, Math.min(50, trimmedStatement.length())) + "...", 
                    e.getMessage());
                
                // You can decide whether to continue or stop on specific errors
                // For now, we continue with other statements
            }
        }
        
        logger.info("Database initialization summary: {} statements succeeded, {} failed", 
            successCount, errorCount);
        
        if (errorCount > 0) {
            logger.warn("Some initialization statements failed. Check logs for details.");
        }
    }
}
