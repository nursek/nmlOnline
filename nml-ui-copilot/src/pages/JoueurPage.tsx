import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchCurrentPlayer } from '../store/playerSlice';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  Chip,
  CircularProgress,
  Alert,
  Container,
  Paper,
  Divider,
  Grid,
} from '@mui/material';
import {
  Person,
  AttachMoney,
  TrendingUp,
  Place,
  Inventory,
  Shield as ShieldIcon,
} from '@mui/icons-material';
import '../styles/pages/JoueurPage.scss';

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
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error) {
    return (
      <Container maxWidth="xl" sx={{ py: 4 }}>
        <Alert severity="error" variant="filled">
          {error}
        </Alert>
      </Container>
    );
  }

  if (!currentPlayer) {
    return null;
  }

  const stats = [
    {
      label: 'Argent',
      value: `${currentPlayer.stats.money.toFixed(0)} ₡`,
      icon: AttachMoney,
      color: '#ffc107',
    },
    {
      label: 'Revenus',
      value: `${currentPlayer.stats.totalIncome.toFixed(0)} ₡/tour`,
      icon: TrendingUp,
      color: '#4caf50',
    },
    {
      label: 'Puissance globale',
      value: currentPlayer.stats.globalPower.toFixed(0),
      icon: ShieldIcon,
      color: '#2196f3',
    },
    {
      label: 'Territoires',
      value: currentPlayer.sectors.length,
      icon: Place,
      color: '#9c27b0',
    },
  ];

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} className="fade-in">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar
            sx={{
              width: 80,
              height: 80,
              bgcolor: 'primary.main',
            }}
          >
            <Person sx={{ fontSize: 48 }} />
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
              {currentPlayer.name}
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Commandant en chef
            </Typography>
          </Box>
        </Box>

        {/* Stats Cards */}
        <Grid container spacing={3}>
          {stats.map((stat) => (
            <Grid xs={12} sm={6} md={3} key={stat.label}>
              <Card className="hover-lift" elevation={4}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        {stat.label}
                      </Typography>
                      <Typography variant="h4" fontWeight={700}>
                        {stat.value}
                      </Typography>
                    </Box>
                    <Avatar
                      sx={{
                        width: 56,
                        height: 56,
                        bgcolor: `${stat.color}20`,
                      }}
                    >
                      <stat.icon sx={{ fontSize: 32, color: stat.color }} />
                    </Avatar>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Statistiques détaillées */}
        <Card elevation={4}>
          <CardContent>
            <Typography variant="h5" fontWeight={600} gutterBottom>
              Statistiques détaillées
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Analyse complète de votre puissance
            </Typography>
            <Grid container spacing={2}>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Puissance offensive
                  </Typography>
                  <Typography variant="h5" fontWeight={700} color="error.main">
                    {currentPlayer.stats.totalOffensivePower.toFixed(0)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Puissance défensive
                  </Typography>
                  <Typography variant="h5" fontWeight={700} color="success.main">
                    {currentPlayer.stats.totalDefensivePower.toFixed(0)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Puissance économique
                  </Typography>
                  <Typography variant="h5" fontWeight={700} color="warning.main">
                    {currentPlayer.stats.totalEconomyPower.toFixed(0)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Valeur des véhicules
                  </Typography>
                  <Typography variant="h5" fontWeight={700}>
                    {currentPlayer.stats.totalVehiclesValue.toFixed(0)} ₡
                  </Typography>
                </Paper>
              </Grid>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Valeur des équipements
                  </Typography>
                  <Typography variant="h5" fontWeight={700}>
                    {currentPlayer.stats.totalEquipmentValue.toFixed(0)} ₡
                  </Typography>
                </Paper>
              </Grid>
              <Grid xs={12} sm={6} md={4}>
                <Paper sx={{ p: 2, bgcolor: 'background.default' }}>
                  <Typography variant="body2" color="text.secondary">
                    Armure totale
                  </Typography>
                  <Typography variant="h5" fontWeight={700}>
                    {currentPlayer.stats.totalArmor.toFixed(0)}
                  </Typography>
                </Paper>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Équipements */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
              <Inventory color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Équipements possédés
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Votre arsenal actuel
            </Typography>
            {currentPlayer.equipments.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <Inventory sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
                <Typography variant="body1" color="text.secondary">
                  Aucun équipement pour le moment. Visitez la boutique !
                </Typography>
              </Box>
            ) : (
              <Grid container spacing={2}>
                {currentPlayer.equipments.map((stack) => (
                  <Grid xs={12} sm={6} md={4} key={stack.equipment.name}>
                    <Paper
                      sx={{
                        p: 2,
                        bgcolor: 'background.default',
                        border: '1px solid',
                        borderColor: 'divider',
                        transition: 'all 0.3s',
                        '&:hover': {
                          borderColor: 'primary.main',
                          transform: 'translateY(-4px)',
                          boxShadow: 4,
                        },
                      }}
                    >
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                        <Box sx={{ flex: 1 }}>
                          <Typography variant="h6" fontWeight={600}>
                            {stack.equipment.name}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {stack.equipment.category}
                          </Typography>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                            {stack.equipment.pdfBonus > 0 && (
                              <Chip
                                label={`+${stack.equipment.pdfBonus} PDF`}
                                size="small"
                                sx={{ bgcolor: 'error.dark', color: 'white' }}
                              />
                            )}
                            {stack.equipment.pdcBonus > 0 && (
                              <Chip
                                label={`+${stack.equipment.pdcBonus} PDC`}
                                size="small"
                                sx={{ bgcolor: 'info.dark', color: 'white' }}
                              />
                            )}
                            {stack.equipment.armBonus > 0 && (
                              <Chip
                                label={`+${stack.equipment.armBonus} ARM`}
                                size="small"
                                sx={{ bgcolor: 'success.dark', color: 'white' }}
                              />
                            )}
                            {stack.equipment.evasionBonus > 0 && (
                              <Chip
                                label={`+${stack.equipment.evasionBonus} ESQ`}
                                size="small"
                                sx={{ bgcolor: 'warning.dark', color: 'white' }}
                              />
                            )}
                          </Box>
                        </Box>
                        <Typography variant="h4" fontWeight={700} color="primary.main" sx={{ ml: 2 }}>
                          ×{stack.quantity}
                        </Typography>
                      </Box>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            )}
          </CardContent>
        </Card>

        {/* Territoires */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
              <Place color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Territoires contrôlés
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Les secteurs sous votre commandement
            </Typography>
            {currentPlayer.sectors.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <Place sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
                <Typography variant="body1" color="text.secondary">
                  Aucun territoire contrôlé. Partez à la conquête !
                </Typography>
              </Box>
            ) : (
              <Grid container spacing={2}>
                {currentPlayer.sectors.map((sector, index) => (
                  <Grid xs={12} sm={6} md={4} key={`sector-${sector.number ?? index}`}>
                    <Paper
                      sx={{
                        p: 2,
                        bgcolor: 'background.default',
                        border: '2px solid',
                        borderColor: 'primary.main',
                        transition: 'all 0.3s',
                        '&:hover': {
                          transform: 'translateY(-4px)',
                          boxShadow: 4,
                        },
                      }}
                    >
                      <Typography variant="h6" fontWeight={600}>
                        {sector.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
                        Secteur #{sector.number ?? 'N/A'}
                      </Typography>
                      <Divider sx={{ my: 1 }} />
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Typography variant="body2" color="text.secondary">
                            Revenus:
                          </Typography>
                          <Typography variant="body2" fontWeight={600} color="warning.main">
                            {sector.income ?? 0} ₡/tour
                          </Typography>
                        </Box>
                        {sector.army && sector.army.length > 0 && (
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="body2" color="text.secondary">
                              Unités:
                            </Typography>
                            <Typography variant="body2" fontWeight={600}>
                              {sector.army.length}
                            </Typography>
                          </Box>
                        )}
                        {sector.stats && (
                          <>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                              <Typography variant="body2" color="text.secondary">
                                Défense:
                              </Typography>
                              <Typography variant="body2" fontWeight={600}>
                                +{sector.stats.defenseBonus}
                              </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                              <Typography variant="body2" color="text.secondary">
                                Production:
                              </Typography>
                              <Typography variant="body2" fontWeight={600}>
                                {sector.stats.resourceProduction}
                              </Typography>
                            </Box>
                          </>
                        )}
                      </Box>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            )}
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}

