import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchCurrentPlayer } from '../store/playerSlice';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { User, Coins, TrendingUp, MapPin, Package, Loader2 } from 'lucide-react';
import { cn } from '../lib/utils';

export default function JoueurPage() {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const { currentPlayer, loading, error } = useAppSelector((state) => state.player);

  useEffect(() => {
    if (user?.username) {
      dispatch(fetchCurrentPlayer(user.username));
    }
  }, [dispatch, user]);

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

  if (!currentPlayer) {
    return null;
  }

  const stats = [
    {
      label: 'Argent',
      value: `${currentPlayer.stats.money.toFixed(0)} ₡`,
      icon: Coins,
      color: 'text-yellow-500',
      bgColor: 'bg-yellow-500/10',
    },
    {
      label: 'Revenus',
      value: `${currentPlayer.stats.totalIncome.toFixed(0)} ₡/tour`,
      icon: TrendingUp,
      color: 'text-green-500',
      bgColor: 'bg-green-500/10',
    },
    {
      label: 'Puissance globale',
      value: currentPlayer.stats.globalPower.toFixed(0),
      icon: TrendingUp,
      color: 'text-blue-500',
      bgColor: 'bg-blue-500/10',
    },
    {
      label: 'Territoires',
      value: currentPlayer.sectors.length,
      icon: MapPin,
      color: 'text-purple-500',
      bgColor: 'bg-purple-500/10',
    },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center space-x-4">
        <div className="p-4 bg-primary/10 rounded-full">
          <User className="h-12 w-12 text-primary" />
        </div>
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
            {currentPlayer.name}
          </h1>
          <p className="text-muted-foreground">Commandant en chef</p>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat) => (
          <Card key={stat.label} className="border-2 hover:shadow-xl transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-muted-foreground">{stat.label}</p>
                  <p className="text-3xl font-bold mt-2">{stat.value}</p>
                </div>
                <div className={cn('p-3 rounded-full', stat.bgColor)}>
                  <stat.icon className={cn('h-8 w-8', stat.color)} />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Statistiques détaillées */}
      <Card className="border-2">
        <CardHeader>
          <CardTitle>Statistiques détaillées</CardTitle>
          <CardDescription>Analyse complète de votre puissance</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Puissance offensive</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalOffensivePower.toFixed(0)}</p>
            </div>
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Puissance défensive</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalDefensivePower.toFixed(0)}</p>
            </div>
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Puissance économique</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalEconomyPower.toFixed(0)}</p>
            </div>
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Valeur des véhicules</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalVehiclesValue.toFixed(0)} ₡</p>
            </div>
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Valeur des équipements</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalEquipmentValue.toFixed(0)} ₡</p>
            </div>
            <div className="p-4 bg-secondary rounded-lg">
              <p className="text-sm text-muted-foreground">Armure totale</p>
              <p className="text-2xl font-bold mt-1">{currentPlayer.stats.totalArmor.toFixed(0)}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Equipements */}
      <Card className="border-2">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Package className="h-6 w-6 text-primary" />
            <CardTitle>Équipements possédés</CardTitle>
          </div>
          <CardDescription>Votre arsenal actuel</CardDescription>
        </CardHeader>
        <CardContent>
          {currentPlayer.equipments.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Aucun équipement pour le moment. Visitez la boutique !
            </p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {currentPlayer.equipments.map((stack) => (
                <div
                  key={stack.equipment.name}
                  className="game-card flex items-center justify-between"
                >
                  <div className="flex-1">
                    <h3 className="font-semibold">{stack.equipment.name}</h3>
                    <p className="text-sm text-muted-foreground">{stack.equipment.category}</p>
                    <div className="flex flex-wrap gap-2 mt-2 text-xs">
                      {stack.equipment.pdfBonus > 0 && (
                        <span className="px-2 py-1 bg-red-500/10 text-red-400 rounded">
                          +{stack.equipment.pdfBonus} PDF
                        </span>
                      )}
                      {stack.equipment.pdcBonus > 0 && (
                        <span className="px-2 py-1 bg-blue-500/10 text-blue-400 rounded">
                          +{stack.equipment.pdcBonus} PDC
                        </span>
                      )}
                      {stack.equipment.armBonus > 0 && (
                        <span className="px-2 py-1 bg-green-500/10 text-green-400 rounded">
                          +{stack.equipment.armBonus} ARM
                        </span>
                      )}
                      {stack.equipment.evasionBonus > 0 && (
                        <span className="px-2 py-1 bg-yellow-500/10 text-yellow-400 rounded">
                          +{stack.equipment.evasionBonus} ESQ
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="ml-4 text-right">
                    <span className="text-2xl font-bold text-primary">×{stack.quantity}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Territoires */}
      <Card className="border-2">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <MapPin className="h-6 w-6 text-primary" />
            <CardTitle>Territoires contrôlés</CardTitle>
          </div>
          <CardDescription>Les secteurs sous votre commandement</CardDescription>
        </CardHeader>
        <CardContent>
          {currentPlayer.sectors.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              Aucun territoire contrôlé. Partez à la conquête !
            </p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {currentPlayer.sectors.map((sector, index) => (
                <div key={`sector-${sector.number ?? index}`} className="territory-card border-primary p-4">
                  <h3 className="font-bold text-lg">{sector.name}</h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    Secteur #{sector.number ?? 'N/A'}
                  </p>
                  <div className="mt-3 space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Revenus:</span>
                      <span className="font-semibold text-yellow-500">{sector.income ?? 0} ₡/tour</span>
                    </div>
                    {sector.army && sector.army.length > 0 && (
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Unités:</span>
                        <span className="font-semibold">{sector.army.length}</span>
                      </div>
                    )}
                    {sector.stats && (
                      <>
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">Défense:</span>
                          <span className="font-semibold">+{sector.stats.defenseBonus}</span>
                        </div>
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">Production:</span>
                          <span className="font-semibold">{sector.stats.resourceProduction}</span>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

