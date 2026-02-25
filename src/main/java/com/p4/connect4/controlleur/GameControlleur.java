package com.p4.connect4.controlleur;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.p4.connect4.model.Model_connect4;
import com.p4.connect4.model.fct_minmax;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class GameControlleur {

    private final Model_connect4 game = new Model_connect4(9, 9, 1);
    private final fct_minmax mx = new fct_minmax();

    // Profondeur modifiable
    private int profondeur = 4;

    // Joueur humain
    @PostMapping("/play/{col}")
    public Map<String, Object> play(@PathVariable int col) {
        game.gameplay(col);
        if (!game.getisPartieTerminee() && (game.mode_j == 2 || game.mode_j == 3) && game.getJoueurCourant() == 2) {
            jouerUnCoupIA();
        }
        return buildResponse();
    }

    // Reprendre
    @PostMapping("/play")
    public Map<String, Object> playGame() {
        game.reprendre();
        return buildResponse();
    }

    // Coup IA
    @PostMapping("/playIA")
    public Map<String, Object> playIA() {
        if (!game.getisPartieTerminee() && !game.pause) {
            jouerUnCoupIA();
        }
        return buildResponse();
    }

    // Calcul du score et profondeur
    @PostMapping("/analyse")
    public Map<String, Object> analyse(@RequestParam(defaultValue = "4") int profondeur) {
        // Met à jour la profondeur globale pour les prochains coups IA
        this.profondeur = Math.max(1, Math.min(12, profondeur));

        int cols = game.getCols();
        int[] scores = new int[cols];
        int[][] plateau = game.getPlateau();

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
        game.redemarrer_p();
        return buildResponse();
    }

    // Changement de mode
    @PostMapping("/setMode/{mode}")
    public Map<String, Object> setMode(@PathVariable int mode) {
        game.setmj(mode);
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
        game.mt_pause();
        Map<String, Object> resp = buildResponse();
        resp.put("message", "Pause activée");
        return resp;
    }

    // Remettre ou retire un pion
    @PostMapping("/retirer")
    public Map<String, Object> retirer() {
        game.retirer();
        return buildResponse();
    }

    @PostMapping("/remettre")
    public Map<String, Object> remettre() {
        game.remettre();
        return buildResponse();
    }

    // Etat
    @GetMapping("/state")
    public Map<String, Object> state() {
        return buildResponse();
    }

    //Jouer un coup IA
    private void jouerUnCoupIA() {
        switch (game.mode_j) {
            case 2, 4 -> game.jouervsordi();
            case 3, 5 -> {
                int col = mx.meilleurCoup(game.getPlateau(), profondeur, game.enregistrement_cp);
                if (col != -1) game.gameplay(col);
            }
        }
    }

    private Map<String, Object> buildResponse() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("board", game.getPlateau());
        resp.put("wins", getWinsArray());
        resp.put("joueurCourant", game.getJoueurCourant());
        resp.put("partieTerminee", game.getisPartieTerminee());
        resp.put("scores", new int[game.getCols()]);
        resp.put("mode", game.mode_j);
        resp.put("profondeur", profondeur);
        return resp;
    }

    private boolean[][] getWinsArray() {
        boolean[][] wins = new boolean[game.getRows()][game.getCols()];
        for (int r = 0; r < game.getRows(); r++)
            for (int c = 0; c < game.getCols(); c++)
                wins[r][c] = game.getCaseG(r, c);
        return wins;
    }
}