package com.expensetracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class ExpenseTrackerApplication extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseTrackerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
        logger.info("Expense Tracker Application started successfully!");
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ExpenseTrackerApplication.class);
    }
}