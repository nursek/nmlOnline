import { sanitizeSvg } from './svg-sanitize';

const SVG_NS = 'http://www.w3.org/2000/svg';

describe('sanitizeSvg', () => {
  it('retire les gestionnaires d’événements et conserve la géométrie', () => {
    const out = sanitizeSvg(
      `<svg xmlns="${SVG_NS}" onload="alert(1)"><path id="path1" d="M0 0 L1 1"/></svg>`,
    );

    expect(out).not.toContain('onload');
    expect(out).toContain('path1');
    expect(out).toContain('M0 0 L1 1');
  });

  it('retire script, foreignObject et iframe (casse incluse)', () => {
    const out = sanitizeSvg(
      `<svg xmlns="${SVG_NS}"><SCRIPT>alert(1)</SCRIPT>` +
        `<foreignObject><iframe srcdoc="&lt;script&gt;alert(2)&lt;/script&gt;"></iframe></foreignObject>` +
        `<path id="path2" d="M0 0"/></svg>`,
    );

    expect(out.toLowerCase()).not.toContain('<script');
    expect(out.toLowerCase()).not.toContain('foreignobject');
    expect(out.toLowerCase()).not.toContain('<iframe');
    expect(out).toContain('path2');
  });

  it('retire les références javascript: et data:text/html', () => {
    const out = sanitizeSvg(
      `<svg xmlns="${SVG_NS}"><image href="javascript:alert(1)"/>` +
        `<image href="data:text/html,alert(2)"/>` +
        `<path id="path3" d="M0 0"/></svg>`,
    );

    expect(out.toLowerCase()).not.toContain('javascript:');
    expect(out.toLowerCase()).not.toContain('data:text/html');
    expect(out).toContain('path3');
  });

  it('retire les attributs javascript: encodés en entités', () => {
    const out = sanitizeSvg(
      `<svg xmlns="${SVG_NS}"><a href="&#106;avascript:alert(1)">x</a>` +
        `<path id="path4" d="M0 0"/></svg>`,
    );

    expect(out.toLowerCase()).not.toContain('javascript:');
    expect(out.toLowerCase()).not.toContain('<a ');
    expect(out).toContain('path4');
  });

  it('neutralise un mXSS CDATA/style', () => {
    const payload =
      `<svg xmlns="${SVG_NS}"><style><![CDATA[</style>` +
      `<a href="&#106;avascript:alert(1)">x</a>]]></style><path id="path5" d="M0 0"/></svg>`;
    const out = sanitizeSvg(payload);

    expect(out.toLowerCase()).not.toContain('<style');
    expect(out.toLowerCase()).not.toContain('javascript:');
    expect(out).toContain('path5');
  });

  it('neutralise un mXSS CDATA/xmp', () => {
    const payload =
      `<svg xmlns="${SVG_NS}"><xmp><![CDATA[</xmp>` +
      `<a href="&#106;avascript:alert(1)">x</a><img src=x onerror="alert(2)">]]></xmp>` +
      `<path id="path6" d="M0 0"/></svg>`;
    const out = sanitizeSvg(payload);

    expect(out.toLowerCase()).not.toContain('<xmp');
    expect(out.toLowerCase()).not.toContain('javascript:');
    expect(out.toLowerCase()).not.toContain('onerror');
    expect(out).toContain('path6');
  });

  it('rejette un contenu sans racine <svg>', () => {
    expect(sanitizeSvg('<div>hello</div>')).toBe('');
    expect(sanitizeSvg('<div><style><![CDATA[</style><a href="x">y</a>]]></style></div>')).toBe('');
  });

  it('conserve un SVG inoffensif', () => {
    const out = sanitizeSvg(
      `<svg xmlns="${SVG_NS}" viewBox="0 0 10 10"><g fill="#fff">` +
        `<polygon id="path1" points="0,0 1,1 2,2"/></g></svg>`,
    );

    expect(out).toContain('viewBox');
    expect(out).toContain('polygon');
    expect(out).toContain('path1');
  });
});
