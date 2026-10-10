const SVG_NS = 'http://www.w3.org/2000/svg';

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

// Seules les références internes (fragment ou assets servis par l'app) évitent le beacon externe.
const LOCAL_REF = /^(#|\/boards\/|\/assets\/)/;
const EXTERNAL_URL = /url\s*\(\s*(?!['"]?\s*(#|\/boards\/|\/assets\/))/;

// C0/espaces ignorés par le parseur d'URL en tête de valeur : « \u0001javascript: » exécute.
const OBFUSCATING_CHARS = /[\s\p{Cc}]/gu;

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
  if (!root || root.namespaceURI !== SVG_NS || root.localName.toLowerCase() !== 'svg') {
    return '';
  }

  template.content.querySelectorAll('*').forEach((el) => {
    const tag = (el.localName || el.tagName).toLowerCase();
    // Allowlist de namespace : un <form>/<input> HTML clobberait `el.attributes` et échappait aux contrôles.
    if (el.namespaceURI !== SVG_NS || FORBIDDEN_ELEMENTS.has(tag)) {
      Element.prototype.remove.call(el);
      return;
    }
    for (const name of Element.prototype.getAttributeNames.call(el)) {
      const value = (Element.prototype.getAttribute.call(el, name) ?? '')
        .replace(OBFUSCATING_CHARS, '')
        .toLowerCase();
      const lowerName = name.toLowerCase();
      if (lowerName.startsWith('on') || DANGEROUS_SCHEME.test(value)) {
        Element.prototype.removeAttribute.call(el, name);
        continue;
      }
      if ((lowerName === 'href' || lowerName === 'xlink:href') && !LOCAL_REF.test(value)) {
        Element.prototype.removeAttribute.call(el, name);
        continue;
      }
      if (EXTERNAL_URL.test(value)) {
        Element.prototype.removeAttribute.call(el, name);
      }
    }
  });

  return template.innerHTML;
}
