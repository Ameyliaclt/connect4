package com.p4.connect4.model;
public class Coup {
    private int col;
    private int ligne;
    private int joueur;

    public Coup(){}

    public Coup(int col, int ligne, int joueur){
        this.col = col;
        this.ligne = ligne;
        this.joueur = joueur;
    }

    public int getcol(){
        return col;
    }

    public int getligne(){
        return ligne;
    }

    public int getjoueur(){
        return joueur;
    }
}