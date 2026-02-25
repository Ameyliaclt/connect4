package com.p4.connect4.model;
import java.util.ArrayList;

public class fct_minmax {

    Evaluation eval = new Evaluation(); 
    public int meilleurCoup(int[][] plateau, int profondeur, ArrayList<Coup> historique) {
        System.out.println("algo minmax enclenché");
        int meilleurScore = Integer.MIN_VALUE;
        int meilleurColonne = -1;

        for (int col = 0; col < plateau[0].length; col++) {
            int[][] copie = copierPlateau(plateau);
            int row = simulCoup(copie, col, 2);
            if (row != -1) {
                int score = minmax(copie, profondeur - 1, false);
                if (score > meilleurScore) {
                    meilleurScore = score;
                    meilleurColonne = col;
                }
            }
        }

        if (meilleurColonne == -1) {
            for (int col = 0; col < plateau[0].length; col++) {
                if (plateau[0][col] == 0) { meilleurColonne = col; break; }
            }
        }
        return meilleurColonne;
    }

    public int minmax(int[][] tabl, int prof, boolean maxplayer) {
        if (victoire(tabl, 2))   return 100000 + prof;
        if (victoire(tabl, 1))   return -100000 - prof;
        if (plateauPlein(tabl))  return 0;
        if (prof == 0)           return eval.evaluer(tabl, 2);

        if (maxplayer) {
            int meilleurScore = Integer.MIN_VALUE;
            for (int col = 0; col < tabl[0].length; col++) {
                int row = simulCoup(tabl, col, 2);
                if (row != -1) {
                    int score = minmax(tabl, prof - 1, false);
                    annulCoup(tabl, row, col); // FIX 2 : on passe row précis
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
                    annulCoup(tabl, row, col); // FIX 2 : on passe row précis
                    if (score < meilleurScore) meilleurScore = score;
                }
            }
            return meilleurScore;
        }
    }

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