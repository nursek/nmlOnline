import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { BookOpen, Target, Users, Swords, Trophy } from 'lucide-react';

export default function ReglesPage() {
  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-full">
          <BookOpen className="h-10 w-10 text-primary" />
        </div>
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
            Règles du Jeu
          </h1>
          <p className="text-muted-foreground">Guide complet de NML Online</p>
        </div>
      </div>

      {/* But du jeu */}
      <Card className="border-2 border-primary">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Target className="h-6 w-6 text-primary" />
            <CardTitle className="text-2xl">But du jeu</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <p className="text-lg leading-relaxed">
            Contrôler des territoires et gérer les ressources pour devenir le joueur le plus puissant.
            Votre objectif est de conquérir et de maintenir le contrôle du plus grand nombre de territoires
            possible avant la fin du temps imparti.
          </p>
        </CardContent>
      </Card>

      {/* Déroulement */}
      <Card className="border-2">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Users className="h-6 w-6 text-primary" />
            <CardTitle className="text-2xl">Déroulement</CardTitle>
          </div>
          <CardDescription>Comment se déroule une partie</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-4">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-8 h-8 bg-primary/20 rounded-full flex items-center justify-center font-bold text-primary">
                1
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Recrutement de troupes</h3>
                <p className="text-muted-foreground">
                  Les joueurs recrutent des troupes pour renforcer leur armée. Chaque unité
                  possède des caractéristiques spécifiques comme des points de vie et des points
                  de mouvement.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-8 h-8 bg-primary/20 rounded-full flex items-center justify-center font-bold text-primary">
                2
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Achat d'équipements</h3>
                <p className="text-muted-foreground">
                  Visitez la boutique pour acheter des équipements qui amélioreront les capacités
                  de vos unités. Les équipements offrent des bonus comme la force de frappe (PDF),
                  la défense (PDC), l'armure (ARM) et l'évasion (ESQ).
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-8 h-8 bg-primary/20 rounded-full flex items-center justify-center font-bold text-primary">
                3
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">Capture de territoires</h3>
                <p className="text-muted-foreground">
                  Utilisez vos troupes pour capturer des territoires ennemis ou neutres.
                  Chaque territoire contrôlé augmente votre influence et peut fournir des
                  ressources précieuses.
                </p>
              </div>
            </div>
          </div>

          <div className="bg-secondary/50 p-4 rounded-lg border border-border">
            <h4 className="font-semibold mb-2 flex items-center space-x-2">
              <Swords className="h-5 w-5 text-primary" />
              <span>Système de combat</span>
            </h4>
            <p className="text-sm text-muted-foreground">
              Chaque unité possède des points de vie et de mouvement. Les combats se font en
              comparant la force des troupes opposées, en tenant compte des équipements et
              des bonus de territoire. La stratégie et le positionnement sont essentiels
              pour remporter la victoire !
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Conditions de victoire */}
      <Card className="border-2 border-yellow-500/50">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Trophy className="h-6 w-6 text-yellow-500" />
            <CardTitle className="text-2xl">Conditions de victoire</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <p className="text-lg leading-relaxed">
              Le joueur ayant le <span className="font-bold text-primary">plus de territoires</span> à
              la fin du temps imparti remporte la partie.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
              <div className="bg-gradient-to-br from-yellow-500/10 to-yellow-500/5 p-4 rounded-lg border border-yellow-500/30">
                <h4 className="font-bold text-yellow-500 mb-2">🥇 1ère Place</h4>
                <p className="text-sm text-muted-foreground">
                  Le commandant avec le plus de territoires
                </p>
              </div>

              <div className="bg-gradient-to-br from-gray-400/10 to-gray-400/5 p-4 rounded-lg border border-gray-400/30">
                <h4 className="font-bold text-gray-400 mb-2">🥈 2ème Place</h4>
                <p className="text-sm text-muted-foreground">
                  Le deuxième plus grand conquérant
                </p>
              </div>

              <div className="bg-gradient-to-br from-orange-600/10 to-orange-600/5 p-4 rounded-lg border border-orange-600/30">
                <h4 className="font-bold text-orange-600 mb-2">🥉 3ème Place</h4>
                <p className="text-sm text-muted-foreground">
                  Le troisième commandant
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Conseils stratégiques */}
      <Card className="border-2">
        <CardHeader>
          <CardTitle className="text-2xl">💡 Conseils stratégiques</CardTitle>
          <CardDescription>Pour devenir un grand conquérant</CardDescription>
        </CardHeader>
        <CardContent>
          <ul className="space-y-3">
            <li className="flex items-start space-x-2">
              <span className="text-primary font-bold">•</span>
              <span className="text-muted-foreground">
                Gérez votre argent avec soin - équilibrez entre recrutement et équipement
              </span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="text-primary font-bold">•</span>
              <span className="text-muted-foreground">
                Choisissez les bons équipements pour vos unités selon leur classe
              </span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="text-primary font-bold">•</span>
              <span className="text-muted-foreground">
                Les territoires avec bonus de défense sont plus difficiles à capturer
              </span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="text-primary font-bold">•</span>
              <span className="text-muted-foreground">
                La production de ressources des territoires augmente vos revenus
              </span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="text-primary font-bold">•</span>
              <span className="text-muted-foreground">
                Une bonne stratégie vaut mieux qu'une armée nombreuse !
              </span>
            </li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}

