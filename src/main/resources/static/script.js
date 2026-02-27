const ROWS = 9, COLS = 9;
let dernierColonne = null;

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

//Etat
let modeActuel = 0;
let loopIA = false;
let loopToken = 0;

//API
const api = {

  setModeJ(m) {
    modeActuel = m;
    loopIA = false;
    const token = ++loopToken;
    setModeActif(m);
    document.getElementById('board-overlay').style.display = 'none';
    fetch(`/api/setMode/${m}`, { method: 'POST' })
      .then(r => r.json())
      .then(game => {
        afficherEtat(game);
        // boucle frontend
        if ((m === 4 || m === 5) && !game.partieTerminee) {
          lancerBoucleIA(500, token);
        }
      })
      .catch(console.error);
  },

  colonneCliquee(col) {
  if (modeActuel === 4 || modeActuel === 5) return;
  fetch(`/api/play/${col}`, { method: 'POST' })
    .then(r => r.json())
    .then(game => {
      dernierColonne = col;
      afficherEtat(game);
      // Si c'est au tour de l'IA, on attend avant de jouer
      if (!game.partieTerminee && (modeActuel === 2 || modeActuel === 3)) {
        const token = loopToken;
        setTimeout(() => {
          if (loopToken !== token) return;
          jouerUnCoupIA(token);
        }, 700); // délai visible pour lire l'annonce
      }
    })
    .catch(console.error);
},

  retourner() {
    fetch('/api/retirer', { method: 'POST' })
      .then(r => r.json())
      .then(afficherEtat)
      .catch(console.error);
  },

  remettre() {
    fetch('/api/remettre', { method: 'POST' })
      .then(r => r.json())
      .then(afficherEtat)
      .catch(console.error);
  },

  pause() {
    loopIA = false;
    loopToken++;
    fetch('/api/pause', { method: 'POST' })
      .then(r => r.json())
      .then(game => afficherMessage(game.message || 'Pause'))
      .catch(console.error);
  },

  play() {
    const token = ++loopToken;
    loopIA = false;
    fetch('/api/play', { method: 'POST' })
      .then(r => r.json())
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
    fetch(`/api/analyse?profondeur=${prof}`, { method: 'POST' })
      .then(r => r.json())
      .then(game => afficherScores(game.scores))
      .catch(console.error);
  },

  rejouer() {
    loopIA = false;
    loopToken++;
    fetch('/api/reset', { method: 'POST' })
      .then(r => r.json())
      .then(game => {
        refreshBoard(game.board, game.wins);
        afficherMessage('Sélectionner un mode de jeu');
        modeActuel = 0;
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
  fetch('/api/playIA', { method: 'POST' })
    .then(r => r.json())
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
    fetch('/api/playIA', { method: 'POST' })
      .then(r => r.json())
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
    const couleur = game.joueurCourant === 1 ? 'Rouge' : 'Jaune';
    afficherMessage(`Le joueur ${couleur} a gagné !`);
  } else if (game.message) {
    afficherMessage(game.message);
  } else {
    joueurMaj(game.joueurCourant, dernierColonne);
  }
}


function refreshBoard(board, wins) {
  if (!board) return;
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const p = document.getElementById(`pion-${r}-${c}`);
      if (!p) continue;
      p.className = 'pion';
      if (board[r][c] === 1) p.classList.add('j1');
      else if (board[r][c] === 2) p.classList.add('j2');
      if (wins) {
        let isWin = false;
        if (wins[r] !== undefined && !Array.isArray(wins[r])) {
          isWin = wins[r][c] === true;
        } else if (Array.isArray(wins[r])) {
          isWin = wins[r][c] === true;
        } else if (Array.isArray(wins)) {
          isWin = wins.some(w => Array.isArray(w) ? w[0]===r && w[1]===c : w.r===r && w.c===c);
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

  const nom = j => j === 1 ? 'Rouge' : 'Jaune';
  const joueurPrecedent = joueurSuivant === 1 ? 2 : 1;

  if (dernierCoup !== undefined && dernierCoup !== null) {
    const colonne = dernierCoup + 1; // index 0 → numéro 1
    afficherMessage(`${nom(joueurPrecedent)} a joué colonne ${colonne} — ${nom(joueurSuivant)}, à vous de jouer`);
  } else {
    afficherMessage(`Tour du joueur ${nom(joueurSuivant)}`);
  }
}