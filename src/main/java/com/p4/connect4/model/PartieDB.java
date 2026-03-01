package com.p4.connect4.model;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Repository
public class PartieDB {

    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();
    private final Type typeCoup = new TypeToken<ArrayList<Coup>>(){}.getType();

    public PartieDB(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Recherche du meilleur coup dans la BDD
    public int chercherMeilleurCoup(ArrayList<Coup> historiqueActuel) {
        if (historiqueActuel == null || historiqueActuel.isEmpty()) return -1;

        String sql = """
            SELECT pa.cols, pa.confiance, pa.historique_coup
            FROM p01.partie pa
            WHERE jsonb_array_length(pa.historique_coup) > ?
            ORDER BY pa.confiance DESC
        """;

        int taille = historiqueActuel.size();

        List<int[]> resultats = jdbc.query(sql, (rs, rowNum) -> {
            ArrayList<Coup> histBD = gson.fromJson(rs.getString("historique_coup"), typeCoup);
            if (histBD == null) return null;

            boolean match = true;
            for (int i = 0; i < taille; i++) {
                if (historiqueActuel.get(i).getcol() != histBD.get(i).getcol()) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return new int[]{ histBD.get(taille).getcol(), rs.getInt("confiance") };
            }
            return null;
        }, taille);

        for (int[] r : resultats) {
            if (r != null) {
                System.out.printf("📚 Séquence trouvée (confiance=%d) → colonne %d%n", r[1], r[0]);
                return r[0];
            }
        }

        return -1;
    }

    // Sauvegarder dans la BDD
    public void sauvegarder(Sauvegarde s) {
        String sqlPos = "SELECT p01.insert_position(?::jsonb)";
        String sqlPrt = "INSERT INTO p01.partie(num_p, grille, mode_j, partie_term, cases_g, joueur_c, historique_coup, id_pos, cols, confiance) " +
                        "VALUES(?, ?::jsonb, ?, ?, ?::json, ?, ?::jsonb, ?, ?, ?)";

        try {
            Integer idPos = jdbc.queryForObject(sqlPos, Integer.class, gson.toJson(s.grille));

            jdbc.update(sqlPrt,
                s.num_p,
                gson.toJson(s.grille),
                s.modeJeu,
                s.partieterminee,
                gson.toJson(s.casesGagnantes),
                s.joueurCourant,
                gson.toJson(s.historique_coup),
                idPos,
                s.col,
                s.confiance
            );
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new RuntimeException("Cette partie existe déjà");
        }
    }

    // Charger une sauvegarde par id_pos
    public Sauvegarde charger_p(int id) {
        String sql = "SELECT * FROM p01.partie WHERE id_pos = ?";

        List<Sauvegarde> result = jdbc.query(sql, (rs, rowNum) -> {
            Sauvegarde s = new Sauvegarde();
            s.num_p = rs.getInt("num_p");
            s.grille = gson.fromJson(rs.getString("grille"), int[][].class);
            s.modeJeu = rs.getInt("mode_j");
            s.partieterminee = rs.getBoolean("partie_term");
            s.casesGagnantes = gson.fromJson(rs.getString("cases_g"), boolean[][].class);
            s.joueurCourant = rs.getInt("joueur_c");
            s.historique_coup = gson.fromJson(rs.getString("historique_coup"), typeCoup);
            return s;
        }, id);

        return result.isEmpty() ? new Sauvegarde() : result.get(0);
    }

    // Charger une partie avec sa symétrie
    public Sauvegarde charger_ps(int id) {
        String sql = "SELECT pa.*, p.grille AS grille_pos, p.id_symetrie, ps.grille AS grille_sym " +
                     "FROM p01.partie pa " +
                     "JOIN p01.\"position\" p ON pa.id_pos = p.id_p " +
                     "LEFT JOIN p01.\"position\" ps ON p.id_symetrie = ps.id_p " +
                     "WHERE pa.id_partie = ?";

        List<Sauvegarde> result = jdbc.query(sql, (rs, rowNum) -> {
            Sauvegarde s = new Sauvegarde();
            s.num_p = rs.getInt("num_p");
            s.modeJeu = rs.getInt("mode_j");
            s.partieterminee = rs.getBoolean("partie_term");
            s.casesGagnantes = gson.fromJson(rs.getString("cases_g"), boolean[][].class);
            s.joueurCourant = rs.getInt("joueur_c");
            s.historique_coup = gson.fromJson(rs.getString("historique_coup"), typeCoup);
            s.grille = gson.fromJson(rs.getString("grille_pos"), int[][].class);

            String grilleSym = rs.getString("grille_sym");
            if (grilleSym != null) {
                s.grilleSym = gson.fromJson(grilleSym, int[][].class);
            }
            return s;
        }, id);

        return result.isEmpty() ? new Sauvegarde() : result.get(0);
    }

    // Charger toutes les sauvegardes
    public List<Sauvegarde> charger_All() {
        String sql = "SELECT pa.id_partie, p.id_canonique, p.id_symetrie, " +
                     "p.grille AS grille_pos, ps.grille AS grille_sym " +
                     "FROM p01.partie pa " +
                     "JOIN p01.\"position\" p ON pa.id_pos = p.id_p " +
                     "LEFT JOIN p01.\"position\" ps ON p.id_symetrie = ps.id_p " +
                     "ORDER BY pa.id_partie";

        return jdbc.query(sql, (rs, rowNum) -> {
            Sauvegarde s = new Sauvegarde();
            s.id_partie = rs.getInt("id_partie");
            s.canonique = rs.getInt("id_canonique");
            s.id_symetrie = rs.getInt("id_symetrie");

            String grillePos = rs.getString("grille_pos");
            if (grillePos != null) s.grille = gson.fromJson(grillePos, int[][].class);

            String grilleSym = rs.getString("grille_sym");
            if (grilleSym != null) s.grilleSym = gson.fromJson(grilleSym, int[][].class);

            return s;
        });
    }
}