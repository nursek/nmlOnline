import React from 'react';

const ReglesPage: React.FC = () => (
  <div className="container mt-4 text-light">
    <h1>Règles du Jeu</h1>
    <p className="lead"><strong>But du jeu :</strong> Contrôler des territoires et gérer les ressources.</p>
    <h2 className="mt-3">Déroulement</h2>
    <p>Les joueurs recrutent des troupes, achètent des équipements et capturent des territoires pour augmenter leur puissance.</p>
    <ul>
      <li>Chaque unité possède des points de vie et de mouvement.</li>
      <li>Les combats se font en comparant la force des troupes opposées.</li>
    </ul>
    <h2>Conditions de victoire</h2>
    <p>Le joueur ayant le plus de territoires à la fin du temps imparti remporte la partie.</p>
  </div>
);

export default ReglesPage;