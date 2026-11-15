import type { GazetteIssue, GazetteWeather } from '../../models';

export function paragraphs(body: string | null | undefined): string[] {
  return (body ?? '')
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter((paragraph) => paragraph.length > 0);
}

export function editionLabel(issue: GazetteIssue): string {
  if (issue.finalEdition) {
    return 'Dernière édition';
  }
  const count = issue.frontPage?.length ?? 0;
  if (count >= 3) {
    return 'Édition spéciale — triple élimination';
  }
  if (count === 2) {
    return 'Édition spéciale — double élimination';
  }
  return 'Édition du soir';
}

export function weatherLine(weather: GazetteWeather): string {
  return `${weather.temperature} °C · ${weather.precipitation} mm · humidité ${weather.humidity} % · vent ${weather.wind} km/h ${weather.windDirection}`;
}

export function isUpdated(issue: GazetteIssue): boolean {
  return !!issue.publishedAt && !!issue.updatedAt && issue.updatedAt > issue.publishedAt;
}
