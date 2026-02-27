const ROWS = 9, COLS = 9;
let dernierColonne = null;
let couleurJoueur1 = 1; // 1 = rouge (défaut), 2 = jaune

//Grille 
(function buildGrid() {
  const colNums = document.getElementById('col-numbers');
  for (let c = 0; c < COLS; c++) {
    const btn = document.createElement('button');
    btn.textContent = c + 1;
    btn.onclick = () => api.colonneCliquee(c);
    colNums.appendChild(btn);
  }
  const grille = document.getElementById('grille');
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const cell = document.createElement('div');
      cell.className = 'cell';
      cell.onclick = () => api.colonneCliquee(c);
      const pion = document.createElement('div');
      pion.className = 'pion';
      pion.id = `pion-${r}-${c}`;
      cell.appendChild(pion);
      grille.appendChild(cell);
    }
  }
  const scoresDiv = document.getElementById('scores');
  for (let c = 0; c < COLS; c++) {
    const sc = document.createElement('div');
    sc.className = 'score-cell';
    sc.id = `sc-${c}`;
    scoresDiv.appendChild(sc);
  }
})();

//Fenetre parametre
function ouvrirParametres() {
  document.getElementById('modal-overlay').style.display = 'flex';
}
function fermerParametres() {
  document.getElementById('modal-overlay').style.display = 'none';
}

// Fenetre choix couleur
let _modePending = null;

function ouvrirChoixCouleur(mode) {
  _modePending = mode;
  // Met à jour l'état visuel du sélecteur selon la couleur actuelle
  document.querySelectorAll('.color-option').forEach(btn => {
    btn.classList.toggle('selected', parseInt(btn.dataset.color) === couleurJoueur1);
  });
  document.getElementById('modal-color').style.display = 'flex';
}

function fermerChoixCouleur() {
  document.getElementById('modal-color').style.display = 'none';
  _modePending = null;
}

function confirmerCouleur() {
  const mode = _modePending;
  fermerChoixCouleur();
  if (mode !== null) _lancerMode(mode);
}

function _lancerMode(mode) {
  modeActuel = mode;
  loopIA = false;
  const token = ++loopToken;
  setModeActif(mode);
  document.getElementById('board-overlay').style.display = 'none';

  // On envoie la couleur choisie avec le mode
  apiFetch(`/api/setMode/${mode}?couleur=${couleurJoueur1}`, { method: 'POST' })
    .then(game => {
      afficherEtat(game);
      if ((mode === 4 || mode === 5) && !game.partieTerminee) {
        lancerBoucleIA(500, token);
      }
    })
    .catch(console.error);
}

function apiFetch(url, options = {}) {
  return fetch(url, {
    ...options,
    credentials: 'include'
  }).then(r => r.json());
}

//Etat
let modeActuel = 0;
let loopIA = false;
let loopToken = 0;

//API
const api = {

  setModeJ(m) {
    // Modes avec choix de couleur : 1, 2, 3
    if (m === 1 || m === 2 || m === 3) {
      ouvrirChoixCouleur(m);
    } else {
      // Modes OvsO et IAvsIA : pas de choix
      couleurJoueur1 = 1;
      _lancerMode(m);
    }
  },

  colonneCliquee(col) {
    if (modeActuel === 4 || modeActuel === 5) return;
    apiFetch(`/api/play/${col}`, { method: 'POST' })
      .then(game => {
        dernierColonne = col;
        afficherEtat(game);
        if (!game.partieTerminee && (modeActuel === 2 || modeActuel === 3)) {
          const token = loopToken;
          setTimeout(() => {
            if (loopToken !== token) return;
            jouerUnCoupIA(token);
          }, 700);
        }
      })
      .catch(console.error);
  },

  retourner() {
    apiFetch('/api/retirer', { method: 'POST' })
      .then(afficherEtat)
      .catch(console.error);
  },

  remettre() {
    apiFetch('/api/remettre', { method: 'POST' })
      .then(afficherEtat)
      .catch(console.error);
  },

  pause() {
    loopIA = false;
    loopToken++;
    apiFetch('/api/pause', { method: 'POST' })
      .then(game => afficherMessage(game.message || 'Pause'))
      .catch(console.error);
  },

  play() {
    const token = ++loopToken;
    loopIA = false;
    apiFetch('/api/play', { method: 'POST' })
      .then(game => {
        afficherEtat(game);
        if (!game.partieTerminee && (modeActuel === 4 || modeActuel === 5)) {
          lancerBoucleIA(500, token);
        }
      })
      .catch(console.error);
  },

  analyse() {
    const prof = parseInt(document.getElementById('num-prof').value);
    if (isNaN(prof) || prof < 1) return;
    apiFetch(`/api/analyse?profondeur=${prof}`, { method: 'POST' })
      .then(game => afficherScores(game.scores))
      .catch(console.error);
  },

  rejouer() {
    loopIA = false;
    loopToken++;
    apiFetch('/api/reset', { method: 'POST' })
      .then(game => {
        refreshBoard(game.board, game.wins);
        afficherMessage('Sélectionner un mode de jeu');
        modeActuel = 0;
        couleurJoueur1 = 1;
        document.getElementById('board-overlay').style.display = 'flex';
        document.querySelectorAll('.btn-mode[data-mode]').forEach(b => b.classList.remove('active'));
        afficherScores(game.scores);
      })
      .catch(console.error);
  }
};

