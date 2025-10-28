import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import CartePage from './pages/CartePage';
import JoueurPage from './pages/JoueurPage';
import BoutiquePage from './pages/BoutiquePage';
import ReglesPage from './pages/ReglesPage';
import './index.css';

function App() {
  return (
    <Provider store={store}>
      <Router>
        <div className="min-h-screen bg-background text-foreground">
          <Navbar />
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
        </div>
      </Router>
    </Provider>
  );
}

export default App;