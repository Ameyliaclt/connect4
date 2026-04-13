package com.p4.connect4.controlleur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.p4.connect4.model.Coup;
import com.p4.connect4.model.PartieDB;
import com.p4.connect4.model.Sauvegarde;
import com.p4.connect4.model.fct_minmax;

@CrossOrigin(origins = {"http://localhost:8080","https://connect4-fzwc.onrender.com"}, allowCredentials = "true")
@RestController
@RequestMapping("/api")
public class GameControlleur {

    @Autowired
    private fct_minmax mx;

    private int profondeur = 4;

    @Autowired
    private Sessionjeu jeu;

    @Autowired
    private PartieDB dao;

    @PostMapping("/play/{col}")
    public Map<String, Object> play(@PathVariable int col) {
        jeu.getGame().gameplay(col);
        return buildResponse();
    }

    @PostMapping("/play")
    public Map<String, Object> playGame() {
        jeu.getGame().reprendre();
        return buildResponse();
    }

    @PostMapping("/playIA")
    public Map<String, Object> playIA() {
        if (!jeu.getGame().getisPartieTerminee() && !jeu.getGame().pause) {
            jouerUnCoupIA();
        }
        return buildResponse();
    }

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

    @PostMapping("/predire")
    public Map<String, Object> predire(@RequestParam(defaultValue = "6") int profondeur) {
        int prof = Math.max(1, Math.min(12, profondeur));
        int[][] plateau      = jeu.getGame().getPlateau();
        int joueurCourant    = jeu.getGame().getJoueurCourant();

        fct_minmax.Prediction pred = mx.predire(plateau, prof, joueurCourant);

        Map<String, Object> resp = new HashMap<>();
        resp.put("gagnant",    pred.gagnant);   // 0 = nul, 1 = J1, 2 = J2
        resp.put("coups",      pred.coups);      
        resp.put("certain",    pred.certain);    
        resp.put("profondeur", prof);
        return resp;
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        jeu.getGame().redemarrer_p();
        return buildResponse();
    }

    @PostMapping("/setMode/{mode}")
    public Map<String, Object> setMode(
            @PathVariable int mode,
            @RequestParam(defaultValue = "1") int couleur,
            @RequestParam(required = false) Integer profondeur) {
        if (profondeur != null) {
            this.profondeur = Math.max(1, Math.min(12, profondeur));
        }
        jeu.getGame().setmj(mode);
        Map<String, Object> resp = buildResponse();
        String msg = switch (mode) {
            case 1 -> "Mode 2 joueurs";
            case 2 -> "Mode vs ordinateur aléatoire";
            case 3 -> "Mode vs ordinateur MiniMax (prof. " + this.profondeur + ")";
            case 4 -> "Mode ordi vs ordi aléatoire";
            case 5 -> "Mode ordi vs ordi MiniMax (prof. " + this.profondeur + ")";
            default -> "Mode inconnu";
        };
        resp.put("message", msg);
        resp.put("profondeur", this.profondeur);
        return resp;
    }

    @PostMapping("/pause")
    public Map<String, Object> pause() {
        jeu.getGame().mt_pause();
        Map<String, Object> resp = buildResponse();
        resp.put("message", "Pause activée");
        return resp;
    }

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

    @GetMapping("/state")
    public Map<String, Object> state() {
        return buildResponse();
    }

    private void jouerUnCoupIA() {
        switch (jeu.getGame().mode_j) {
            case 2, 4 -> jeu.getGame().jouervsordi();
            case 3, 5 -> {
                int col = mx.meilleurCoup(
                        jeu.getGame().getPlateau(),
                        profondeur,
                        jeu.getGame().enregistrement_cp
                );
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

        if (!jeu.getGame().enregistrement_cp.isEmpty()) {
            Coup dernier = jeu.getGame().enregistrement_cp
                    .get(jeu.getGame().enregistrement_cp.size() - 1);
            resp.put("dernierCoup", dernier.getcol());
        } else {
            resp.put("dernierCoup", null);
        }

        if (jeu.getGame().getisPartieTerminee()) {
            try {
                Sauvegarde sv = jeu.getGame().sauvegarder_p();
                dao.sauvegarder(sv);
                resp.put("sauvegarde", "ok");
            } catch (Exception e) {
                System.err.println("[buildResponse] Erreur BDD : " + e.getMessage());
                resp.put("sauvegarde", "erreur: " + e.getMessage());
            }
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

    @PostMapping("/setPlateau")
    public Map<String, Object> setPlateau(@RequestBody int[][] grille) {
        int rows = grille.length;
        int cols = grille[0].length;

        int count1 = 0, count2 = 0;
        for (int[] row : grille)
            for (int v : row) {
                if (v == 1) count1++;
                else if (v == 2) count2++;
            }
        int joueur = (count1 <= count2) ? 1 : 2;

        jeu.getGame().redemarrer_p();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                jeu.getGame().tabl[r][c] = grille[r][c];

        jeu.getGame().joueurCourant = joueur;
        jeu.getGame().partieTerminee = false;

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                jeu.getGame().casesG[r][c] = false;

        jeu.getGame().enregistrement_cp.clear();
        ArrayList<int[]> pionsJ1 = new ArrayList<>();
        ArrayList<int[]> pionsJ2 = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            for (int r = rows - 1; r >= 0; r--) {
                if (grille[r][c] == 1) pionsJ1.add(new int[]{r, c});
                else if (grille[r][c] == 2) pionsJ2.add(new int[]{r, c});
            }
        }
        int maxCoups = Math.max(pionsJ1.size(), pionsJ2.size());
        for (int i = 0; i < maxCoups; i++) {
            if (i < pionsJ1.size()) {
                int[] p = pionsJ1.get(i);
                jeu.getGame().enregistrement_cp.add(new Coup(p[1], p[0], 1));
            }
            if (i < pionsJ2.size()) {
                int[] p = pionsJ2.get(i);
                jeu.getGame().enregistrement_cp.add(new Coup(p[1], p[0], 2));
            }
        }

        boolean victoireTrouvee = false;
        outer:
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grille[r][c] != 0) {
                    if (jeu.getGame().checkvictoire(r, c)) {
                        jeu.getGame().partieTerminee = true;
                        jeu.getGame().joueurCourant = grille[r][c];
                        victoireTrouvee = true;
                        break outer;
                    }
                }
            }
        }
        if (!victoireTrouvee && jeu.getGame().ismtchnul()) {
            jeu.getGame().partieTerminee = true;
        }

        Map<String, Object> resp = buildResponse();
        if (jeu.getGame().getisPartieTerminee()) {
            String couleurGagnant = (jeu.getGame().joueurCourant == 1) ? "Rouge" : "Jaune";
            resp.put("message", victoireTrouvee
                    ? "Le joueur " + couleurGagnant + " a déjà gagné !"
                    : "Match nul !");
        } else {
            String couleurCourant = (joueur == 1) ? "Rouge" : "Jaune";
            resp.put("message", "Situation chargée — tour du joueur " + couleurCourant);
        }
        return resp;
    }
}