package com.p4.connect4.controlleur;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.p4.connect4.model.Model_connect4;

@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Sessionjeu {
    private Model_connect4 game = new Model_connect4(9, 9, 1);  
    public Model_connect4 getGame() {
        return game;
    }
}