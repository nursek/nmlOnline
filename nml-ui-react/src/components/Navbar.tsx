import React, { useContext } from 'react';
import { Link } from 'react-router-dom';
import { UserContext } from '../context/UserContext';

const Navbar: React.FC = () => {
  const userContext = useContext(UserContext);
  const user = userContext?.user;
  const setUser = userContext?.setUser;

  const handleAuth = () => {
    if (user) {
      // Déconnexion
      setUser && setUser(null);
    } else {
      // Simulation de connexion
      const demoUser = { id: '1', name: 'Joueur1', money: 100 };
      setUser && setUser(demoUser);
    }
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
      <div className="container-fluid">
        <Link className="navbar-brand" to="/">GameInterface</Link>
        <div className="navbar-nav">
          <Link className="nav-link" to="/carte">CARTE</Link>
          <Link className="nav-link" to="/joueur">JOUEUR</Link>
          <Link className="nav-link" to="/boutique">BOUTIQUE</Link>
          <Link className="nav-link" to="/regles">RÈGLES</Link>
        </div>
        <button className="btn btn-outline-light ms-auto" onClick={handleAuth}>
          {user ? `Déconnexion (${user.name})` : 'Connexion'}
        </button>
      </div>
    </nav>
  );
};

export default Navbar;