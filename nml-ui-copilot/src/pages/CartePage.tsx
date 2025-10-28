import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchAllPlayers } from '../store/playerSlice';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Map, Loader2, Users, MapPin } from 'lucide-react';
import { cn } from '../lib/utils';

export default function CartePage() {
  const dispatch = useAppDispatch();
  const { players, loading, error } = useAppSelector((state) => state.player);

  useEffect(() => {
    dispatch(fetchAllPlayers());
  }, [dispatch]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <Loader2 className="h-12 w-12 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-destructive/10 border border-destructive text-destructive px-4 py-3 rounded-md">
          {error}
        </div>
      </div>
    );
  }

  // Couleurs pour différencier les joueurs
  const playerColors = [
    'bg-blue-500',
    'bg-red-500',
    'bg-green-500',
    'bg-yellow-500',
    'bg-purple-500',
    'bg-pink-500',
    'bg-orange-500',
    'bg-cyan-500',
  ];

  const getPlayerColor = (index: number) => {
    return playerColors[index % playerColors.length];
  };

  // Collecter tous les secteurs de tous les joueurs
  const allSectors = players.flatMap((player, idx) =>
    player.sectors.map((sector) => ({
      ...sector,
      playerName: player.name,
      playerColor: getPlayerColor(idx),
    }))
  );

  // Pour la carte, on utilise number comme identifiant
  // Puisqu'on n'a pas de coordonnées x/y, on affiche juste une liste

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-full">
          <Map className="h-10 w-10 text-primary" />
        </div>
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
            Carte du Monde
          </h1>
          <p className="text-muted-foreground">Vue d'ensemble des territoires conquis</p>
        </div>
      </div>

      {/* Légende des joueurs */}
      <Card className="border-2">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Users className="h-6 w-6 text-primary" />
            <CardTitle>Joueurs actifs</CardTitle>
          </div>
          <CardDescription>Commandants en présence</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {players.map((player, idx) => (
              <div
                key={`player-${player.id ?? idx}`}
                className="flex items-center space-x-3 p-3 bg-secondary rounded-lg border-2 border-border"
              >
                <div className={cn('w-4 h-4 rounded-full', getPlayerColor(idx))} />
                <div>
                  <p className="font-semibold">{player.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {player.sectors.length} {player.sectors.length > 1 ? 'territoires' : 'territoire'}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Carte */}
      <Card className="border-2">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <MapPin className="h-6 w-6 text-primary" />
            <CardTitle>Carte des territoires</CardTitle>
          </div>
          <CardDescription>
            {allSectors.length} secteur{allSectors.length > 1 ? 's' : ''} contrôlé{allSectors.length > 1 ? 's' : ''}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {allSectors.length === 0 ? (
            <div className="text-center py-16 text-muted-foreground">
              <Map className="h-16 w-16 mx-auto mb-4 opacity-50" />
              <p className="text-lg">Aucun territoire n'a encore été conquis</p>
              <p className="text-sm">La carte est vide pour le moment</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {allSectors.map((sector, index) => (
                <div
                  key={`${sector.playerName}-${sector.number ?? index}`}
                  className="territory-card p-4 border-2 hover:scale-105 transition-transform cursor-pointer"
                  style={{
                    borderColor: sector.playerColor.replace('bg-', 'rgb(var(--color-'),
                  }}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className={cn('w-4 h-4 rounded-full', sector.playerColor)} />
                    <span className="text-xs font-bold text-muted-foreground">
                      #{sector.number ?? 'N/A'}
                    </span>
                  </div>
                  <h3 className="font-bold text-lg mb-1">{sector.name}</h3>
                  <p className="text-sm text-muted-foreground mb-2">{sector.playerName}</p>
                  <div className="space-y-1 text-xs">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Revenus:</span>
                      <span className="font-semibold text-yellow-500">{sector.income ?? 0} ₡</span>
                    </div>
                    {sector.army && sector.army.length > 0 && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Armée:</span>
                        <span className="font-semibold">{sector.army.length} unités</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Statistiques des territoires */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {players.map((player, idx) => (
          <Card key={`player-stats-${player.id ?? idx}`} className="border-2">
            <CardHeader>
              <div className="flex items-center space-x-2">
                <div className={cn('w-4 h-4 rounded-full', getPlayerColor(idx))} />
                <CardTitle>{player.name}</CardTitle>
              </div>
              <CardDescription>
                {player.sectors.length} territoire{player.sectors.length > 1 ? 's' : ''}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Puissance globale:</span>
                  <span className="font-semibold">{player.stats.globalPower.toFixed(0)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Argent:</span>
                  <span className="font-semibold">{player.stats.money.toFixed(0)} ₡</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Revenus:</span>
                  <span className="font-semibold">{player.stats.totalIncome.toFixed(0)} ₡/tour</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Territoires:</span>
                  <span className="font-semibold">{player.sectors.length}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

