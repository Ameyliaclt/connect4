package com.p4.connect4.model;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

@Component
public class fct_minmax {
    PartieDB db;
    Evaluation eval;

    public fct_minmax(PartieDB db, Evaluation eval) {
        this.db = db;
        this.eval = eval;
    }

    /**
     * Calcule l'ordre des colonnes pour explorer le centre en premier.
     * Pour 7 colonnes : [3, 4, 2, 5, 1, 6, 0]
     */
    private int[] getOrdreColonnes(int nbCols) {
        int[] ordre = new int[nbCols];
        int mid = nbCols / 2;
        ordre[0] = mid;
        int count = 1;
        for (int i = 1; i <= mid; i++) {
            if (mid + i < nbCols) {
                ordre[count++] = mid + i;
            }
            if (mid - i >= 0) {
                ordre[count++] = mid - i;
            }
        }
        return ordre;
    }

    public int meilleurCoup(int[][] plateau, int profondeur, ArrayList<Coup> historique) {
        int nbCols = plateau[0].length;
        int[] ordre = getOrdreColonnes(nbCols);

        // 1. Victoire immédiate
        for (int col : ordre) {
            int[][] copie = copierPlateau(plateau);
            int row = simulCoup(copie, col, 2);
            if (row != -1 && victoire(copie, 2)) return col;
        }

        // 2. Blocage immédiat
        for (int col : ordre) {
            int[][] copie = copierPlateau(plateau);
            int row = simulCoup(copie, col, 1);
            if (row != -1 && victoire(copie, 1)) return col;
        }

        int coupBD = db.chercherMeilleurCoup(historique);
        int meilleurScoreMM = Integer.MIN_VALUE;
        int meilleurColonneMM = -1;

        // 3. Recherche MinMax avec ordre optimisé
        for (int col : ordre) {
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

        if (coupBD == -1) return meilleurColonneMM;

        // Comparaison avec Base de données
        int scoreBD = Integer.MIN_VALUE;
        int[][] copieBD = copierPlateau(plateau);
        int rowBD = simulCoup(copieBD, coupBD, 2);
        if (rowBD != -1) {
            scoreBD = minmax(copieBD, profondeur - 1, false);
        }

        if (scoreBD >= 100000) return coupBD;
        if (meilleurScoreMM >= 100000) return meilleurColonneMM;
        if (scoreBD <= -100000) return meilleurColonneMM;

        double seuil = 0.05;
        int ref = Math.max(Math.abs(meilleurScoreMM), 1);
        double ecart = (double) (meilleurScoreMM - scoreBD) / ref;

        return (ecart > seuil) ? meilleurColonneMM : coupBD;
    }

    public int minmax(int[][] tabl, int prof, boolean maxplayer) {
        if (victoire(tabl, 2)) return 100000 + prof;
        if (victoire(tabl, 1)) return -100000 - prof;
        if (plateauPlein(tabl)) return 0;
        if (prof == 0) return eval.evaluer(tabl, 2);

        int[] ordre = getOrdreColonnes(tabl[0].length);

        if (maxplayer) {
            int meilleurScore = Integer.MIN_VALUE;
            for (int col : ordre) {
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
            for (int col : ordre) {
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

    public static class Prediction {
        public int gagnant;
        public int coups;
        public boolean certain;

        public Prediction(int gagnant, int coups, boolean certain) {
            this.gagnant = gagnant;
            this.coups = coups;
            this.certain = certain;
        }
    }

    public Prediction predire(int[][] plateau, int profondeur, int joueurCourant) {
        if (victoire(plateau, 1)) return new Prediction(1, 0, true);
        if (victoire(plateau, 2)) return new Prediction(2, 0, true);
        if (plateauPlein(plateau)) return new Prediction(0, 0, true);

        boolean iaJoueMaintenant = (joueurCourant == 2);
        int[] resultat = minmaxPrediction(plateau, profondeur, iaJoueMaintenant, 0);

        int score = resultat[0];
        int coupsTrouves = resultat[1];

        if (score > 50000) return new Prediction(2, coupsTrouves, true);
        if (score < -50000) return new Prediction(1, coupsTrouves, true);
        
        return new Prediction(score > 0 ? 2 : (score < 0 ? 1 : 0), coupsTrouves, false);
    }

    private int[] minmaxPrediction(int[][] tabl, int prof, boolean maxplayer, int coupJoues) {
        if (victoire(tabl, 2)) return new int[]{100000 + prof, coupJoues};
        if (victoire(tabl, 1)) return new int[]{-100000 - prof, coupJoues};
        if (plateauPlein(tabl)) return new int[]{0, coupJoues};
        if (prof == 0) return new int[]{eval.evaluer(tabl, 2), coupJoues};

        int[] ordre = getOrdreColonnes(tabl[0].length);

        if (maxplayer) {
            int meilleurScore = Integer.MIN_VALUE;
            int meilleursCoups = coupJoues;
            for (int col : ordre) {
                int row = simulCoup(tabl, col, 2);
                if (row != -1) {
                    int[] res = minmaxPrediction(tabl, prof - 1, false, coupJoues + 1);
                    annulCoup(tabl, row, col);
                    if (res[0] > meilleurScore) {
                        meilleurScore = res[0];
                        meilleursCoups = res[1];
                    }
                }
            }
            return new int[]{meilleurScore, meilleursCoups};
        } else {
            int meilleurScore = Integer.MAX_VALUE;
            int meilleursCoups = coupJoues;
            for (int col : ordre) {
                int row = simulCoup(tabl, col, 1);
                if (row != -1) {
                    int[] res = minmaxPrediction(tabl, prof - 1, true, coupJoues + 1);
                    annulCoup(tabl, row, col);
                    if (res[0] < meilleurScore) {
                        meilleurScore = res[0];
                        meilleursCoups = res[1];
                    }
                }
            }
            return new int[]{meilleurScore, meilleursCoups};
        }
    }

    public int getScoreCol(int[][] tabl, int prof, int col) {
        int[][] copie = copierPlateau(tabl);
        int row = simulCoup(copie, col, 2);
        if (row == -1) return -500000;
        return minmax(copie, prof - 1, false);
    }

    public double calculerConfiance(int[][] plateau, int profondeur) {
        int nbCols = plateau[0].length;
        int[] ordre = getOrdreColonnes(nbCols);
        int bestScore = Integer.MIN_VALUE;
        int secondBest = Integer.MIN_VALUE;

        for (int col : ordre) {
            int[][] copie = copierPlateau(plateau);
            int row = simulCoup(copie, col, 2);
            if (row != -1) {
                int score = minmax(copie, profondeur - 1, false);
                if (score > bestScore) {
                    secondBest = bestScore;
                    bestScore = score;
                } else if (score > secondBest) {
                    secondBest = score;
                }
            }
        }
        
        if (bestScore == Integer.MIN_VALUE) return 0;
        
        int ref = Math.max(Math.abs(bestScore), 1);
        double marge = (secondBest == Integer.MIN_VALUE) ? 1.0 : Math.max(0, Math.min(1, (double) (bestScore - secondBest) / ref));
        
        double stabilite = 0.5;
        if (profondeur >= 3) {
            int scoreNmoins2 = minmax(plateau, profondeur - 2, false);
            stabilite = Math.max(0, Math.min(1, 1.0 - (double) Math.abs(bestScore - scoreNmoins2) / ref));
        }

        double bonus = Math.abs(bestScore) >= 100000 ? 1.0 : 0.0;
        double profRel = (double) (profondeur - 1) / profondeur;

        return Math.max(0, Math.min(1, 0.35 * marge + 0.25 * stabilite + 0.20 * bonus + 0.20 * profRel));
    }

    // --- Méthodes utilitaires ---

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
            System.arraycopy(original[l], 0, copie[l], 0, original[0].length);
        return copie;
    }

    private boolean victoire(int[][] plateau, int joueur) {
        int rows = plateau.length;
        int cols = plateau[0].length;
        // Horizontal
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c] == joueur && plateau[r][c + 1] == joueur && plateau[r][c + 2] == joueur && plateau[r][c + 3] == joueur) return true;
        // Vertical
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows - 3; r++)
                if (plateau[r][c] == joueur && plateau[r + 1][c] == joueur && plateau[r + 2][c] == joueur && plateau[r + 3][c] == joueur) return true;
        // Diagonale descendante
        for (int r = 0; r < rows - 3; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c] == joueur && plateau[r + 1][c + 1] == joueur && plateau[r + 2][c + 2] == joueur && plateau[r + 3][c + 3] == joueur) return true;
        // Diagonale ascendante
        for (int r = 3; r < rows; r++)
            for (int c = 0; c < cols - 3; c++)
                if (plateau[r][c] == joueur && plateau[r - 1][c + 1] == joueur && plateau[r - 2][c + 2] == joueur && plateau[r - 3][c + 3] == joueur) return true;
        return false;
    }

    private boolean plateauPlein(int[][] plateau) {
        for (int col = 0; col < plateau[0].length; col++)
            if (plateau[0][col] == 0) return false;
        return true;
    }
}