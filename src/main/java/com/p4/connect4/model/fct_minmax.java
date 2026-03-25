package com.p4.connect4.model;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

@Component
public class fct_minmax {
    PartieDB db;
    Evaluation eval;

    public fct_minmax(PartieDB db, Evaluation eval){
        this.db = db;
        this.eval = eval;
    }

    // ===================== MÉTHODES EXISTANTES =====================

    public int meilleurCoup(int[][] plateau, int profondeur, ArrayList<Coup> historique) {
    
    int coupBD = db.chercherMeilleurCoup(historique);
    
    // ── Calcul minimax en temps réel ──────────────────────────────
    int meilleurScoreMM = Integer.MIN_VALUE;
    int meilleurColonneMM = -1;

    for (int col = 0; col < plateau[0].length; col++) {
        int[][] copie = copierPlateau(plateau);
        int row = simulCoup(copie, col, 2);
        if (row != -1) {
            int score = minmax(copie, profondeur - 1, false);
            if (score > meilleurScoreMM) {
                meilleurScoreMM = score;
                meilleurColonneMM = col;
            }
        }
    }

    // ── Pas de coup BDD : on retourne minimax directement ─────────
    if (coupBD == -1) return meilleurColonneMM;

    // ── Coup BDD trouvé : on évalue son score minimax ─────────────
    int scoreBD = Integer.MIN_VALUE;
    int[][] copieBD = copierPlateau(plateau);
    int rowBD = simulCoup(copieBD, coupBD, 2);
    if (rowBD != -1) {
        scoreBD = minmax(copieBD, profondeur - 1, false);
    }

    System.out.println("[meilleurCoup] BDD col=" + coupBD + " score=" + scoreBD
            + " | MM col=" + meilleurColonneMM + " score=" + meilleurScoreMM);

    // ── Règles de priorité ────────────────────────────────────────

    // 1. Victoire immédiate BDD → toujours prendre
    if (scoreBD >= 100000) return coupBD;

    // 2. Victoire immédiate minimax → toujours prendre
    if (meilleurScoreMM >= 100000) return meilleurColonneMM;

    // 3. Le coup BDD mène à une défaite forcée → ignorer la BDD
    if (scoreBD <= -100000) return meilleurColonneMM;

    // 4. Comparer les deux scores avec un seuil de tolérance
    //    Si le minimax est significativement meilleur (>15%), on le préfère
    //    Sinon on fait confiance à la BDD (elle peut avoir une info positionnelle
    //    que le minimax à profondeur limitée ne voit pas)
    double seuil = 0.15; // 15% de marge
    int ref = Math.max(Math.abs(meilleurScoreMM), 1);
    double ecart = (double)(meilleurScoreMM - scoreBD) / ref;

    if (ecart > seuil) {
        System.out.println("[meilleurCoup] minimax préféré (écart=" + String.format("%.2f", ecart) + ")");
        return meilleurColonneMM;
    }

    System.out.println("[meilleurCoup] BDD conservée (écart=" + String.format("%.2f", ecart) + ")");
    return coupBD;
}

    public int minmax(int[][] tabl, int prof, boolean maxplayer) {
        if (victoire(tabl, 2))  return 100000 + prof;
        if (victoire(tabl, 1))  return -100000 - prof;
        if (plateauPlein(tabl)) return 0;
        if (prof == 0)          return eval.evaluer(tabl, 2);

        if (maxplayer) {
            int meilleurScore = Integer.MIN_VALUE;
            for (int col = 0; col < tabl[0].length; col++) {
                int row = simulCoup(tabl, col, 2);
                if (row != -1) {
                    int score = minmax(tabl, prof - 1, false);
                    annulCoup(tabl, row, col);
                    if (score > meilleurScore) meilleurScore = score;
                }
            }
            return meilleurScore;
        } else {
            int meilleurScore = Integer.MAX_VALUE;
            for (int col = 0; col < tabl[0].length; col++) {
                int row = simulCoup(tabl, col, 1);
                if (row != -1) {
                    int score = minmax(tabl, prof - 1, true);
                    annulCoup(tabl, row, col);
                    if (score < meilleurScore) meilleurScore = score;
                }
            }
            return meilleurScore;
        }
    }

