const DIACRITIQUES = /[\u0300-\u036f]/g;

export interface Occurrence {
  debut: number;
  fin: number;
}

export function normaliserTexte(texte: string): string {
  return texte.normalize('NFD').replace(DIACRITIQUES, '').toLowerCase();
}

interface TexteNormalise {
  texte: string;
  debuts: number[];
  fins: number[];
}

// Conserve la position d'origine : les dégâts de `Range` ont besoin d'index dans le texte brut.
function normaliserAvecPositions(texte: string): TexteNormalise {
  let normalise = '';
  const debuts: number[] = [];
  const fins: number[] = [];
  let position = 0;
  for (const caractere of texte) {
    const replie = normaliserTexte(caractere);
    for (let index = 0; index < replie.length; index++) {
      normalise += replie[index];
      debuts.push(position);
      fins.push(position + caractere.length);
    }
    position += caractere.length;
  }
  return { texte: normalise, debuts, fins };
}

export function occurrences(texte: string, terme: string): Occurrence[] {
  const recherche = normaliserTexte(terme.trim());
  if (!recherche) return [];
  const normalise = normaliserAvecPositions(texte);
  const resultats: Occurrence[] = [];
  let depuis = 0;
  for (;;) {
    const index = normalise.texte.indexOf(recherche, depuis);
    if (index < 0) break;
    resultats.push({
      debut: normalise.debuts[index],
      fin: normalise.fins[index + recherche.length - 1],
    });
    depuis = index + recherche.length;
  }
  return resultats;
}
