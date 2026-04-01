const ROWS = 9, COLS = 9;
let dernierColonne = null;
let couleurJoueur1 = 1;

//Grille 
(function buildGrid() {
  const colNums = document.getElementById('col-numbers');
  for (let c = 0; c < COLS; c++) {
    const btn = document.createElement('button');
    btn.textContent = c + 1;
    btn.onclick = () => { if (!modePeinture) api.colonneCliquee(c); };
    colNums.appendChild(btn);
  }
  const grille = document.getElementById('grille');
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const cell = document.createElement('div');
      cell.className = 'cell';
      cell.onclick = () => { if (!modePeinture) api.colonneCliquee(c); };
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

function ouvrirParametres() {
  document.getElementById('modal-overlay').style.display = 'flex';
}
function fermerParametres() {
  document.getElementById('modal-overlay').style.display = 'none';
}

let _modePending = null;

function ouvrirChoixCouleur(mode) {
  _modePending = mode;
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

let modeActuel = 0;
let loopIA = false;
let loopToken = 0;

function getProfondeur() {
  const val = parseInt(document.getElementById('num-prof').value);
  return (isNaN(val) || val < 1) ? 4 : Math.min(val, 12);
}

function _lancerMode(mode, keepBoard = false) {
  modeActuel = mode;
  loopIA = false;
  const token = ++loopToken;
  setModeActif(mode);
  document.getElementById('board-overlay').style.display = 'none';

  const prof = getProfondeur();
  apiFetch(`/api/setMode/${mode}?couleur=${couleurJoueur1}&profondeur=${prof}`, { method: 'POST' })
    .then(game => {
      afficherEtat(game);
      if (game.partieTerminee) return;

      const joueur = game.joueurCourant;
      const iaEstJoueur1 = (couleurJoueur1 === 2);
      const iaEstJoueur2 = (couleurJoueur1 === 1);
      const cEstTourIA = (mode === 2 || mode === 3) &&
        ((iaEstJoueur1 && joueur === 1) || (iaEstJoueur2 && joueur === 2));

      if (mode === 4 || mode === 5) {
        lancerBoucleIA(500, token);
      } else if (cEstTourIA) {
        setTimeout(() => jouerUnCoupIA(token), 500);
      } else if (!keepBoard && (mode === 2 || mode === 3) && couleurJoueur1 === 2) {
        setTimeout(() => jouerUnCoupIA(token), 500);
      }
    })
    .catch(console.error);
}

function apiFetch(url, options = {}) {
  return fetch(url, { ...options, credentials: 'include' }).then(r => r.json());
}

const api = {

  setModeJ(m) {
    const partieEnCours = modeActuel !== 0;
    if (m === 1 || m === 2 || m === 3) {
      if (partieEnCours) {
        _lancerMode(m, true);
      } else {
        ouvrirChoixCouleur(m);
      }
    } else {
      couleurJoueur1 = 1;
      _lancerMode(m, false);
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
    apiFetch('/api/retirer', { method: 'POST' }).then(afficherEtat).catch(console.error);
  },

  remettre() {
    apiFetch('/api/remettre', { method: 'POST' }).then(afficherEtat).catch(console.error);
  },

  pause() {
    loopIA = false;
    loopToken++;
    arreterProgression(true);
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
    const prof = getProfondeur();
    apiFetch(`/api/analyse?profondeur=${prof}`, { method: 'POST' })
      .then(game => afficherScores(game.scores))
      .catch(console.error);
  },

  suggestion() {
    const prof = getProfondeur();
    apiFetch(`/api/analyse?profondeur=${prof}`, { method: 'POST' })
      .then(game => {
        const scores = game.scores;
        if (!scores) return;
        let bestCol = -1, bestScore = -Infinity;
        scores.forEach((s, c) => {
          if (s !== null && s !== undefined && s > bestScore) {
            bestScore = s; bestCol = c;
          }
        });
        afficherScores(scores);
        document.querySelectorAll('.score-cell').forEach(el => el.classList.remove('suggestion'));
        if (bestCol !== -1) {
          const el = document.getElementById(`sc-${bestCol}`);
          if (el) el.classList.add('suggestion');
          afficherMessage(`💡 L'IA jouerait colonne ${bestCol + 1}`);
        }
      })
      .catch(console.error);
  },

  predire() {
    const prof = getProfondeur();
    const btn = document.getElementById('btn-predire');
    if (btn) {
      btn.disabled = true;
      btn.textContent = '⏳ Calcul…';
    }

    //Barre de progression de l'IA
    demarrerProgression(prof);

    apiFetch(`/api/predire?profondeur=${prof}`, { method: 'POST' })
      .then(pred => {
        finirProgression();
        if (btn) {
          btn.disabled = false;
          btn.textContent = '🔮 Prédire';
        }
        afficherPrediction(pred);
      })
      .catch(err => {
        arreterProgression(true);
        if (btn) {
          btn.disabled = false;
          btn.textContent = '🔮 Prédire';
        }
        console.error(err);
      });
  },

  rejouer() {
    loopIA = false;
    loopToken++;
    arreterProgression(true);
    cacherPrediction();
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
  },

  suggestionPeinture() {
    const prof = getProfondeur();
    apiFetch('/api/setPlateau', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(grillePeinte)
    })
    .then(() => apiFetch(`/api/analyse?profondeur=${prof}`, { method: 'POST' }))
    .then(game => {
      const scores = game.scores;
      if (!scores) return;
      let bestCol = -1, bestScore = -Infinity;
      scores.forEach((s, c) => {
        if (s !== null && s !== undefined && s > bestScore) {
          bestScore = s; bestCol = c;
        }
      });
      afficherScores(scores);
      document.querySelectorAll('.score-cell').forEach(el => el.classList.remove('suggestion'));
      if (bestCol !== -1) {
        const el = document.getElementById(`sc-${bestCol}`);
        if (el) el.classList.add('suggestion');
        afficherMessage(`🤖 L'IA jouerait colonne ${bestCol + 1} (score : ${bestScore})`);
      }
    })
    .catch(console.error);
  }
};

function afficherPrediction(pred) {
  const zone = document.getElementById('prediction-result');
  if (!zone) return;

  const { gagnant, coups, certain } = pred;

  let couleur, emoji, nomJoueur, certitudeTexte;

  if (gagnant === 0) {
    couleur = '#888';
    emoji = '🤝';
    nomJoueur = 'Match nul';
    certitudeTexte = certain ? 'résultat forcé' : 'tendance';
  } else {
    const estJ1 = gagnant === 1;
    const nomCouleur = (estJ1 === (couleurJoueur1 === 1)) ? 'Rouge' : 'Jaune';
    couleur = nomCouleur === 'Rouge' ? '#e63946' : '#ffd60a';
    emoji = nomCouleur === 'Rouge' ? '🔴' : '🟡';
    nomJoueur = `Joueur ${nomCouleur}`;
    certitudeTexte = certain ? 'victoire forcée' : 'favori';
  }

  const coupsTexte = coups > 0
    ? `en ~${coups} coup${coups > 1 ? 's' : ''}`
    : '';

  zone.style.display = 'flex';
  zone.innerHTML = `
    <span class="pred-emoji">${emoji}</span>
    <span class="pred-nom" style="color:${couleur}">${nomJoueur}</span>
    <span class="pred-detail">${coupsTexte}</span>
    <span class="pred-certitude ${certain ? 'certain' : 'probable'}">${certitudeTexte}</span>
  `;
}

function cacherPrediction() {
  const zone = document.getElementById('prediction-result');
  if (zone) { zone.style.display = 'none'; zone.innerHTML = ''; }
}

function dureeEstimeeIA(profondeur) {
  const base = {
    1: 50,   2: 100,    3: 200,    4: 500,
    5: 1200, 6: 2500,   7: 5000,   8: 12000,
    9: 30000, 10: 70000, 11: 150000, 12: 300000
  };
  return base[profondeur] ?? 500;
}

let _progressTimer = null;
let _progressStart = null;

function demarrerProgression(profondeur) {
  const conteneur = document.getElementById('ia-progress-container');
  const barre     = document.getElementById('ia-progress-bar');
  const texte     = document.getElementById('ia-progress-text');
  if (!conteneur || !barre || !texte) return;

  arreterProgression(false);

  const duree = dureeEstimeeIA(profondeur);
  _progressStart = performance.now();
  conteneur.style.display = 'flex';
  barre.style.width = '0%';
  texte.textContent = '0%';

  _progressTimer = setInterval(() => {
    const elapsed = performance.now() - _progressStart;
    const ratio = elapsed / duree;
    const pct = Math.min(95, Math.round((1 - Math.exp(-3 * ratio)) * 100));
    barre.style.width = pct + '%';
    texte.textContent = pct + '%';
  }, 50);
}

function finirProgression() {
  arreterProgression(false);
  const conteneur = document.getElementById('ia-progress-container');
  const barre     = document.getElementById('ia-progress-bar');
  const texte     = document.getElementById('ia-progress-text');
  if (!conteneur || !barre || !texte) return;

  barre.style.width = '100%';
  texte.textContent = '100%';
  setTimeout(() => arreterProgression(true), 400);
}

function arreterProgression(cacher = true) {
  if (_progressTimer) {
    clearInterval(_progressTimer);
    _progressTimer = null;
  }
  if (cacher) {
    const conteneur = document.getElementById('ia-progress-container');
    if (conteneur) conteneur.style.display = 'none';
  }
}

function jouerUnCoupIA(token) {
  if (loopToken !== token) return;

  const prof = getProfondeur();
  demarrerProgression(prof);

  apiFetch('/api/playIA', { method: 'POST' })
    .then(game => {
      finirProgression();
      if (game.dernierCoup !== undefined) dernierColonne = game.dernierCoup;
      afficherEtat(game);
    })
    .catch(err => {
      arreterProgression(true);
      console.error(err);
    });
}

function lancerBoucleIA(delai, token) {
  if (loopIA) return;
  loopIA = true;

  function step() {
    if (!loopIA || loopToken !== token) { loopIA = false; arreterProgression(true); return; }

    const prof = getProfondeur();
    demarrerProgression(prof);

    apiFetch('/api/playIA', { method: 'POST' })
      .then(game => {
        finirProgression();
        if (game.dernierCoup !== undefined) dernierColonne = game.dernierCoup;
        afficherEtat(game);
        if (!game.partieTerminee && loopIA && loopToken === token) {
          setTimeout(step, delai);
        } else {
          loopIA = false;
        }
      })
      .catch(() => {
        loopIA = false;
        arreterProgression(true);
      });
  }
  setTimeout(step, delai);
}

let modePeinture = false;
let pinceauActuel = 1;
let grillePeinte = Array.from({ length: ROWS }, () => Array(COLS).fill(0));
const _paintHandlers = new Map();

function togglePeinture() {
  modePeinture = !modePeinture;
  const boardWrap = document.getElementById('board-wrap');
  const btnIA     = document.getElementById('btn-ia-peint');
  const btnPaint  = document.getElementById('btn-paint-mode');

  if (modePeinture) {
    for (let r = 0; r < ROWS; r++)
      for (let c = 0; c < COLS; c++) {
        const p = document.getElementById(`pion-${r}-${c}`);
        grillePeinte[r][c] = p.classList.contains('j1') ? 1
                            : p.classList.contains('j2') ? 2 : 0;
      }
    boardWrap.classList.add('paint-mode');
    btnIA.style.display = 'block';
    btnPaint.textContent = ' Fin peinture ';
    afficherMessage('🎨 Mode peinture — cliquez/glissez sur les cases');
    attachPaintListeners();

  } else {
    boardWrap.classList.remove('paint-mode');
    btnIA.style.display = 'none';
    btnPaint.textContent = '🎨 Peindre';
    detachPaintListeners();
    cacherPrediction();

    dernierColonne = null;

    apiFetch('/api/setPlateau', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(grillePeinte)
    })
    .then(game => {
      afficherEtat(game);
      if (!game.partieTerminee && modeActuel !== 0) {
        setModeActif(modeActuel);
        const token = loopToken;
        const joueur = game.joueurCourant;
        const iaEstJoueur1 = (couleurJoueur1 === 2);
        const iaEstJoueur2 = (couleurJoueur1 === 1);
        const cEstTourIA = (modeActuel === 2 || modeActuel === 3) &&
          ((iaEstJoueur1 && joueur === 1) || (iaEstJoueur2 && joueur === 2));
        if (modeActuel === 4 || modeActuel === 5) {
          lancerBoucleIA(500, token);
        } else if (cEstTourIA) {
          setTimeout(() => jouerUnCoupIA(token), 500);
        }
      } else if (modeActuel === 0) {
        document.getElementById('board-overlay').style.display = 'flex';
      }
    })
    .catch(console.error);
  }
}

function setPinceau(val) {
  pinceauActuel = val;
  document.getElementById('brush-rouge').classList.toggle('active', val === 1);
  document.getElementById('brush-jaune').classList.toggle('active', val === 2);
  document.getElementById('brush-gomme').classList.toggle('active', val === 0);
}

function peindreCase(r, c) {
  grillePeinte[r][c] = pinceauActuel;
  const p = document.getElementById(`pion-${r}-${c}`);
  p.className = 'pion';
  if (pinceauActuel === 1) p.classList.add('j1');
  else if (pinceauActuel === 2) p.classList.add('j2');
}

function attachPaintListeners() {
  let isPainting = false;
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const cell = document.getElementById(`pion-${r}-${c}`).parentElement;
      const mousedown = (e) => { e.preventDefault(); isPainting = true; peindreCase(r, c); };
      const mouseover  = () => { if (isPainting) peindreCase(r, c); };
      const mouseup    = () => { isPainting = false; };
      cell.addEventListener('mousedown', mousedown);
      cell.addEventListener('mouseover', mouseover);
      document.addEventListener('mouseup', mouseup);
      _paintHandlers.set(`${r}-${c}`, { cell, mousedown, mouseover, mouseup });
    }
  }
}

