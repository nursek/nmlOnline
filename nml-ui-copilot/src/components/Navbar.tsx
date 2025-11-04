import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/authSlice';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Chip,
  Container,
  IconButton,
  useMediaQuery,
  useTheme,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Shield,
  Person,
  ShoppingBag,
  Map,
  MenuBook,
  Logout,
  Menu as MenuIcon,
} from '@mui/icons-material';
import { useState } from 'react';

export default function Navbar() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/login');
  };

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  if (!isAuthenticated) {
    return null;
  }

  const menuItems = [
    { path: '/carte', label: 'Carte', icon: <Map /> },
    { path: '/joueur', label: 'Mon Joueur', icon: <Person /> },
    { path: '/boutique', label: 'Boutique', icon: <ShoppingBag /> },
    { path: '/regles', label: 'Règles', icon: <MenuBook /> },
  ];

  const drawer = (
    <Box onClick={handleDrawerToggle} sx={{ width: 250, pt: 2 }}>
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton
              component={Link}
              to={item.path}
              selected={location.pathname === item.path}
            >
              <ListItemIcon sx={{ color: location.pathname === item.path ? 'primary.main' : 'text.secondary' }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <>
      <AppBar position="sticky" elevation={4}>
        <Container maxWidth="xl">
          <Toolbar disableGutters>
            {/* Logo */}
            <Shield sx={{ display: 'flex', mr: 1, fontSize: 32, color: 'primary.main' }} />
            <Typography
              variant="h5"
              component={Link}
              to="/"
              sx={{
                mr: 4,
                fontWeight: 700,
                background: 'linear-gradient(135deg, #2196f3 0%, #64b5f6 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                textDecoration: 'none',
              }}
            >
              NML Online
            </Typography>

            {/* Menu mobile */}
            {isMobile && (
              <IconButton
                color="inherit"
                aria-label="open drawer"
                edge="start"
                onClick={handleDrawerToggle}
                sx={{ mr: 2 }}
              >
                <MenuIcon />
              </IconButton>
            )}

            {/* Menu desktop */}
            {!isMobile && (
              <Box sx={{ flexGrow: 1, display: 'flex', gap: 1 }}>
                {menuItems.map((item) => (
                  <Button
                    key={item.path}
                    component={Link}
                    to={item.path}
                    startIcon={item.icon}
                    variant={location.pathname === item.path ? 'contained' : 'text'}
                    color={location.pathname === item.path ? 'primary' : 'inherit'}
                    sx={{
                      color: location.pathname === item.path ? 'white' : 'text.secondary',
                      '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.08)',
                      },
                    }}
                  >
                    {item.label}
                  </Button>
                ))}
              </Box>
            )}

            <Box sx={{ flexGrow: 1 }} />

            {/* User info et logout */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Chip
                icon={<Person />}
                label={user?.username}
                color="primary"
                variant="outlined"
                sx={{ fontWeight: 600 }}
              />
              <Button
                variant="outlined"
                color="error"
                startIcon={<Logout />}
                onClick={handleLogout}
                sx={{
                  display: { xs: 'none', sm: 'flex' },
                }}
              >
                Déconnexion
              </Button>
              <IconButton
                color="error"
                onClick={handleLogout}
                sx={{
                  display: { xs: 'flex', sm: 'none' },
                }}
              >
                <Logout />
              </IconButton>
            </Box>
          </Toolbar>
        </Container>
      </AppBar>

      {/* Mobile drawer */}
      <Drawer
        anchor="left"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        ModalProps={{
          keepMounted: true,
        }}
      >
        {drawer}
      </Drawer>
    </>
  );
}

