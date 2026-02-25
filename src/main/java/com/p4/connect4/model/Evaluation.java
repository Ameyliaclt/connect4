package com.p4.connect4.model;
public class Evaluation{
    public int evaluer(int[][]tabl, int joueur){
        int score = 0;
        score += evaluer_hz(tabl, joueur);
        score += evaluer_vt(tabl,joueur);
        score += evaluer_diagd(tabl,joueur);
        score += evaluer_diagm(tabl,joueur);
        return score;
    }

    public int evaluer_hz(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3 - joueur;

        for(int row = 0; row<tabl.length;row++){
            for(int col = 0; col<tabl[0].length-3;col++){
                int count_cjoueur=0;
                int count_cadversaire=0;
                int count_cvide=0;

                for(int k = 0; k<4;k++){
                    int cell = tabl[row][col+k];
                    if(cell==joueur){
                        count_cjoueur++;
                    } else if(cell == adversaire){
                        count_cadversaire++;
                    } else {
                        count_cvide++;
                    }
                }
                if(count_cadversaire==0){
                    if(count_cjoueur==1) score+=1;
                    else if (count_cjoueur==2) score += 5;
                    else if (count_cjoueur == 3)score += 20;
                    else if (count_cjoueur==4)score += 999;
                }

                if(count_cjoueur==0){
                    if(count_cadversaire==2)score -=5;
                    else if(count_cadversaire==3)score-=20;
                    else if(count_cadversaire==4)score-=999;
                }
            }
        }
        return score;
    }

    public int evaluer_vt(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3-joueur;

         for(int row=0; row<tabl.length-3;row++){
            for(int col =0; col<tabl[0].length;col++){
                int count_cj =0;
                int count_ca = 0;
                int count_cv = 0;

                for(int k=0;k<4;k++){
                    int cell = tabl[row+k][col];
                    if(cell==joueur){
                        count_cj++;
                    } else if(cell == adversaire){
                        count_ca++;
                    }else {
                        count_cv++;
                    }
                }
                if(count_ca == 0){
                    if(count_cj==1) score +=1;
                    else if(count_cj==2) score +=5;
                    else if(count_cj==3)score +=20;
                    else if(count_cj==4)score+=999;
                }
                if(count_cj==0){
                    if(count_ca==2) score -= 5;
                    else if(count_ca==3) score -=20;
                    else if(count_ca==4)score -=999;
                }
            }
        }
        return score;
    }

    public int evaluer_diagd(int[][] tabl, int joueur){
        int score = 0;
        int adversaire = 3-joueur;
        for(int row =0; row<tabl.length-3;row++){
            for(int col=0; col<tabl[0].length-3;col++){
                int count_cj = 0;
                int count_ca=0;
                int count_cv=0;
                for(int k=0;k<4;k++){
                    int cell = tabl[row+k][col+k];
                    if(cell==joueur){
                        count_cj++;
                    }else if(cell==adversaire){
                        count_ca++;
                    }else {
                        count_cv++;
                    }
                }
                if(count_ca == 0){
                    if(count_cj==1) score +=1;
                    else if(count_cj==2) score +=5;
                    else if(count_cj==3)score +=20;
                    else if(count_cj==4)score+=999;
                }
                if(count_cj==0){
                    if(count_ca==2) score -= 5;
                    else if(count_ca==3) score -=20;
                    else if(count_ca==4)score -=999;
                }
            }
        }
        return score;
    }

    public int evaluer_diagm(int[][] tabl,int joueur){
        int score=0;
        int adversaire = 3-joueur;
        for(int row =3; row<tabl.length;row++){
            for(int col=0;col<tabl[0].length-3;col++){
                int count_cj=0;
                int count_ca=0;
                int count_cv=0;

                for(int k=0;k<4;k++){
                    int cell = tabl[row-k][col+k];
                    if(cell==joueur){
                        count_cj++;
                    }else if(cell==adversaire){
                        count_ca++;
                    }else{
                        count_cv++;
                    }
                }
                if(count_ca == 0){
                    if(count_cj==1) score +=1;
                    else if(count_cj==2) score +=5;
                    else if(count_cj==3)score +=20;
                    else if(count_cj==4)score+=999;
                }
                if(count_cj==0){
                    if(count_ca==2) score -= 5;
                    else if(count_ca==3) score -=20;
                    else if(count_ca==4)score -=999;
                }
            }
        }
        return score;
    }
}

