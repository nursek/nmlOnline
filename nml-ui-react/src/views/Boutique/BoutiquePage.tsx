import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../../context/UserContext';

interface Equipment {
  id: number;
  name: string;
  price: number;
}

const BoutiquePage: React.FC = () => {
  const userContext = useContext(UserContext);
  const user = userContext?.user;
  const [items, setItems] = useState<Equipment[]>([]);

  useEffect(() => {
    fetch(`/api/equipment`)
      .then(res => res.json())
      .then(data => setItems(data))
      .catch(err => console.error(err));
  }, []);

  const handleBuy = (item: Equipment) => {
    if (user && user.money >= item.price) {
      fetch(`/api/players/${user.id}/purchase`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ itemId: item.id })
      })
      .then(res => res.json())
      .then(data => {
        console.log('Achat réussi', data);
        // On pourrait mettre à jour l’état du user ou les items ici
      })
      .catch(err => console.error(err));
    } else {
      alert("Fonds insuffisants !");
    }
  };

  return (
    <div>
      <h2>Boutique (Argent disponible : {user?.money} crédits)</h2>
      <div className="row">
        {items.map(item => (
          <div key={item.id} className="card bg-dark text-white m-2" style={{ width: '18rem' }}>
            <div className="card-body">
              <h5 className="card-title">{item.name}</h5>
              <p className="card-text">Prix : {item.price}</p>
              <button
                className="btn btn-primary"
                disabled={!user || (user.money < item.price)}
                onClick={() => handleBuy(item)}
              >
                Acheter
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default BoutiquePage;