import Navbar from "./components/Navbar";
import LoginPage from "./views/Login/LoginPage";
import { Routes, Route, Link } from "react-router-dom";
import CartePage from "./views/Carte/CartePage";
import JoueurPage from "./views/Joueur/JoueurPage";
import BoutiquePage from "./views/Boutique/BoutiquePage";
import ReglesPage from "./views/Regles/ReglesPage";

function App() {
  return (
    <div className="bg-dark text-light min-vh-100">
      <Navbar />
      <div className="container mt-4">
        <Routes>
          <Route path="/carte" element={<CartePage />} />
          <Route path="/joueur" element={<JoueurPage />} />
          <Route path="/boutique" element={<BoutiquePage />} />
          <Route path="/regles" element={<ReglesPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h2>Bienvenue dans le jeu</h2>} />
        </Routes>
      </div>
    </div>
  );
}
export default App;
