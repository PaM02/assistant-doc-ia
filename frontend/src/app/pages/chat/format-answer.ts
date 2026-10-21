/**
 * Met en forme la réponse du modèle pour l'affichage :
 * - échappe le HTML (le texte vient d'une IA : on ne lui fait pas confiance) ;
 * - **gras** → <strong> ;
 * - lignes commençant par « - » → liste à puces ;
 * - citations [1], [2]… → pastilles cliquables liées aux sources.
 */
export function formatAnswer(text: string): string {
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

  const withInline = escaped
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\[(\d+)]/g, '<sup class="cite" data-source="$1">$1</sup>');

  const html: string[] = [];
  let inList = false;
  for (const line of withInline.split('\n')) {
    const item = line.match(/^\s*[-*•]\s+(.*)$/);
    if (item) {
      if (!inList) {
        html.push('<ul>');
        inList = true;
      }
      html.push(`<li>${item[1]}</li>`);
      continue;
    }
    if (inList) {
      html.push('</ul>');
      inList = false;
    }
    if (line.trim()) {
      html.push(`<p>${line}</p>`);
    }
  }
  if (inList) {
    html.push('</ul>');
  }
  return html.join('');
}
