import { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchEquipments, addToCart, removeFromCart, updateCartItemQuantity, clearCart } from '../store/shopSlice';
import { fetchCurrentPlayer } from '../store/playerSlice';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  CircularProgress,
  Alert,
  Container,
  Avatar,
  IconButton,
  Badge,
  Drawer,
  Divider,
  Paper,
  Grid,
} from '@mui/material';
import {
  ShoppingBag,
  Add,
  Remove,
  Delete,
  ShoppingCart,
  AttachMoney,
} from '@mui/icons-material';
import { Equipment } from '../types';
import '../styles/pages/BoutiquePage.scss';

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
    alert('Fonctionnalité d\'achat en cours d\'implémentation');
  };

  if (loading && equipments.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} className="fade-in">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Avatar
              sx={{
                width: 64,
                height: 64,
                bgcolor: 'primary.main',
              }}
            >
              <ShoppingBag sx={{ fontSize: 40 }} />
            </Avatar>
            <Box>
              <Typography
                variant="h3"
                sx={{
                  fontWeight: 700,
                  background: 'linear-gradient(135deg, #2196f3 0%, #64b5f6 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                }}
              >
                Boutique d'Équipements
              </Typography>
              <Typography variant="body1" color="text.secondary">
                Équipez vos troupes pour la victoire
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Paper
              sx={{
                px: 2,
                py: 1,
                bgcolor: 'warning.dark',
                border: '1px solid',
                borderColor: 'warning.main',
              }}
            >
              <Typography variant="caption" color="text.secondary">
                Argent disponible:
              </Typography>
              <Typography variant="h6" fontWeight={700} color="warning.main" sx={{ ml: 1, display: 'inline' }}>
                {currentPlayer?.stats.money.toFixed(0)} ₡
              </Typography>
            </Paper>

            <Badge badgeContent={totalItems} color="primary">
              <Button
                variant={showCart ? 'contained' : 'outlined'}
                startIcon={<ShoppingCart />}
                onClick={() => setShowCart(!showCart)}
                size="large"
              >
                Panier ({cart.length})
              </Button>
            </Badge>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" variant="filled">
            {error}
          </Alert>
        )}

        {/* Liste des équipements */}
        <Box>
          <Typography variant="h5" fontWeight={600} gutterBottom>
            Équipements disponibles
          </Typography>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            {equipments.map((equipment) => {
              const owned = getOwnedQuantity(equipment.name);
              const inCart = cart.find((item) => item.equipment.name === equipment.name)?.quantity || 0;

              return (
                <Grid xs={12} sm={6} md={4} key={equipment.name}>
                  <Card className="hover-lift" elevation={4}>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 2 }}>
                        <Typography variant="h6" fontWeight={600}>
                          {equipment.name}
                        </Typography>
                        <Chip
                          label={`${equipment.cost} ₡`}
                          color="warning"
                          sx={{ fontWeight: 600 }}
                        />
                      </Box>

                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                        {equipment.category}
                      </Typography>

                      {/* Bonus */}
                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
                        {equipment.pdfBonus > 0 && (
                          <Chip
                            label={`+${equipment.pdfBonus} PDF`}
                            size="small"
                            sx={{ bgcolor: 'error.dark', color: 'white', fontWeight: 600 }}
                          />
                        )}
                        {equipment.pdcBonus > 0 && (
                          <Chip
                            label={`+${equipment.pdcBonus} PDC`}
                            size="small"
                            sx={{ bgcolor: 'info.dark', color: 'white', fontWeight: 600 }}
                          />
                        )}
                        {equipment.armBonus > 0 && (
                          <Chip
                            label={`+${equipment.armBonus} ARM`}
                            size="small"
                            sx={{ bgcolor: 'success.dark', color: 'white', fontWeight: 600 }}
                          />
                        )}
                        {equipment.evasionBonus > 0 && (
                          <Chip
                            label={`+${equipment.evasionBonus} ESQ`}
                            size="small"
                            sx={{ bgcolor: 'warning.dark', color: 'white', fontWeight: 600 }}
                          />
                        )}
                      </Box>

                      {/* Classes compatibles */}
                      <Box sx={{ mb: 2 }}>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                          Compatible avec:
                        </Typography>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {equipment.compatibleClass && equipment.compatibleClass.length > 0 ? (
                            equipment.compatibleClass.map((unitClass) => (
                              <Chip
                                key={unitClass.code}
                                label={unitClass.name}
                                size="small"
                                variant="outlined"
                              />
                            ))
                          ) : (
                            <Typography variant="caption" color="text.secondary">
                              Aucune
                            </Typography>
                          )}
                        </Box>
                      </Box>

                      {/* Quantité possédée */}
                      {owned > 0 && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          Vous en possédez: <strong style={{ color: '#2196f3' }}>{owned}</strong>
                        </Typography>
                      )}

                      {/* Bouton d'ajout */}
                      <Button
                        fullWidth
                        variant="contained"
                        startIcon={<Add />}
                        onClick={() => handleAddToCart(equipment)}
                      >
                        Ajouter au panier
                        {inCart > 0 && ` (${inCart})`}
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>
        </Box>
      </Box>

      {/* Drawer Panier */}
      <Drawer
        anchor="right"
        open={showCart}
        onClose={() => setShowCart(false)}
        PaperProps={{
          sx: { width: { xs: '100%', sm: 400 }, p: 3 },
        }}
      >
        <Typography variant="h5" fontWeight={600} gutterBottom>
          Panier
        </Typography>
        <Divider sx={{ my: 2 }} />

        {cart.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 8 }}>
            <ShoppingCart sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
            <Typography variant="body1" color="text.secondary">
              Votre panier est vide
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 4 }}>
            {cart.map((item) => (
              <Paper key={item.equipment.name} sx={{ p: 2, bgcolor: 'background.default' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1 }}>
                  <Typography variant="subtitle1" fontWeight={600}>
                    {item.equipment.name}
                  </Typography>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleRemoveFromCart(item.equipment.name)}
                  >
                    <Delete />
                  </IconButton>
                </Box>

                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  {item.equipment.cost} ₡ × {item.quantity} = {item.equipment.cost * item.quantity} ₡
                </Typography>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <IconButton
                    size="small"
                    onClick={() => handleUpdateQuantity(item.equipment.name, Math.max(1, item.quantity - 1))}
                    disabled={item.quantity <= 1}
                  >
                    <Remove />
                  </IconButton>
                  <Typography variant="body1" fontWeight={600} sx={{ minWidth: 30, textAlign: 'center' }}>
                    {item.quantity}
                  </Typography>
                  <IconButton
                    size="small"
                    onClick={() => handleUpdateQuantity(item.equipment.name, item.quantity + 1)}
                  >
                    <Add />
                  </IconButton>
                </Box>
              </Paper>
            ))}
          </Box>
        )}

        {cart.length > 0 && (
          <>
            <Divider sx={{ my: 2 }} />
            <Box sx={{ mb: 3 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body1">Total:</Typography>
                <Typography variant="h6" fontWeight={700} color="warning.main">
                  {getTotalPrice()} ₡
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">
                  Argent disponible:
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {currentPlayer?.stats.money.toFixed(0)} ₡
                </Typography>
              </Box>
            </Box>

            <Button
              fullWidth
              variant="contained"
              size="large"
              color={canAfford() ? 'primary' : 'error'}
              disabled={!canAfford()}
              onClick={handleCheckout}
              startIcon={<AttachMoney />}
            >
              {canAfford() ? 'Acheter' : 'Fonds insuffisants'}
            </Button>

            <Button
              fullWidth
              variant="outlined"
              color="error"
              sx={{ mt: 1 }}
              onClick={() => dispatch(clearCart())}
            >
              Vider le panier
            </Button>
          </>
        )}
      </Drawer>
    </Container>
  );
}