//jvsIA et jvsO
function jouerUnCoupIA(token) {
  if (loopToken !== token) return;
  apiFetch('/api/playIA', { method: 'POST' })
    .then(game => {
      if (game.dernierCoup !== undefined) dernierColonne = game.dernierCoup;
      afficherEtat(game);
    })
    .catch(console.error);
}

//IAvsIA et OvsO
function lancerBoucleIA(delai, token) {
  if (loopIA) return;
  loopIA = true;
  function step() {
    if (!loopIA || loopToken !== token) { loopIA = false; return; }
    apiFetch('/api/playIA', { method: 'POST' })
      .then(game => {
        if (game.dernierCoup !== undefined) dernierColonne = game.dernierCoup;
        afficherEtat(game);
        if (!game.partieTerminee && loopIA && loopToken === token) {
          setTimeout(step, delai);
        } else {
          loopIA = false;
        }
      })
      .catch(() => { loopIA = false; });
  }
  setTimeout(step, delai);
}

// Affichage 
function afficherEtat(game) {
  if (!game) return;
  refreshBoard(game.board, game.wins);
  afficherScores(game.scores);
  if (game.partieTerminee) {
    // Le gagnant est le joueurCourant (il n'a pas changé après victoire)
    const gagnant = game.joueurCourant;
    const couleur = getCouleurNom(gagnant);
    afficherMessage(`Le joueur ${couleur} a gagné !`);
  } else if (game.message) {
    afficherMessage(game.message);
  } else {
    joueurMaj(game.joueurCourant, dernierColonne);
  }
}

// Retourne le nom de couleur affiché selon la couleur choisie par le joueur 1
function getCouleurNom(joueur) {
  // joueur 1 = couleurJoueur1, joueur 2 = l'autre
  const estJoueur1 = joueur === 1;
  const couleur1 = couleurJoueur1 === 1 ? 'Rouge' : 'Jaune';
  const couleur2 = couleurJoueur1 === 1 ? 'Jaune' : 'Rouge';
  return estJoueur1 ? couleur1 : couleur2;
}

function refreshBoard(board, wins) {
  if (!board) return;
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const p = document.getElementById(`pion-${r}-${c}`);
      if (!p) continue;
      p.className = 'pion';

      if (board[r][c] !== 0) {
        // board[r][c] = 1 ou 2 (numéro du joueur côté serveur)
        // On adapte la classe CSS selon la couleur choisie
        const valeur = board[r][c];
        if (valeur === 1) {
          // Joueur 1 côté serveur → afficher avec la couleur choisie
          p.classList.add(couleurJoueur1 === 1 ? 'j1' : 'j2');
        } else if (valeur === 2) {
          // Joueur 2 côté serveur → afficher avec l'autre couleur
          p.classList.add(couleurJoueur1 === 1 ? 'j2' : 'j1');
        }
      }

      if (wins) {
        let isWin = false;
        if (wins[r] !== undefined && !Array.isArray(wins[r])) {
          isWin = wins[r][c] === true;
        } else if (Array.isArray(wins[r])) {
          isWin = wins[r][c] === true;
        } else if (Array.isArray(wins)) {
          isWin = wins.some(w => Array.isArray(w) ? w[0] === r && w[1] === c : w.r === r && w.c === c);
        }
        if (isWin) p.classList.add('win');
      }
    }
  }
}

function afficherMessage(msg) {
  document.getElementById('annonce').textContent = msg;
}

function afficherScores(scores) {
  if (!scores) return;
  for (let c = 0; c < COLS; c++) {
    const el = document.getElementById(`sc-${c}`);
    if (!el) continue;
    const v = scores[c];
    el.textContent = (v !== undefined && v !== null) ? v : '';
    el.className = 'score-cell' + (v > 0 ? ' pos' : v < 0 ? ' neg' : '');
  }
}

function setModeActif(m) {
  document.querySelectorAll('.btn-mode[data-mode]').forEach(b => {
    b.classList.toggle('active', parseInt(b.dataset.mode) === m);
  });
}

function joueurMaj(joueurSuivant, dernierCoup) {
  if (!joueurSuivant) return;

  // Le joueur précédent est l'autre joueur
  const joueurPrecedent = joueurSuivant === 1 ? 2 : 1;

  // On récupère les noms basés sur la couleur choisie
  const nomSuivant = getCouleurNom(joueurSuivant);
  const nomPrecedent = getCouleurNom(joueurPrecedent);

  if (dernierCoup !== undefined && dernierCoup !== null) {
    const colonne = dernierCoup + 1;
    afficherMessage(`${nomPrecedent} a joué colonne ${colonne} — ${nomSuivant}, à vous de jouer`);
  } else {
    afficherMessage(`Tour du joueur ${nomSuivant}`);
  }
}
