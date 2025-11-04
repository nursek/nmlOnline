import {
  Box,
  Card,
  CardContent,
  Typography,
  Container,
  Avatar,
  Paper,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Grid,
} from '@mui/material';
import {
  MenuBook,
  MyLocation,
  People,
  SportsEsports,
  EmojiEvents,
  TipsAndUpdates,
  ShoppingBag,
  Place,
  Security,
} from '@mui/icons-material';

export default function ReglesPage() {
  return (
    <Container maxWidth="lg" sx={{ py: 4 }} className="fade-in">
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
            <MenuBook sx={{ fontSize: 40 }} />
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
              Règles du Jeu
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Guide complet de NML Online
            </Typography>
          </Box>
        </Box>

        {/* But du jeu */}
        <Card elevation={4} sx={{ border: '2px solid', borderColor: 'primary.main' }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <MyLocation color="primary" />
              <Typography variant="h5" fontWeight={600}>
                But du jeu
              </Typography>
            </Box>
            <Typography variant="body1" sx={{ lineHeight: 1.8 }}>
              Contrôler des territoires et gérer les ressources pour devenir le joueur le plus puissant.
              Votre objectif est de conquérir et de maintenir le contrôle du plus grand nombre de territoires
              possible avant la fin du temps imparti.
            </Typography>
          </CardContent>
        </Card>

        {/* Déroulement */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <People color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Déroulement
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Comment se déroule une partie
            </Typography>

            <List>
              <ListItem sx={{ alignItems: 'flex-start', gap: 2 }}>
                <ListItemIcon>
                  <Avatar sx={{ bgcolor: 'primary.light', width: 40, height: 40 }}>
                    <Typography fontWeight={700}>1</Typography>
                  </Avatar>
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="h6" fontWeight={600} gutterBottom>
                      Recrutement de troupes
                    </Typography>
                  }
                  secondary={
                    <Typography variant="body2" color="text.secondary">
                      Les joueurs recrutent des troupes pour renforcer leur armée. Chaque unité
                      possède des caractéristiques spécifiques comme des points de vie et des points
                      de mouvement.
                    </Typography>
                  }
                />
              </ListItem>

              <ListItem sx={{ alignItems: 'flex-start', gap: 2 }}>
                <ListItemIcon>
                  <Avatar sx={{ bgcolor: 'primary.light', width: 40, height: 40 }}>
                    <Typography fontWeight={700}>2</Typography>
                  </Avatar>
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="h6" fontWeight={600} gutterBottom>
                      Achat d'équipements
                    </Typography>
                  }
                  secondary={
                    <Typography variant="body2" color="text.secondary">
                      Visitez la boutique pour acheter des équipements qui amélioreront les capacités
                      de vos unités. Les équipements offrent des bonus comme la force de frappe (PDF),
                      la défense (PDC), l'armure (ARM) et l'évasion (ESQ).
                    </Typography>
                  }
                />
              </ListItem>

              <ListItem sx={{ alignItems: 'flex-start', gap: 2 }}>
                <ListItemIcon>
                  <Avatar sx={{ bgcolor: 'primary.light', width: 40, height: 40 }}>
                    <Typography fontWeight={700}>3</Typography>
                  </Avatar>
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="h6" fontWeight={600} gutterBottom>
                      Capture de territoires
                    </Typography>
                  }
                  secondary={
                    <Typography variant="body2" color="text.secondary">
                      Utilisez vos troupes pour capturer des territoires ennemis ou neutres.
                      Chaque territoire contrôlé augmente votre influence et peut fournir des
                      ressources précieuses.
                    </Typography>
                  }
                />
              </ListItem>
            </List>

            <Paper
              sx={{
                p: 3,
                mt: 3,
                bgcolor: 'background.default',
                border: '1px solid',
                borderColor: 'divider',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <SportsEsports color="primary" />
                <Typography variant="h6" fontWeight={600}>
                  Système de combat
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Chaque unité possède des points de vie et de mouvement. Les combats se font en
                comparant la force des troupes opposées, en tenant compte des équipements et
                des bonus de territoire. La stratégie et le positionnement sont essentiels
                pour remporter la victoire !
              </Typography>
            </Paper>
          </CardContent>
        </Card>

        {/* Conditions de victoire */}
        <Card
          elevation={4}
          sx={{
            border: '2px solid',
            borderColor: 'warning.main',
            background: 'linear-gradient(135deg, rgba(255, 193, 7, 0.05) 0%, rgba(255, 193, 7, 0.02) 100%)',
          }}
        >
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <EmojiEvents sx={{ color: 'warning.main' }} />
              <Typography variant="h5" fontWeight={600}>
                Conditions de victoire
              </Typography>
            </Box>

            <Typography variant="body1" sx={{ mb: 4, lineHeight: 1.8 }}>
              Le joueur ayant le <strong style={{ color: '#2196f3' }}>plus de territoires</strong> à
              la fin du temps imparti remporte la partie.
            </Typography>

            <Grid container spacing={2}>
              <Grid xs={12} md={4}>
                <Paper
                  sx={{
                    p: 3,
                    bgcolor: 'rgba(255, 193, 7, 0.1)',
                    border: '1px solid',
                    borderColor: 'warning.main',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="h4" sx={{ mb: 1 }}>
                    🥇
                  </Typography>
                  <Typography variant="h6" fontWeight={600} color="warning.main" gutterBottom>
                    1ère Place
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Le commandant avec le plus de territoires
                  </Typography>
                </Paper>
              </Grid>

              <Grid xs={12} md={4}>
                <Paper
                  sx={{
                    p: 3,
                    bgcolor: 'rgba(158, 158, 158, 0.1)',
                    border: '1px solid',
                    borderColor: 'grey.600',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="h4" sx={{ mb: 1 }}>
                    🥈
                  </Typography>
                  <Typography variant="h6" fontWeight={600} color="grey.400" gutterBottom>
                    2ème Place
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Le deuxième plus grand conquérant
                  </Typography>
                </Paper>
              </Grid>

              <Grid xs={12} md={4}>
                <Paper
                  sx={{
                    p: 3,
                    bgcolor: 'rgba(255, 152, 0, 0.1)',
                    border: '1px solid',
                    borderColor: 'orange',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="h4" sx={{ mb: 1 }}>
                    🥉
                  </Typography>
                  <Typography variant="h6" fontWeight={600} color="orange" gutterBottom>
                    3ème Place
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Le troisième commandant
                  </Typography>
                </Paper>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Conseils stratégiques */}
        <Card elevation={4}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <TipsAndUpdates color="primary" />
              <Typography variant="h5" fontWeight={600}>
                Conseils stratégiques
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Pour devenir un grand conquérant
            </Typography>

            <Grid container spacing={2}>
              <Grid xs={12} md={6}>
                <Paper sx={{ p: 2, bgcolor: 'background.default', height: '100%' }}>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <ShoppingBag color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      Équipez intelligemment
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Investissez dans des équipements adaptés à vos unités. Un bon équipement peut
                    faire la différence dans les batailles critiques.
                  </Typography>
                </Paper>
              </Grid>

              <Grid xs={12} md={6}>
                <Paper sx={{ p: 2, bgcolor: 'background.default', height: '100%' }}>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <Place color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      Contrôlez les ressources
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Les territoires génèrent des revenus. Plus vous en contrôlez, plus vous
                    pourrez investir dans votre armée.
                  </Typography>
                </Paper>
              </Grid>

              <Grid xs={12} md={6}>
                <Paper sx={{ p: 2, bgcolor: 'background.default', height: '100%' }}>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <Security color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      Défendez vos positions
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Ne vous concentrez pas uniquement sur l'attaque. Assurez-vous que vos
                    territoires sont bien défendus contre les invasions.
                  </Typography>
                </Paper>
              </Grid>

              <Grid xs={12} md={6}>
                <Paper sx={{ p: 2, bgcolor: 'background.default', height: '100%' }}>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <People color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      Gérez vos ressources
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Ne dépensez pas tout votre argent d'un coup. Gardez une réserve pour
                    réagir aux opportunités et menaces.
                  </Typography>
                </Paper>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}

