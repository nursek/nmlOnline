import { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchEquipments, addToCart, removeFromCart, updateCartItemQuantity, clearCart } from '../store/shopSlice';
import { fetchCurrentPlayer } from '../store/playerSlice';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { ShoppingBag, Loader2, Plus, Minus, Trash2, ShoppingCart } from 'lucide-react';
import { Equipment, CartItem } from '../types';
import { cn } from '../lib/utils';

export default function BoutiquePage() {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const { equipments, cart, loading, error } = useAppSelector((state) => state.shop);
  const { currentPlayer } = useAppSelector((state) => state.player);
  const [showCart, setShowCart] = useState(false);

  useEffect(() => {
    dispatch(fetchEquipments());
    if (user?.username) {
      dispatch(fetchCurrentPlayer(user.username));
    }
  }, [dispatch, user]);

  const handleAddToCart = (equipment: Equipment) => {
    dispatch(addToCart(equipment));
  };

  const handleRemoveFromCart = (name: string) => {
    dispatch(removeFromCart(name));
  };

  const handleUpdateQuantity = (name: string, quantity: number) => {
    dispatch(updateCartItemQuantity({ name, quantity }));
  };

  const getTotalPrice = () => {
    return cart.reduce((total, item) => total + item.equipment.cost * item.quantity, 0);
  };

  const getOwnedQuantity = (equipmentName: string) => {
    const stack = currentPlayer?.equipments.find((e) => e.equipment.name === equipmentName);
    return stack?.quantity || 0;
  };

  const canAfford = () => {
    return currentPlayer && currentPlayer.stats.money >= getTotalPrice();
  };

  const handleCheckout = () => {
    // TODO: Implémenter l'appel API pour acheter les équipements
    alert('Fonctionnalité d\'achat en cours d\'implémentation');
    // dispatch(clearCart());
  };

  if (loading && equipments.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <Loader2 className="h-12 w-12 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center space-x-4">
          <div className="p-3 bg-primary/10 rounded-full">
            <ShoppingBag className="h-10 w-10 text-primary" />
          </div>
          <div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
              Boutique d'Équipements
            </h1>
            <p className="text-muted-foreground">Équipez vos troupes pour la victoire</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div className="px-4 py-2 bg-yellow-500/10 border border-yellow-500/30 rounded-lg">
            <span className="text-sm text-muted-foreground">Argent disponible:</span>
            <span className="ml-2 text-lg font-bold text-yellow-500">
              {currentPlayer?.stats.money.toFixed(0)} ₡
            </span>
          </div>

          <Button
            onClick={() => setShowCart(!showCart)}
            className="relative"
            variant={showCart ? 'default' : 'outline'}
          >
            <ShoppingCart className="h-5 w-5 mr-2" />
            Panier ({cart.length})
            {cart.length > 0 && (
              <span className="absolute -top-2 -right-2 bg-primary text-primary-foreground rounded-full h-6 w-6 flex items-center justify-center text-xs font-bold">
                {cart.reduce((sum, item) => sum + item.quantity, 0)}
              </span>
            )}
          </Button>
        </div>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive text-destructive px-4 py-3 rounded-md mb-6">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Liste des équipements */}
        <div className={cn('space-y-4', showCart ? 'lg:col-span-2' : 'lg:col-span-3')}>
          <h2 className="text-2xl font-bold flex items-center space-x-2">
            <span>Équipements disponibles</span>
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {equipments.map((equipment) => {
              const owned = getOwnedQuantity(equipment.name);
              const inCart = cart.find((item) => item.equipment.name === equipment.name)?.quantity || 0;

              return (
                <Card key={equipment.name} className="border-2 hover:shadow-xl transition-all hover:scale-[1.02]">
                  <CardHeader>
                    <CardTitle className="flex items-center justify-between">
                      <span>{equipment.name}</span>
                      <span className="text-yellow-500 text-lg">{equipment.cost} ₡</span>
                    </CardTitle>
                    <CardDescription>{equipment.category}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {/* Bonus */}
                      <div className="flex flex-wrap gap-2">
                        {equipment.pdfBonus > 0 && (
                          <span className="px-2 py-1 bg-red-500/10 text-red-400 rounded text-sm font-semibold">
                            +{equipment.pdfBonus} PDF
                          </span>
                        )}
                        {equipment.pdcBonus > 0 && (
                          <span className="px-2 py-1 bg-blue-500/10 text-blue-400 rounded text-sm font-semibold">
                            +{equipment.pdcBonus} PDC
                          </span>
                        )}
                        {equipment.armBonus > 0 && (
                          <span className="px-2 py-1 bg-green-500/10 text-green-400 rounded text-sm font-semibold">
                            +{equipment.armBonus} ARM
                          </span>
                        )}
                        {equipment.evasionBonus > 0 && (
                          <span className="px-2 py-1 bg-yellow-500/10 text-yellow-400 rounded text-sm font-semibold">
                            +{equipment.evasionBonus} ESQ
                          </span>
                        )}
                      </div>

                      {/* Classes compatibles */}
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Compatible avec:</p>
                        <div className="flex flex-wrap gap-1">
                          {equipment.compatibleClass && equipment.compatibleClass.length > 0 ? (
                            equipment.compatibleClass.map((unitClass) => (
                              <span key={unitClass.code} className="px-2 py-0.5 bg-secondary text-xs rounded">
                                {unitClass.name}
                              </span>
                            ))
                          ) : (
                            <span className="text-xs text-muted-foreground">Aucune</span>
                          )}
                        </div>
                      </div>

                      {/* Quantité possédée */}
                      {owned > 0 && (
                        <div className="text-sm text-muted-foreground">
                          Vous en possédez: <span className="font-bold text-primary">{owned}</span>
                        </div>
                      )}

                      {/* Bouton d'ajout */}
                      <Button
                        onClick={() => handleAddToCart(equipment)}
                        className="w-full"
                        size="sm"
                      >
                        <Plus className="h-4 w-4 mr-2" />
                        Ajouter au panier
                        {inCart > 0 && <span className="ml-2">({inCart} dans le panier)</span>}
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Panier */}
        {showCart && (
          <div className="lg:col-span-1">
            <Card className="border-2 sticky top-20">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <ShoppingCart className="h-5 w-5" />
                  <span>Panier</span>
                </CardTitle>
                <CardDescription>Votre sélection actuelle</CardDescription>
              </CardHeader>
              <CardContent>
                {cart.length === 0 ? (
                  <p className="text-muted-foreground text-center py-8">Votre panier est vide</p>
                ) : (
                  <div className="space-y-4">
                    {cart.map((item: CartItem) => (
                      <div key={item.equipment.name} className="flex items-center justify-between p-3 bg-secondary rounded-lg">
                        <div className="flex-1">
                          <h4 className="font-semibold text-sm">{item.equipment.name}</h4>
                          <p className="text-xs text-muted-foreground">
                            {item.equipment.cost} ₡ × {item.quantity} = {(item.equipment.cost * item.quantity).toFixed(0)} ₡
                          </p>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Button
                            size="icon"
                            variant="outline"
                            onClick={() => handleUpdateQuantity(item.equipment.name, item.quantity - 1)}
                            className="h-8 w-8"
                          >
                            <Minus className="h-3 w-3" />
                          </Button>
                          <span className="font-bold w-8 text-center">{item.quantity}</span>
                          <Button
                            size="icon"
                            variant="outline"
                            onClick={() => handleUpdateQuantity(item.equipment.name, item.quantity + 1)}
                            className="h-8 w-8"
                          >
                            <Plus className="h-3 w-3" />
                          </Button>
                          <Button
                            size="icon"
                            variant="destructive"
                            onClick={() => handleRemoveFromCart(item.equipment.name)}
                            className="h-8 w-8"
                          >
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                    ))}

                    <div className="border-t border-border pt-4 space-y-3">
                      <div className="flex justify-between text-lg font-bold">
                        <span>Total:</span>
                        <span className="text-yellow-500">{getTotalPrice().toFixed(0)} ₡</span>
                      </div>

                      {!canAfford() && (
                        <p className="text-destructive text-sm">
                          Fonds insuffisants !
                        </p>
                      )}

                      <Button
                        onClick={handleCheckout}
                        disabled={!canAfford()}
                        className="w-full"
                      >
                        <ShoppingBag className="h-4 w-4 mr-2" />
                        Acheter ({getTotalPrice().toFixed(0)} ₡)
                      </Button>

                      <Button
                        onClick={() => dispatch(clearCart())}
                        variant="outline"
                        className="w-full"
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Vider le panier
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}

