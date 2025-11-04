import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import { store } from './store';
import { theme } from './theme';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import CartePage from './pages/CartePage';
import JoueurPage from './pages/JoueurPage';
import BoutiquePage from './pages/BoutiquePage';
import ReglesPage from './pages/ReglesPage';
import './styles/global.scss';

function App() {
  return (
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Router>
          <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Navbar />
            <Box component="main" sx={{ flexGrow: 1 }}>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route
                  path="/carte"
                  element={
                    <ProtectedRoute>
                      <CartePage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/joueur"
                  element={
                    <ProtectedRoute>
                      <JoueurPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/boutique"
                  element={
                    <ProtectedRoute>
                      <BoutiquePage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/regles"
                  element={
                    <ProtectedRoute>
                      <ReglesPage />
                    </ProtectedRoute>
                  }
                />
                <Route path="/" element={<Navigate to="/carte" replace />} />
              </Routes>
            </Box>
          </Box>
        </Router>
      </ThemeProvider>
    </Provider>
  );
}

export default App;