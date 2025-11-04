import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { login, clearError } from '../store/authSlice';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  FormControlLabel,
  Checkbox,
  CircularProgress,
  Container,
  Avatar,
} from '@mui/material';
import { Shield } from '@mui/icons-material';
import '../styles/pages/LoginPage.scss';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);

  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { loading, error, isAuthenticated } = useAppSelector((state) => state.auth);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/carte');
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    return () => {
      dispatch(clearError());
    };
  }, [dispatch]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const result = await dispatch(login({ username, password, rememberMe }));
    if (login.fulfilled.match(result)) {
      navigate('/carte');
    }
  };

  return (
    <Box className="login-page">
      <Container maxWidth="sm">
        <Card className="login-card fade-in" elevation={8}>
          <CardContent sx={{ p: 4 }}>
            {/* Logo et titre */}
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 4 }}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  bgcolor: 'primary.main',
                  mb: 2,
                }}
              >
                <Shield sx={{ fontSize: 48 }} />
              </Avatar>
              <Typography
                variant="h3"
                sx={{
                  fontWeight: 700,
                  background: 'linear-gradient(135deg, #2196f3 0%, #64b5f6 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  mb: 1,
                }}
              >
                NML Online
              </Typography>
              <Typography variant="body1" color="text.secondary">
                Connectez-vous pour conquérir des territoires
              </Typography>
            </Box>

            {/* Formulaire */}
            <form onSubmit={handleSubmit}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                {/* Erreur */}
                {error && (
                  <Alert severity="error" variant="filled">
                    {error}
                  </Alert>
                )}

                {/* Nom d'utilisateur */}
                <TextField
                  fullWidth
                  label="Nom d'utilisateur"
                  variant="outlined"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  disabled={loading}
                  autoFocus
                />

                {/* Mot de passe */}
                <TextField
                  fullWidth
                  label="Mot de passe"
                  type="password"
                  variant="outlined"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  disabled={loading}
                />

                {/* Se souvenir de moi */}
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      disabled={loading}
                      color="primary"
                    />
                  }
                  label="Se souvenir de moi (30 jours)"
                />

                {/* Bouton de connexion */}
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  fullWidth
                  disabled={loading}
                  sx={{
                    py: 1.5,
                    fontSize: '1rem',
                    fontWeight: 600,
                  }}
                >
                  {loading ? (
                    <>
                      <CircularProgress size={24} sx={{ mr: 1 }} color="inherit" />
                      Connexion en cours...
                    </>
                  ) : (
                    'Se connecter'
                  )}
                </Button>
              </Box>
            </form>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}

