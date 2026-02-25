package com.p4.connect4.model;
import java.util.ArrayList;
import java.util.Random;
public class Model_connect4 {
    int num_partie = 1;
    public int confiance = 1;
    int rows;
    int cols;
    int [][] tabl;
    public boolean pause;
    boolean[][] casesG;
    int joueurCourant;
    boolean partieTerminee;
    public int mode_j;
    public ArrayList<Coup> enregistrement_cp = new ArrayList<>();
    ArrayList<Coup> cp_annules = new ArrayList<>();
    boolean rm = false;

    public Model_connect4(int rows, int cols, int startPlayer){
        this.rows = rows;
        this.cols = cols;
        this.joueurCourant = startPlayer;
        tabl = new int[rows][cols];
        casesG = new boolean[rows][cols];
        for(int i = 0; i < tabl.length; i++){
            for(int j = 0; j<tabl[i].length;j++){
                tabl[i][j] = 0;
            }
        }
    }
    
    public boolean gameplay (int col){
        if(!partieTerminee && pause == false){
            //renitial les cases gagnantes
            for(int r =0; r< rows; r++){
                for(int c = 0; c < cols; c++){
                    casesG[r][c] = false;
                }
            }
            for(int i = tabl.length - 1; i >=0; i--){
                if (tabl[i][col] == 0){
                    tabl[i][col] = joueurCourant;
                    Coup coup = new Coup(col, i , joueurCourant);
                    enregistrement_cp.add(coup);
                    if(!rm){
                        cp_annules.clear();
                    }
                    if(checkvictoire(i,col)){
                        partieTerminee = true;
                    } else if(ismtchnul()){
                        partieTerminee = true;
                    }
                    else { 
                        joueurCourant = (joueurCourant == 1) ? 2 : 1;
                    }
                    return true;
                }
            }
        }
        return false;
    }


    public void jouervsordi(){
        ArrayList<Integer> col_p = new ArrayList<>();
        for(int i = 0; i< cols;i++){
            if(tabl[0][i]==0){
                col_p.add(i);
            }
        }
        Random nb_aleatoire = new Random();
        int colonne_c = col_p.get(nb_aleatoire.nextInt(col_p.size()));
        gameplay(colonne_c);
    }

