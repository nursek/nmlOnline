import React, { useState } from "react";

interface ZoneInfo {
  id: string;
  name: string;
  owner: string;
  troops: number;
}

const CartePage: React.FC = () => {
  const [selectedZone, setSelectedZone] = useState<ZoneInfo | null>(null);

  const handleZoneClick = (id: string) => {
    // Exemple : appel à ton backend pour récupérer les infos
    fetch(`/api/zones/${id}`)
      .then((res) => res.json())
      .then((data) => setSelectedZone(data))
      .catch(console.error);
  };

  return (
    <div className="row">
      {/* Carte */}
      <div className="col-md-8">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 1000 600"
          style={{ width: "100%", border: "1px solid #444" }}
        >
          <path
            d="M100,100 L300,100 L300,300 L100,300 Z"
            fill="red"
            stroke="black"
            onClick={() => handleZoneClick("zone1")}
            style={{ cursor: "pointer" }}
          />
          <path
            d="M350,100 L550,100 L550,300 L350,300 Z"
            fill="green"
            stroke="black"
            onClick={() => handleZoneClick("zone2")}
            style={{ cursor: "pointer" }}
          />
          <path
            d="M600,100 L800,100 L800,300 L600,300 Z"
            fill="blue"
            stroke="black"
            onClick={() => handleZoneClick("zone3")}
            style={{ cursor: "pointer" }}
          />
        </svg>
      </div>

      {/* Panneau d'infos */}
      <div className="col-md-4 bg-dark text-light p-3">
        {selectedZone ? (
          <>
            <h4>{selectedZone.name}</h4>
            <p>Contrôlée par : {selectedZone.owner}</p>
            <p>Troupes : {selectedZone.troops}</p>
          </>
        ) : (
          <p>Cliquez sur une zone pour voir les détails</p>
        )}
      </div>
    </div>
  );
};

export default CartePage;