package com.p4.connect4.model;
import java.util.ArrayList;
public class Sauvegarde{
    int id_partie;
    int[][] grille;
    int[][] grilleSym;
    int id_symetrie;
    int col;
    int confiance;
    int joueurCourant;
    int num_p;
    int modeJeu;
    boolean partieterminee;
    boolean [][] casesGagnantes;
    ArrayList<Coup> historique_coup;
    int canonique;
}