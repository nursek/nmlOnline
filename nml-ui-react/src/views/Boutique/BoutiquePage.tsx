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


  return (
    <div>
      <h2>Boutique (Utilisateur : {user.name})</h2>
      <div className="row">
      </div>
    </div>
  );
};

export default BoutiquePage;