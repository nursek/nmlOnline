import React, { useContext, useEffect, useState } from 'react';
import { UserContext } from '../../context/UserContext';

interface Unit {
  id: number;
  name: string;
  troops: string[];
  equipment: string[];
}

const JoueurPage: React.FC = () => {
  const userContext = useContext(UserContext);
  const user = userContext?.user;
  const [units, setUnits] = useState<Unit[]>([]);

  useEffect(() => {
    /*if (user) {
      fetch(`/api/players/${user.id}/units`)
        .then(async res => {
          if (!res.ok) {
            const text = await res.text();
            throw new Error(text || "Erreur lors du chargement des unités");
          }
          return res.json();
        })
        .then(data => setUnits(data))
        .catch(err => {
          console.error(err);
          setUnits([]); // Vide la liste en cas d’erreur
        });
    }*/ // TODO : API
  }, [user]);

  return (
    <div>
      <h2>Unités du joueur {user?.name}</h2>
      <div className="row">
        {units.map(u => (
          <div key={u.id} className="card bg-dark text-white m-2" style={{ width: '18rem' }}>
            <div className="card-body">
              <h5 className="card-title">{u.name}</h5>
              <p className="card-text">
                Troupes : {u.troops.join(', ')}
              </p>
              <p className="card-text">
                Équipements : {u.equipment.join(', ')}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default JoueurPage;