    // ===================== PRÉDICTION =====================

    /**
     * Résultat d'une prédiction :
     *  - gagnant  : 1, 2, ou 0 (match nul)
     *  - coups    : nombre de coups minimum avant la fin (-1 si inconnu)
     *  - certain  : true si la victoire est forcée (l'adversaire ne peut pas l'éviter)
     */
    public static class Prediction {
        public int gagnant;   // 1, 2, 0
        public int coups;     // nb de coups restants
        public boolean certain;
        public Prediction(int gagnant, int coups, boolean certain) {
            this.gagnant = gagnant;
            this.coups   = coups;
            this.certain = certain;
        }
    }

    /**
     * Prédit l'issue de la partie depuis l'état actuel du plateau.
     *
     * @param plateau   état courant
     * @param profondeur profondeur de recherche
     * @param joueurCourant le joueur qui doit jouer maintenant (1 ou 2)
     */
    public Prediction predire(int[][] plateau, int profondeur, int joueurCourant) {
        // Vérifier d'abord si une victoire est déjà présente sur le plateau
        if (victoire(plateau, 1)) return new Prediction(1, 0, true);
        if (victoire(plateau, 2)) return new Prediction(2, 0, true);
        if (plateauPlein(plateau)) return new Prediction(0, 0, true);

        // Lancer la recherche minimax avec comptage de coups
        // On joue du point de vue du joueur 2 (IA) mais on adapte selon joueurCourant
        boolean iaJoueMaintenant = (joueurCourant == 2);
        int[] resultat = minmaxPrediction(plateau, profondeur, iaJoueMaintenant, 0);

        // resultat[0] = score final, resultat[1] = profondeur à laquelle on a trouvé la fin
        int score      = resultat[0];
        int coupsTrouves = resultat[1];

        if (score > 50000) {
            // Joueur 2 gagne de façon forcée
            return new Prediction(2, coupsTrouves, true);
        } else if (score < -50000) {
            // Joueur 1 gagne de façon forcée
            return new Prediction(1, coupsTrouves, true);
        } else if (score == 0 && plateauPlein(plateau)) {
            return new Prediction(0, coupsTrouves, true);
        } else {
            // Pas de victoire forcée trouvée dans la profondeur donnée
            // On retourne le favori selon le score d'évaluation
            if (score > 0) {
                return new Prediction(2, coupsTrouves, false);
            } else if (score < 0) {
                return new Prediction(1, coupsTrouves, false);
            } else {
                return new Prediction(0, coupsTrouves, false);
            }
        }
    }

    /**
     * Minimax qui retourne [score, coupsRestants].
     * coupsRestants = profondeurMax - profondeurActuelle au moment de la fin trouvée.
     */
    private int[] minmaxPrediction(int[][] tabl, int prof, boolean maxplayer, int coupJoues) {
        if (victoire(tabl, 2))  return new int[]{100000 + prof, coupJoues};
        if (victoire(tabl, 1))  return new int[]{-100000 - prof, coupJoues};
        if (plateauPlein(tabl)) return new int[]{0, coupJoues};
        if (prof == 0)          return new int[]{eval.evaluer(tabl, 2), coupJoues};

        if (maxplayer) {
            int meilleurScore = Integer.MIN_VALUE;
            int meilleursCoups = coupJoues;
            for (int col = 0; col < tabl[0].length; col++) {
                int row = simulCoup(tabl, col, 2);
                if (row != -1) {
                    int[] res = minmaxPrediction(tabl, prof - 1, false, coupJoues + 1);
                    annulCoup(tabl, row, col);
                    if (res[0] > meilleurScore) {
                        meilleurScore  = res[0];
                        meilleursCoups = res[1];
                    }
                }
            }
            return new int[]{meilleurScore, meilleursCoups};
        } else {
            int meilleurScore = Integer.MAX_VALUE;
            int meilleursCoups = coupJoues;
            for (int col = 0; col < tabl[0].length; col++) {
                int row = simulCoup(tabl, col, 1);
                if (row != -1) {
                    int[] res = minmaxPrediction(tabl, prof - 1, true, coupJoues + 1);
                    annulCoup(tabl, row, col);
                    if (res[0] < meilleurScore) {
                        meilleurScore  = res[0];
                        meilleursCoups = res[1];
                    }
                }
            }
            return new int[]{meilleurScore, meilleursCoups};
        }
    }

