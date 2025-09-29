import React, { useState, useContext } from "react";
import { UserContext } from "../context/UserContext";

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const userContext = useContext(UserContext);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const res = await fetch("/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    if (res.ok) {
      const data = await res.json();
      userContext?.setUser({ id: data.id, name: data.name, money: data.money });
      localStorage.setItem("token", data.token);
    } else {
      alert("Login échoué");
    }
  };

  return (
    <form onSubmit={handleLogin} className="bg-dark text-light p-4 rounded">
      <h2>Connexion</h2>
      <div className="mb-3">
        <input
          className="form-control"
          value={username}
          onChange={e => setUsername(e.target.value)}
          placeholder="Utilisateur"
        />
      </div>
      <div className="mb-3">
        <input
          className="form-control"
          type="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          placeholder="Mot de passe"
        />
      </div>
      <button type="submit" className="btn btn-primary">Connexion</button>
    </form>
  );
};

export default LoginPage;