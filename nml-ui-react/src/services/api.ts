export async function fetchTroops(playerId: string) {
  const res = await fetch(`/api/players/${playerId}/troops`);
  if (!res.ok) throw new Error('Erreur lors du chargement des troupes');
  return res.json();
}

export async function fetchUnits(playerId: string) {
  const res = await fetch(`/api/players/${playerId}/units`);
  if (!res.ok) throw new Error('Erreur lors du chargement des unités');
  return res.json();
}

export async function fetchEquipment() {
  const res = await fetch(`/api/equipment`);
  if (!res.ok) throw new Error('Erreur lors du chargement des équipements');
  return res.json();
}

export async function buyEquipment(playerId: string, itemId: number) {
  const res = await fetch(`/api/players/${playerId}/purchase`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ itemId })
  });
  if (!res.ok) throw new Error('Erreur lors de l\'achat');
  return res.json();
}