    //verifier la victoire
    public  boolean checkvictoire(int row, int col){ 
        int count = 1;
        int joueur = tabl[row][col];

        //verification horizontale
        for(int c = col - 1; c >= 0; c--){
            if(tabl[row][c] == joueur){
                count++;
            } else {
                break;
            }
        }
        for(int c = col + 1; c < cols; c++){
            if(tabl[row][c] == joueur){
                count++;
            } else {
                break;
            }
        }
        if(count >= 4){
            casesG[row][col]=true;
            for(int c = col-1; c >=0; c--){
                if(tabl[row][c] == joueur){
                    casesG[row][c]=true;
                } else {
                    break;
                }
            }
            for(int c = col+1; c < cols; c++){
                if(tabl[row][c] == joueur){
                     casesG[row][c]=true;
                } else {
                    break;
                }
            }
            return true;
        }

        //verification verticale
        count = 1 ; 
        for(int r = row +1; r < rows; r++){
            if(tabl[r][col] == joueur){
                count++;
            } else {
                break;
            }
        }
        for(int r = row - 1; r >= 0; r--){
            if(tabl[r][col] == joueur){
                count++;
            } else {
                break;
            }
        }
        if(count >= 4){
            casesG[row][col] = true;
            for(int r = row +1; r<rows;r++){
                if(tabl[r][col] == joueur){
                    casesG[r][col]=true;
                } else {
                    break;
                }
            }
            for(int r = row-1;r >= 0; r--){
                if(tabl[r][col] == joueur){
                    casesG[r][col]=true;
                }else {
                    break;
                }
            }
            return true;
        }

        //verification diagonales
        count = 1;
        for(int r = row - 1, c = col - 1; r>=0 && c>=0; r--, c--){
            if (tabl[r][c] == joueur){
                count ++;
            }else {break;}
        }
        for(int r = row +1, c = col +1; r<rows && c < cols; r++, c++){
            if(tabl[r][c] == joueur){
                count++;
            } else {break;}
            }
        if(count >=4){
            casesG[row][col]=true;
            for(int r = row - 1, c = col - 1; r>=0 && c>=0; r--, c--){
                if(tabl[r][c] == joueur){
                    casesG[r][c]=true;
                } else {
                    break;
                }
            }
            for (int r = row +1, c = col +1; r<rows && c < cols; r++, c++){
                if (tabl[r][c] == joueur){
                    casesG[r][c]=true;
                } else {
                    break;
                }
            }
            return true;
        }

        //verification diagonales inverses
        count =1;
        for(int r = row - 1, c = col + 1; r >=0 && c <cols; r--, c++){
            if (tabl [r][c] == joueur){
                count ++;
            }else {
                break;
            }
        }
        for(int r = row +1, c = col-1; r<rows && c >=0; r++, c--){
            if(tabl[r][c] ==joueur){
                count++;
            }else {
                break;
            }
        }
        if(count >=4){
            casesG[row][col]=true;
            for(int r = row - 1, c = col + 1; r >=0 && c <cols; r--, c++){
                if(tabl[r][c] == joueur ){
                    casesG[r][c]=true;
                } else {
                    break;
                }
            }
            for(int r = row +1, c = col-1; r<rows && c >=0; r++, c--){
                if(tabl [r][c] == joueur){
                    casesG[r][c] = true;
                } else {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    //verifier Match nul
    public boolean ismtchnul(){
        for(int c = 0; c < cols; c++){
            if(tabl[0][c] == 0){
                return false;
            }
        }
        return true;
    }

    
    public void retirer(){
        if(!enregistrement_cp.isEmpty()){
            Coup cp_d =enregistrement_cp.get(enregistrement_cp.size() - 1);
            enregistrement_cp.remove(cp_d);
            cp_annules.add(cp_d);
            tabl[cp_d.getligne()][cp_d.getcol()] = 0;
            joueurCourant = cp_d.getjoueur();
            for(int r =0; r< rows; r++){
                for(int c = 0; c < cols; c++){
                    casesG[r][c] = false;
                }
            }
            partieTerminee = false;
        }
    }

    public void remettre(){
        if(!cp_annules.isEmpty()){
            Coup cp_d =cp_annules.get(cp_annules.size() - 1);
            cp_annules.remove(cp_d);
            rm =true;
            gameplay(cp_d.getcol());
            rm=false;
        }
    }


    public void redemarrer_p(){
        for(int r =0; r< rows; r++){
            for(int c = 0; c < cols; c++){
                tabl[r][c] = 0;
            }
        }
        enregistrement_cp.clear();
        cp_annules.clear();
        joueurCourant = 1;
        partieTerminee= false;
        pause = false;
        for(int r =0; r< rows; r++){
            for(int c = 0; c < cols; c++){
                casesG[r][c] = false;
            }
        }
    }

    public void nouvelle_partie(){
        num_partie++;
    }

    public void mt_pause(){
        pause = true;
    }

    public void reprendre(){
        pause = false;
    }

    public int getCase(int row, int col){
        return tabl[row][col];
    }

    public int getJoueurCourant(){
        return joueurCourant;
    }

    public boolean getisPartieTerminee(){
        return partieTerminee;
    }
    public boolean getCaseG(int row, int col){
        return casesG[row][col];
    }
    public int getRows(){
        return rows;
    }
    public int getCols(){
        return cols;
    }
    public void setmj(int a){
        this.mode_j = a;
    }
    public int getnum_p(){
        return num_partie;
    }
    public Sauvegarde sauvegarder_p() {
        Sauvegarde sv = new Sauvegarde();
        sv.grille = new int[rows][cols];
        for(int i = 0; i<rows;i++){
            for(int j = 0; j<cols;j++){
                sv.grille[i][j] = tabl[i][j];
            }
        }
        sv.col = cols;
        sv.joueurCourant = joueurCourant;
        sv.num_p = num_partie;
        sv.historique_coup = new ArrayList<>();
        for(Coup cp : enregistrement_cp){
            sv.historique_coup.add(new Coup(cp.getcol(), cp.getligne(),cp.getjoueur()));
        }
        sv.modeJeu = mode_j;
        sv.confiance = confiance;
        sv.partieterminee = partieTerminee;
        sv.casesGagnantes = new boolean[rows][cols];
        for(int i = 0; i< rows; i++){
            for(int j = 0; j<cols; j++){
                sv.casesGagnantes[i][j] = casesG[i][j];
            }
        }
        return sv;
    }

    public void chargerSave(Sauvegarde save){
        this.tabl = save.grille;
        this.rows = tabl.length; 
        this.cols = tabl[0].length;
        this.confiance = save.confiance;
        this.joueurCourant = save.joueurCourant;
        this.num_partie = save.num_p;
        this.partieTerminee = save.partieterminee;
        this.casesG = save.casesGagnantes;
        if(this.casesG == null){
            this.casesG = new boolean[rows][cols];
        }
        if(save.historique_coup == null){
            this.enregistrement_cp = new ArrayList<>();
        } else { 
            this.enregistrement_cp = save.historique_coup;
        }

        if(!this.partieTerminee){
            for(int r =0; r<rows;r++){
                for(int c=0;c<cols;c++){
                    casesG[r][c]=false;
                }
            }
        }
        if(!enregistrement_cp.isEmpty()){
            Coup dernier_cp = enregistrement_cp.get(enregistrement_cp.size()-1);
            if(checkvictoire(dernier_cp.getligne(), dernier_cp.getcol())){
                partieTerminee = true;
            }else if(ismtchnul()){
                partieTerminee=true;
            }else{
                partieTerminee=false;
            }
        }
    } 

    /*public void chargerSave_fch(Sauvegarde save){
        this.tabl = save.grille;
        this.joueurCourant = save.joueurCourant;
        this.num_partie = save.num_p;
        this.partieTerminee = save.partieterminee;
        this.casesG = save.casesGagnantes;
        this.enregistrement_cp = save.historique_coup;

        if(!this.partieTerminee){
            for(int r =0; r<rows;r++){
                for(int c=0;c<cols;c++){
                    casesG[r][c]=false;
                }
            }
        }
        if(!enregistrement_cp.isEmpty()){
            Coup dernier_cp = enregistrement_cp.get(enregistrement_cp.size()-1);
            if(checkvictoire(dernier_cp.getligne(), dernier_cp.getcol())){
                partieTerminee = true;
            }else if(ismtchnul()){
                partieTerminee=true;
            }else{
                partieTerminee=false;
            }
        }
    }*/
    public int[][] getPlateau() {
        int[][] copie = new int[tabl.length][];
        for (int i = 0; i < tabl.length; i++) {
            copie[i] = tabl[i].clone();
        }
        return copie;
    }
}