function detachPaintListeners() {
  _paintHandlers.forEach(({ cell, mousedown, mouseover, mouseup }) => {
    cell.removeEventListener('mousedown', mousedown);
    cell.removeEventListener('mouseover', mouseover);
    document.removeEventListener('mouseup', mouseup);
  });
  _paintHandlers.clear();
}

function afficherEtat(game) {
  if (!game) return;
  refreshBoard(game.board, game.wins);
  afficherScores(game.scores);
  cacherPrediction(); // on efface la prédiction à chaque coup joué
  if (game.partieTerminee) {
    const couleur = getCouleurNom(game.joueurCourant);
    afficherMessage(game.message || `Le joueur ${couleur} a gagné !`);
  } else if (game.message) {
    afficherMessage(game.message);
  } else {
    joueurMaj(game.joueurCourant, dernierColonne);
  }
}

function getCouleurNom(joueur) {
  const couleur1 = couleurJoueur1 === 1 ? 'Rouge' : 'Jaune';
  const couleur2 = couleurJoueur1 === 1 ? 'Jaune' : 'Rouge';
  return joueur === 1 ? couleur1 : couleur2;
}

function refreshBoard(board, wins) {
  if (!board) return;
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const p = document.getElementById(`pion-${r}-${c}`);
      if (!p) continue;
      p.className = 'pion';
      const valeur = board[r][c];
      if (valeur === 1) p.classList.add('j1');
      else if (valeur === 2) p.classList.add('j2');

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
  const joueurPrecedent = joueurSuivant === 1 ? 2 : 1;
  const nomSuivant   = getCouleurNom(joueurSuivant);
  const nomPrecedent = getCouleurNom(joueurPrecedent);
  if (dernierCoup !== undefined && dernierCoup !== null) {
    afficherMessage(`${nomPrecedent} a joué colonne ${dernierCoup + 1} — ${nomSuivant}, à vous de jouer`);
  } else {
    afficherMessage(`Tour du joueur ${nomSuivant}`);
  }
}