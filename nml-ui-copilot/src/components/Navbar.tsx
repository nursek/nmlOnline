import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/authSlice';
import { Button } from './ui/Button';
import { Shield, User, ShoppingBag, Map, BookOpen, LogOut } from 'lucide-react';

export default function Navbar() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);

  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/login');
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <nav className="bg-card border-b border-border sticky top-0 z-50 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center space-x-8">
            <Link to="/" className="flex items-center space-x-2">
              <Shield className="h-8 w-8 text-primary" />
              <span className="text-2xl font-bold bg-gradient-to-r from-primary to-blue-400 bg-clip-text text-transparent">
                NML Online
              </span>
            </Link>

            <div className="hidden md:flex space-x-4">
              <Link to="/carte">
                <Button variant="ghost" className="flex items-center space-x-2">
                  <Map className="h-5 w-5" />
                  <span>Carte</span>
                </Button>
              </Link>

              <Link to="/joueur">
                <Button variant="ghost" className="flex items-center space-x-2">
                  <User className="h-5 w-5" />
                  <span>Mon Joueur</span>
                </Button>
              </Link>

              <Link to="/boutique">
                <Button variant="ghost" className="flex items-center space-x-2">
                  <ShoppingBag className="h-5 w-5" />
                  <span>Boutique</span>
                </Button>
              </Link>

              <Link to="/regles">
                <Button variant="ghost" className="flex items-center space-x-2">
                  <BookOpen className="h-5 w-5" />
                  <span>Règles</span>
                </Button>
              </Link>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 px-3 py-1 bg-secondary rounded-lg">
              <User className="h-4 w-4 text-primary" />
              <span className="text-sm font-semibold">{user?.username}</span>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              className="flex items-center space-x-2"
            >
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Déconnexion</span>
            </Button>
          </div>
        </div>
      </div>
    </nav>
  );
}