    // ===================== UTILITAIRES =====================

    private int simulCoup(int[][] tabl, int col, int joueur) {
        for (int row = tabl.length - 1; row >= 0; row--) {
            if (tabl[row][col] == 0) {
                tabl[row][col] = joueur;
                return row;
            }
        }
        return -1;
    }

    private void annulCoup(int[][] tabl, int row, int col) {
        tabl[row][col] = 0;
    }

    public int[][] copierPlateau(int[][] original) {
        int[][] copie = new int[original.length][original[0].length];
        for (int l = 0; l < original.length; l++)
            for (int c = 0; c < original[0].length; c++)
                copie[l][c] = original[l][c];
        return copie;
    }

    private boolean victoire(int[][] plateau, int joueur) {
        int rows = plateau.length;
        int cols = plateau[0].length;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c]==joueur && plateau[r][c+1]==joueur &&
                    plateau[r][c+2]==joueur && plateau[r][c+3]==joueur) return true;
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows - 3; r++)
                if (plateau[r][c]==joueur && plateau[r+1][c]==joueur &&
                    plateau[r+2][c]==joueur && plateau[r+3][c]==joueur) return true;
        for (int r = 0; r < rows - 3; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c]==joueur && plateau[r+1][c+1]==joueur &&
                    plateau[r+2][c+2]==joueur && plateau[r+3][c+3]==joueur) return true;
        for (int r = 3; r < rows; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c]==joueur && plateau[r-1][c+1]==joueur &&
                    plateau[r-2][c+2]==joueur && plateau[r-3][c+3]==joueur) return true;
        return false;
    }

    private boolean plateauPlein(int[][] plateau) {
        for (int col = 0; col < plateau[0].length; col++)
            if (plateau[0][col] == 0) return false;
        return true;
    }

    public int getScoreCol(int[][] tabl, int prof, int col) {
        int[][] copie = copierPlateau(tabl);
        int row = simulCoup(copie, col, 2);
        if (row == -1) return -500000;
        return minmax(copie, prof - 1, false);
    }

    public double calculerConfiance(int[][] plateau, int profondeur) {
        int bestScore  = Integer.MIN_VALUE;
        int secondBest = Integer.MIN_VALUE;
        for (int col = 0; col < plateau[0].length; col++) {
            int[][] copie = copierPlateau(plateau);
            int row = simulCoup(copie, col, 2);
            if (row != -1) {
                int score = minmax(copie, profondeur - 1, false);
                if (score > bestScore) { secondBest = bestScore; bestScore = score; }
                else if (score > secondBest) { secondBest = score; }
            }
        }
        if (bestScore == Integer.MIN_VALUE) return 0;
        double marge;
        if (secondBest == Integer.MIN_VALUE) { marge = 1.0; }
        else {
            int ref = Math.max(Math.abs(bestScore), 1);
            marge = Math.max(0, Math.min(1, (double)(bestScore - secondBest) / ref));
        }
        double stabilite;
        if (profondeur >= 3) {
            int scoreNmoins2 = minmax(plateau, profondeur - 2, false);
            int ref = Math.max(Math.abs(bestScore), 1);
            stabilite = Math.max(0, Math.min(1, 1.0 - (double)Math.abs(bestScore - scoreNmoins2) / ref));
        } else { stabilite = 0.5; }
        double bonus   = Math.abs(bestScore) >= 100000 ? 1.0 : 0.0;
        double profRel = (double)(profondeur - 1) / profondeur;
        return Math.max(0, Math.min(1, 0.35*marge + 0.25*stabilite + 0.20*bonus + 0.20*profRel));
    }

    public int echelleConfiance(double c) {
        if (c < 0.10) return 0;
        if (c < 0.25) return 1;
        if (c < 0.45) return 2;
        if (c < 0.65) return 3;
        if (c < 0.85) return 4;
        return 5;
    }
}