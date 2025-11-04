import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchAllPlayers } from '../store/playerSlice';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  CircularProgress,
  Alert,
  Container,
  Paper,
  Avatar,
  Divider,
  Grid,
} from '@mui/material';
import { Map, Person, Place } from '@mui/icons-material';
import '../styles/pages/CartePage.scss';

export default function CartePage() {
  const dispatch = useAppDispatch();
  const { players, loading, error } = useAppSelector((state) => state.player);

  useEffect(() => {
    dispatch(fetchAllPlayers());
  }, [dispatch]);

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

  // Couleurs pour différencier les joueurs
  const playerColors = [
    '#2196f3',
    '#f44336',
    '#4caf50',
    '#ffc107',
    '#9c27b0',
    '#e91e63',
    '#ff9800',
    '#00bcd4',
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

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} className="fade-in">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar
            sx={{
              width: 64,
              height: 64,
              bgcolor: 'primary.main',
            }}
          >
            <Map sx={{ fontSize: 40 }} />
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
              Carte du Monde
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Vue d'ensemble des territoires conquis
            </Typography>
          </Box>
        </Box>

        {/* Légende des joueurs */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
              <Person color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Joueurs actifs
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Commandants en présence
            </Typography>
            <Grid container spacing={2}>
              {players.map((player, idx) => (
                <Grid xs={12} sm={6} md={3} key={`player-${player.id ?? idx}`}>
                  <Paper
                    sx={{
                      p: 2,
                      bgcolor: 'background.default',
                      border: '2px solid',
                      borderColor: getPlayerColor(idx),
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                    }}
                  >
                    <Box
                      sx={{
                        width: 16,
                        height: 16,
                        borderRadius: '50%',
                        bgcolor: getPlayerColor(idx),
                        flexShrink: 0,
                      }}
                    />
                    <Box>
                      <Typography variant="body1" fontWeight={600}>
                        {player.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {player.sectors.length} {player.sectors.length > 1 ? 'territoires' : 'territoire'}
                      </Typography>
                    </Box>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>

        {/* Carte */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
              <Place color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Carte des territoires
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {allSectors.length} secteur{allSectors.length > 1 ? 's' : ''} contrôlé{allSectors.length > 1 ? 's' : ''}
            </Typography>
            {allSectors.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <Map sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  Aucun territoire n'a encore été conquis
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  La carte est vide pour le moment
                </Typography>
              </Box>
            ) : (
              <Grid container spacing={2}>
                {allSectors.map((sector, index) => (
                  <Grid xs={12} sm={6} md={4} lg={3} key={`${sector.playerName}-${sector.number ?? index}`}>
                    <Paper
                      className="territory-card"
                      sx={{
                        p: 2,
                        bgcolor: 'background.default',
                        border: '2px solid',
                        borderColor: sector.playerColor,
                        transition: 'all 0.3s',
                        cursor: 'pointer',
                        '&:hover': {
                          transform: 'translateY(-4px)',
                          boxShadow: 4,
                        },
                      }}
                    >
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1 }}>
                        <Box
                          sx={{
                            width: 16,
                            height: 16,
                            borderRadius: '50%',
                            bgcolor: sector.playerColor,
                          }}
                        />
                        <Chip
                          label={`#${sector.number ?? 'N/A'}`}
                          size="small"
                          sx={{ fontWeight: 600 }}
                        />
                      </Box>
                      <Typography variant="h6" fontWeight={600} gutterBottom>
                        {sector.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                        {sector.playerName}
                      </Typography>
                      <Divider sx={{ my: 1 }} />
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Typography variant="caption" color="text.secondary">
                            Revenus:
                          </Typography>
                          <Typography variant="caption" fontWeight={600} color="warning.main">
                            {sector.income ?? 0} ₡
                          </Typography>
                        </Box>
                        {sector.army && sector.army.length > 0 && (
                          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                            <Typography variant="caption" color="text.secondary">
                              Armée:
                            </Typography>
                            <Typography variant="caption" fontWeight={600}>
                              {sector.army.length} unités
                            </Typography>
                          </Box>
                        )}
                      </Box>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            )}
          </CardContent>
        </Card>

        {/* Statistiques des joueurs */}
        <Typography variant="h5" fontWeight={600}>
          Statistiques des joueurs
        </Typography>
        <Grid container spacing={3}>
          {players.map((player, idx) => (
            <Grid xs={12} sm={6} md={4} key={`player-stats-${player.id ?? idx}`}>
              <Card elevation={4} className="hover-lift">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <Box
                      sx={{
                        width: 16,
                        height: 16,
                        borderRadius: '50%',
                        bgcolor: getPlayerColor(idx),
                      }}
                    />
                    <Typography variant="h6" fontWeight={600}>
                      {player.name}
                    </Typography>
                  </Box>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                    {player.sectors.length} territoire{player.sectors.length > 1 ? 's' : ''}
                  </Typography>
                  <Divider sx={{ my: 1 }} />
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">
                        Puissance globale:
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {player.stats.globalPower.toFixed(0)}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">
                        Argent:
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {player.stats.money.toFixed(0)} ₡
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">
                        Revenus:
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {player.stats.totalIncome.toFixed(0)} ₡/tour
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">
                        Territoires:
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {player.sectors.length}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>
    </Container>
  );
}

