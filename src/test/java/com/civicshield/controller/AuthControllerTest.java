package com.civicshield.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    private final AuthController controller = new AuthController();

    @Test
    void registerReturnsRegisterView() {
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertEquals("register", view);
    }
}
