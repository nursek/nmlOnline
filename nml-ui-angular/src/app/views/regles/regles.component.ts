import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-regles',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="regles-container">
      <div class="hero-section">
        <h1>📖 Règles du Jeu</h1>
        <p class="lead">Découvrez comment jouer à NML Online et dominer le monde</p>
      </div>

      <div class="rules-content">
        <!-- But du jeu -->
        <section class="rule-section">
          <div class="section-icon">🎯</div>
          <h2>But du Jeu</h2>
          <div class="section-content">
            <p>
              Le but de <strong>NML Online</strong> est de contrôler le maximum de territoires 
              en gérant stratégiquement vos ressources, vos troupes et vos équipements.
            </p>
            <div class="highlight-box">
              <strong>Objectif principal :</strong> Être le joueur avec le plus de territoires 
              et de points de victoire à la fin de la partie ou atteindre la domination totale.
            </div>
          </div>
        </section>

        <!-- Déroulement -->
        <section class="rule-section">
          <div class="section-icon">🔄</div>
          <h2>Déroulement d'un Tour</h2>
          <div class="section-content">
            <ol class="steps-list">
              <li>
                <strong>Phase de Revenus</strong>
                <p>Collectez l'or et les ressources générés par vos territoires</p>
              </li>
              <li>
                <strong>Phase d'Achat</strong>
                <p>Achetez des équipements dans la boutique pour renforcer vos unités</p>
              </li>
              <li>
                <strong>Phase de Recrutement</strong>
                <p>Recrutez de nouvelles troupes dans vos territoires contrôlés</p>
              </li>
              <li>
                <strong>Phase de Mouvement</strong>
                <p>Déplacez vos unités sur la carte pour attaquer ou défendre</p>
              </li>
              <li>
                <strong>Phase de Combat</strong>
                <p>Résolvez les combats dans les zones contestées</p>
              </li>
            </ol>
          </div>
        </section>

        <!-- Unités et Combat -->
        <section class="rule-section">
          <div class="section-icon">⚔️</div>
          <h2>Unités et Combat</h2>
          <div class="section-content">
            <div class="info-grid">
              <div class="info-card">
                <h3>👥 Types d'Unités</h3>
                <ul>
                  <li><strong>Infanterie :</strong> Unités de base, équilibrées</li>
                  <li><strong>Cavalerie :</strong> Rapide, bonus contre infanterie</li>
                  <li><strong>Archers :</strong> Attaque à distance</li>
                  <li><strong>Mages :</strong> Sorts puissants, fragiles</li>
                </ul>
              </div>

              <div class="info-card">
                <h3>⚔️ Système de Combat</h3>
                <ul>
                  <li>Chaque unité a des <strong>points de vie (PV)</strong></li>
                  <li>Les attaques causent des dégâts selon la <strong>force</strong></li>
                  <li>Les <strong>équipements</strong> augmentent les stats</li>
                  <li>Le terrain peut donner des <strong>bonus de défense</strong></li>
                </ul>
              </div>

              <div class="info-card">
                <h3>🛡️ Défense</h3>
                <ul>
                  <li>Les territoires ont un <strong>niveau de défense</strong></li>
                  <li>Construisez des <strong>fortifications</strong> pour augmenter la défense</li>
                  <li>Les unités en défense ont un <strong>bonus de +20%</strong></li>
                </ul>
              </div>

              <div class="info-card">
                <h3>💰 Ressources</h3>
                <ul>
                  <li><strong>Or :</strong> Monnaie principale pour les achats</li>
                  <li><strong>Nourriture :</strong> Nécessaire pour recruter</li>
                  <li><strong>Fer :</strong> Pour les équipements</li>
                  <li><strong>Gemmes :</strong> Ressource rare pour objets spéciaux</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <!-- Territoires -->
        <section class="rule-section">
          <div class="section-icon">🗺️</div>
          <h2>Territoires et Conquête</h2>
          <div class="section-content">
            <div class="territory-rules">
              <div class="rule-item">
                <span class="rule-number">1</span>
                <div class="rule-text">
                  <h4>Contrôle de Territoire</h4>
                  <p>Un territoire est contrôlé quand toutes les unités ennemies sont éliminées</p>
                </div>
              </div>
              <div class="rule-item">
                <span class="rule-number">2</span>
                <div class="rule-text">
                  <h4>Revenus</h4>
                  <p>Chaque territoire génère de l'or et des ressources par tour selon son niveau</p>
                </div>
              </div>
              <div class="rule-item">
                <span class="rule-number">3</span>
                <div class="rule-text">
                  <h4>Amélioration</h4>
                  <p>Vous pouvez améliorer vos territoires pour augmenter leurs revenus</p>
                </div>
              </div>
              <div class="rule-item">
                <span class="rule-number">4</span>
                <div class="rule-text">
                  <h4>Adjacence</h4>
                  <p>Vous ne pouvez attaquer que les territoires adjacents aux vôtres</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Victoire -->
        <section class="rule-section">
          <div class="section-icon">👑</div>
          <h2>Conditions de Victoire</h2>
          <div class="section-content">
            <div class="victory-conditions">
              <div class="victory-card">
                <div class="victory-icon">🏆</div>
                <h3>Domination Totale</h3>
                <p>Contrôlez <strong>tous les territoires</strong> de la carte</p>
              </div>
              <div class="victory-card">
                <div class="victory-icon">⭐</div>
                <h3>Victoire aux Points</h3>
                <p>Ayez le <strong>score le plus élevé</strong> à la fin du temps imparti</p>
              </div>
              <div class="victory-card">
                <div class="victory-icon">💎</div>
                <h3>Victoire Économique</h3>
                <p>Accumulez <strong>10,000 or</strong> et 5 territoires de niveau 5</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Conseils -->
        <section class="rule-section tips-section">
          <div class="section-icon">💡</div>
          <h2>Conseils Stratégiques</h2>
          <div class="section-content">
            <div class="tips-grid">
              <div class="tip-card">
                <span class="tip-icon">🎯</span>
                <h4>Équilibrez votre économie</h4>
                <p>Ne dépensez pas tout votre or. Gardez des réserves pour les urgences.</p>
              </div>
              <div class="tip-card">
                <span class="tip-icon">🛡️</span>
                <h4>Défendez vos frontières</h4>
                <p>Placez des unités fortes sur vos territoires les plus vulnérables.</p>
              </div>
              <div class="tip-card">
                <span class="tip-icon">⚔️</span>
                <h4>Choisissez vos batailles</h4>
                <p>N'attaquez pas sans préparation. Analysez les forces ennemies.</p>
              </div>
              <div class="tip-card">
                <span class="tip-icon">🛒</span>
                <h4>Équipez stratégiquement</h4>
                <p>Les bons équipements peuvent faire toute la différence en combat.</p>
              </div>
              <div class="tip-card">
                <span class="tip-icon">🤝</span>
                <h4>Formez des alliances</h4>
                <p>Coopérez avec d'autres joueurs pour des avantages mutuels.</p>
              </div>
              <div class="tip-card">
                <span class="tip-icon">📊</span>
                <h4>Planifiez à long terme</h4>
                <p>Pensez plusieurs tours à l'avance et anticipez les mouvements adverses.</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Call to Action -->
        <section class="cta-section">
          <h2>Prêt à Jouer ?</h2>
          <p>Maintenant que vous connaissez les règles, il est temps de conquérir le monde !</p>
          <div class="cta-buttons">
            <a routerLink="/carte" class="btn btn-primary btn-large">
              <span>🗺️</span>
              Commencer une Partie
            </a>
            <a routerLink="/joueur" class="btn btn-secondary btn-large">
              <span>👤</span>
              Mon Profil
            </a>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .regles-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
      animation: fadeIn 0.5s ease-out;
    }

    .hero-section {
      text-align: center;
      padding: 3rem 2rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 20px;
      margin-bottom: 3rem;
      box-shadow: 0 8px 30px rgba(102, 126, 234, 0.3);
    }

    .hero-section h1 {
      font-size: 3rem;
      margin: 0 0 1rem 0;
    }

    .lead {
      font-size: 1.3rem;
      margin: 0;
      opacity: 0.95;
    }

    .rules-content {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .rule-section {
      background: white;
      border-radius: 16px;
      padding: 2rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      animation: slideUp 0.5s ease-out;
    }

    .section-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
      display: inline-block;
    }

    .rule-section h2 {
      font-size: 2rem;
      color: #333;
      margin: 0 0 1.5rem 0;
    }

    .section-content {
      color: #666;
      line-height: 1.8;
    }

    .highlight-box {
      background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
      border-left: 4px solid #ff9800;
      padding: 1.5rem;
      border-radius: 8px;
      margin-top: 1rem;
      color: #333;
    }

    .steps-list {
      list-style: none;
      padding: 0;
      counter-reset: step-counter;
    }

    .steps-list li {
      counter-increment: step-counter;
      margin-bottom: 1.5rem;
      padding-left: 3.5rem;
      position: relative;
    }

    .steps-list li::before {
      content: counter(step-counter);
      position: absolute;
      left: 0;
      top: 0;
      width: 2.5rem;
      height: 2.5rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 1.2rem;
    }

    .steps-list li strong {
      color: #333;
      font-size: 1.1rem;
      display: block;
      margin-bottom: 0.25rem;
    }

    .steps-list li p {
      margin: 0;
      color: #666;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-top: 1.5rem;
    }

    .info-card {
      background: #f8f9fa;
      padding: 1.5rem;
      border-radius: 12px;
      border: 2px solid #e9ecef;
      transition: all 0.3s;
    }

    .info-card:hover {
      border-color: #667eea;
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.2);
    }

    .info-card h3 {
      color: #333;
      margin: 0 0 1rem 0;
      font-size: 1.2rem;
    }

    .info-card ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .info-card li {
      padding: 0.5rem 0;
      color: #666;
      border-bottom: 1px solid #e9ecef;
    }

    .info-card li:last-child {
      border-bottom: none;
    }

    .territory-rules {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      margin-top: 1.5rem;
    }

    .rule-item {
      display: flex;
      gap: 1.5rem;
      align-items: flex-start;
    }

    .rule-number {
      width: 50px;
      height: 50px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 1.5rem;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
    }

    .rule-text h4 {
      margin: 0 0 0.5rem 0;
      color: #333;
      font-size: 1.2rem;
    }

    .rule-text p {
      margin: 0;
      color: #666;
    }

    .victory-conditions {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-top: 1.5rem;
    }

    .victory-card {
      background: linear-gradient(to bottom, #f8f9fa, white);
      padding: 2rem;
      border-radius: 16px;
      text-align: center;
      border: 3px solid #e9ecef;
      transition: all 0.3s;
    }

    .victory-card:hover {
      border-color: #ffd700;
      transform: scale(1.05);
      box-shadow: 0 8px 30px rgba(255, 215, 0, 0.3);
    }

    .victory-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    .victory-card h3 {
      color: #333;
      margin: 0 0 0.75rem 0;
      font-size: 1.3rem;
    }

    .victory-card p {
      color: #666;
      margin: 0;
    }

    .tips-section {
      background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
    }

    .tips-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
      margin-top: 1.5rem;
    }

    .tip-card {
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      border-left: 4px solid #667eea;
      transition: all 0.3s;
    }

    .tip-card:hover {
      transform: translateX(8px);
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    }

    .tip-icon {
      font-size: 2rem;
      display: block;
      margin-bottom: 0.5rem;
    }

    .tip-card h4 {
      color: #333;
      margin: 0 0 0.5rem 0;
      font-size: 1.1rem;
    }

    .tip-card p {
      color: #666;
      margin: 0;
      font-size: 0.95rem;
    }

    .cta-section {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      text-align: center;
      padding: 3rem 2rem;
      border-radius: 20px;
      box-shadow: 0 8px 30px rgba(102, 126, 234, 0.4);
    }

    .cta-section h2 {
      font-size: 2.5rem;
      margin: 0 0 1rem 0;
    }

    .cta-section p {
      font-size: 1.2rem;
      margin: 0 0 2rem 0;
      opacity: 0.95;
    }

    .cta-buttons {
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
    }

    .btn {
      padding: 1rem 2rem;
      border-radius: 12px;
      text-decoration: none;
      font-weight: 600;
      font-size: 1.1rem;
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      transition: all 0.3s;
      border: none;
      cursor: pointer;
    }

    .btn-large {
      padding: 1.25rem 2.5rem;
      font-size: 1.2rem;
    }

    .btn-primary {
      background: white;
      color: #667eea;
      box-shadow: 0 4px 20px rgba(255, 255, 255, 0.3);
    }

    .btn-primary:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 30px rgba(255, 255, 255, 0.5);
    }

    .btn-secondary {
      background: transparent;
      color: white;
      border: 2px solid white;
    }

    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.2);
      transform: translateY(-3px);
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }

    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(30px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @media (max-width: 768px) {
      .regles-container {
        padding: 1rem;
      }

      .hero-section h1 {
        font-size: 2rem;
      }

      .lead {
        font-size: 1.1rem;
      }

      .rule-section {
        padding: 1.5rem;
      }

      .rule-section h2 {
        font-size: 1.5rem;
      }

      .info-grid,
      .victory-conditions,
      .tips-grid {
        grid-template-columns: 1fr;
      }

      .cta-buttons {
        flex-direction: column;
      }
    }
  `]
})
export class ReglesComponent {}
