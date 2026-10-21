import { formatAnswer } from './format-answer';

describe('formatAnswer', () => {
  it('escapes HTML coming from the model', () => {
    expect(formatAnswer('<img src=x onerror=alert(1)>')).toBe('<p>&lt;img src=x onerror=alert(1)&gt;</p>');
  });

  it('turns citations into source markers', () => {
    expect(formatAnswer('Code 7600 [1][2].')).toBe(
      '<p>Code 7600 <sup class="cite" data-source="1">1</sup><sup class="cite" data-source="2">2</sup>.</p>'
    );
  });

  it('renders bold text and bullet lists', () => {
    expect(formatAnswer('Le pilote doit :\n- afficher **7600** ;\n- surveiller la tour.')).toBe(
      '<p>Le pilote doit :</p><ul><li>afficher <strong>7600</strong> ;</li><li>surveiller la tour.</li></ul>'
    );
  });
});
