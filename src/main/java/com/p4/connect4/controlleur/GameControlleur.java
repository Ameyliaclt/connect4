package com.p4.connect4.controlleur;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.p4.connect4.model.Coup;
import com.p4.connect4.model.fct_minmax;

@CrossOrigin(origins = {"http://localhost:8080","https://connect4-fzwc.onrender.com"}, allowCredentials = "true")
@RestController
@RequestMapping("/api")
public class GameControlleur {

    private final fct_minmax mx = new fct_minmax();

    private int profondeur = 4;
    @Autowired
    private Sessionjeu jeu;

    // Joueur humain
    @PostMapping("/play/{col}")
public Map<String, Object> play(@PathVariable int col) {
    jeu.getGame().gameplay(col);
    return buildResponse(); // retourne l'état après le coup humain uniquement
}

    // Reprendre
    @PostMapping("/play")
    public Map<String, Object> playGame() {
        jeu.getGame().reprendre();
        return buildResponse();
    }

    // Coup IA
    @PostMapping("/playIA")
    public Map<String, Object> playIA() {
        if (!jeu.getGame().getisPartieTerminee() && !jeu.getGame().pause) {
            jouerUnCoupIA();
        }
        return buildResponse();
    }

    // Calcul du score et profondeur
    @PostMapping("/analyse")
    public Map<String, Object> analyse(@RequestParam(defaultValue = "4") int profondeur) {
        this.profondeur = Math.max(1, Math.min(12, profondeur));
        int cols = jeu.getGame().getCols();
        int[] scores = new int[cols];
        int[][] plateau = jeu.getGame().getPlateau();
        for (int col = 0; col < cols; col++) {
            scores[col] = mx.getScoreCol(plateau, this.profondeur, col);
        }
        Map<String, Object> resp = buildResponse();
        resp.put("scores", scores);
        resp.put("profondeur", this.profondeur);
        return resp;
    }

    // Rejouer
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        jeu.getGame().redemarrer_p();
        return buildResponse();
    }

    // Changement de mode
    @PostMapping("/setMode/{mode}")
    public Map<String, Object> setMode(@PathVariable int mode) {
        jeu.getGame().setmj(mode);
        Map<String, Object> resp = buildResponse();
        String msg = switch (mode) {
            case 1 -> "Mode 2 joueurs";
            case 2 -> "Mode vs ordinateur aléatoire";
            case 3 -> "Mode vs ordinateur MiniMax (prof. " + profondeur + ")";
            case 4 -> "Mode ordi vs ordi aléatoire";
            case 5 -> "Mode ordi vs ordi MiniMax (prof. " + profondeur + ")";
            default -> "Mode inconnu";
        };
        resp.put("message", msg);
        return resp;
    }

    // Pause
    @PostMapping("/pause")
    public Map<String, Object> pause() {
        jeu.getGame().mt_pause();
        Map<String, Object> resp = buildResponse();
        resp.put("message", "Pause activée");
        return resp;
    }

    // Retirer / Remettre
    @PostMapping("/retirer")
    public Map<String, Object> retirer() {
        jeu.getGame().retirer();
        return buildResponse();
    }

    @PostMapping("/remettre")
    public Map<String, Object> remettre() {
        jeu.getGame().remettre();
        return buildResponse();
    }

    // Etat
    @GetMapping("/state")
    public Map<String, Object> state() {
        return buildResponse();
    }

    // Jouer un coup IA
    private void jouerUnCoupIA() {
        switch (jeu.getGame().mode_j) {
            case 2, 4 -> jeu.getGame().jouervsordi();
            case 3, 5 -> {
                int col = mx.meilleurCoup(jeu.getGame().getPlateau(), profondeur, jeu.getGame().enregistrement_cp);
                if (col != -1) jeu.getGame().gameplay(col);
            }
        }
    }

    private Map<String, Object> buildResponse() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("board", jeu.getGame().getPlateau());
        resp.put("wins", getWinsArray());
        resp.put("joueurCourant", jeu.getGame().getJoueurCourant());
        resp.put("partieTerminee", jeu.getGame().getisPartieTerminee());
        resp.put("scores", new int[jeu.getGame().getCols()]);
        resp.put("mode", jeu.getGame().mode_j);
        resp.put("profondeur", profondeur);

        // Dernier coup lu directement depuis l'historique
        if (!jeu.getGame().enregistrement_cp.isEmpty()) {
            Coup dernier = jeu.getGame().enregistrement_cp.get(jeu.getGame().enregistrement_cp.size() - 1);
            resp.put("dernierCoup", dernier.getcol());
        } else {
            resp.put("dernierCoup", null);
        }

        return resp;
    }

    private boolean[][] getWinsArray() {
        boolean[][] wins = new boolean[jeu.getGame().getRows()][jeu.getGame().getCols()];
        for (int r = 0; r < jeu.getGame().getRows(); r++)
            for (int c = 0; c < jeu.getGame().getCols(); c++)
                wins[r][c] = jeu.getGame().getCaseG(r, c);
        return wins;
    }
}