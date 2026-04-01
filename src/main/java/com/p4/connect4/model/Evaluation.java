package com.p4.connect4.model;

import org.springframework.stereotype.Component;

@Component
public class Evaluation {

    public int evaluer(int[][] tabl, int joueur){
        int score = 0;

        score += evaluer_hz(tabl, joueur);
        score += evaluer_vt(tabl, joueur);
        score += evaluer_diagd(tabl, joueur);
        score += evaluer_diagm(tabl, joueur);

        int centre = tabl[0].length / 2;
        for (int row = 0; row < tabl.length; row++) {
            if (tabl[row][centre] == joueur) score += 6;
        }

        return score;
    }

    public int evaluer_hz(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3 - joueur;

        for(int row = 0; row < tabl.length; row++){
            for(int col = 0; col < tabl[0].length - 3; col++){

                int count_j = 0;
                int count_a = 0;

                for(int k = 0; k < 4; k++){
                    int cell = tabl[row][col + k];
                    if(cell == joueur) count_j++;
                    else if(cell == adversaire) count_a++;
                }

                score += scoreFenetre(count_j, count_a);
            }
        }
        return score;
    }

    public int evaluer_vt(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3 - joueur;

        for(int row = 0; row < tabl.length - 3; row++){
            for(int col = 0; col < tabl[0].length; col++){

                int count_j = 0;
                int count_a = 0;

                for(int k = 0; k < 4; k++){
                    int cell = tabl[row + k][col];
                    if(cell == joueur) count_j++;
                    else if(cell == adversaire) count_a++;
                }

                score += scoreFenetre(count_j, count_a);
            }
        }
        return score;
    }

    public int evaluer_diagd(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3 - joueur;

        for(int row = 0; row < tabl.length - 3; row++){
            for(int col = 0; col < tabl[0].length - 3; col++){

                int count_j = 0;
                int count_a = 0;

                for(int k = 0; k < 4; k++){
                    int cell = tabl[row + k][col + k];
                    if(cell == joueur) count_j++;
                    else if(cell == adversaire) count_a++;
                }

                score += scoreFenetre(count_j, count_a);
            }
        }
        return score;
    }

    public int evaluer_diagm(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3 - joueur;

        for(int row = 3; row < tabl.length; row++){
            for(int col = 0; col < tabl[0].length - 3; col++){

                int count_j = 0;
                int count_a = 0;

                for(int k = 0; k < 4; k++){
                    int cell = tabl[row - k][col + k];
                    if(cell == joueur) count_j++;
                    else if(cell == adversaire) count_a++;
                }

                score += scoreFenetre(count_j, count_a);
            }
        }
        return score;
    }

    private int scoreFenetre(int count_j, int count_a){

        if(count_j > 0 && count_a > 0) return 0;

        if(count_a == 0){
            if(count_j == 1) return 1;
            if(count_j == 2) return 10;
            if(count_j == 3) return 1000;
            if(count_j == 4) return 100000;
        }

        if(count_j == 0){
            if(count_a == 1) return -1;
            if(count_a == 2) return -10;
            if(count_a == 3) return -2000;
            if(count_a == 4) return -100000;
        }

        return 0;
    }
}


