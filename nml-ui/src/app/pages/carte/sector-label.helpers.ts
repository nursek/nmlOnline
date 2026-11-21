export function computeVisualCenter(el: SVGGeometryElement): { x: number; y: number } {
  const bbox = el.getBBox();
  const fallback = { x: bbox.x + bbox.width / 2, y: bbox.y + bbox.height / 2 };

  const boundary: { x: number; y: number }[] = [];
  const pathEl = el.tagName.toLowerCase() === 'path' ? (el as SVGPathElement) : null;
  try {
    if (pathEl) {
      const total = pathEl.getTotalLength();
      if (total <= 0) return fallback;
      const samples = Math.min(128, Math.max(32, Math.ceil(total / 6)));
      for (let i = 0; i < samples; i++) {
        const point = pathEl.getPointAtLength((i * total) / (samples - 1));
        boundary.push({ x: point.x, y: point.y });
      }
    } else {
      const points = el.getAttribute('points') ?? '';
      for (const pair of points
        .trim()
        .split(/[\s,]+/)
        .reduce<string[][]>((acc, value, index) => {
          if (index % 2 === 0) acc.push([value]);
          else acc[acc.length - 1].push(value);
          return acc;
        }, [])) {
        const x = Number(pair[0]);
        const y = Number(pair[1]);
        if (Number.isFinite(x) && Number.isFinite(y)) boundary.push({ x, y });
      }
      if (boundary.length === 0) return fallback;
    }
  } catch {
    return fallback;
  }

  const canFillTest = typeof el.isPointInFill === 'function';
  const step = 4;
  const margin = 12;
  const candidates: { x: number; y: number }[] = [];
  for (let gy = bbox.y; gy <= bbox.y + bbox.height; gy += step) {
    for (let gx = bbox.x; gx <= bbox.x + bbox.width; gx += step) {
      if (canFillTest) {
        try {
          if (!el.isPointInFill({ x: gx, y: gy })) continue;
        } catch {
          continue;
        }
      }
      candidates.push({ x: gx, y: gy });
    }
  }
  if (candidates.length === 0) return fallback;

  let best = candidates[0];
  let bestDist = -Infinity;
  let bestAboveMargin = false;
  for (const candidate of candidates) {
    let minDist = Infinity;
    for (const point of boundary) {
      const dx = candidate.x - point.x;
      const dy = candidate.y - point.y;
      const distance = dx * dx + dy * dy;
      if (distance < minDist) minDist = distance;
    }
    const above = minDist >= margin * margin;
    if (bestAboveMargin) {
      if (!above || minDist <= bestDist) continue;
    } else if (above) {
      bestAboveMargin = true;
    } else if (minDist <= bestDist) {
      continue;
    }
    bestDist = minDist;
    best = candidate;
  }
  return best;
}
