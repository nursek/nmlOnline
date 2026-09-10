const FORBIDDEN_ELEMENTS = new Set([
  'script',
  'foreignobject',
  'iframe',
  'object',
  'embed',
  'audio',
  'video',
  'a',
  'animate',
  'animatetransform',
  'animatemotion',
  'set',
  'style',
  'link',
  'meta',
  'base',
  'xmp',
  'noembed',
  'noframes',
  'plaintext',
  'noscript',
  'template',
]);

const DANGEROUS_SCHEME = /^(javascript|vbscript|data):/;

/**
 * Neutralise les éléments et attributs actifs d'un SVG avant injection en innerHTML.
 * Parsing via <template> : on inspecte le markup que produira innerHTML (un parseur XML
 * serait contournable via CDATA/entités). On répète jusqu'au point fixe car la
 * sérialisation peut ré-exposer un contenu caché (CDATA/rawtext), sinon on rejette.
 */
export function sanitizeSvg(text: string): string {
  let current = text;
  for (let i = 0; i < 4; i++) {
    const next = sanitizeOnce(current);
    if (next === current) {
      return next;
    }
    current = next;
  }
  return '';
}

function sanitizeOnce(text: string): string {
  const template = document.createElement('template');
  template.innerHTML = text;

  const root = template.content.firstElementChild;
  if (!root || root.localName.toLowerCase() !== 'svg') {
    return '';
  }

  template.content.querySelectorAll('*').forEach((el) => {
    const tag = (el.localName || el.tagName).toLowerCase();
    if (FORBIDDEN_ELEMENTS.has(tag)) {
      el.remove();
      return;
    }
    for (const attr of Array.from(el.attributes)) {
      const name = attr.name.toLowerCase();
      const value = attr.value.replace(/\s/g, '').toLowerCase();
      if (name.startsWith('on') || DANGEROUS_SCHEME.test(value)) {
        el.removeAttribute(attr.name);
      }
    }
  });

  return template.innerHTML;
}
