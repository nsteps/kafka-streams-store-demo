package ru.step.store;

public class LocalRunner {
    public static void main(String[] args) {
        var app = StoreApplication.createSpringApplication();
        app.addInitializers(new AbstractIntegrationTest.Initializer());
        app.run(args);
    }
}
