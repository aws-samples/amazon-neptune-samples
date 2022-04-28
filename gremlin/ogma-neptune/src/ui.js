/**
 * Creates autocomplete element with eternal library (awesomplete)
 *
 * @param  {String}   selector   DOM selector for input
 * @param  {Array}    data       Stations data
 * @param  {Number}   maxLength  Max items length in list
 * @param  {Function} onSelect   Select item callback
 */
export const createAutocomplete = (selector, data, maxLength, onSubmit) => {
  const input = document.querySelector(selector);
  const select = new Awesomplete(input, {
    list: data,
    minChars: 0,
    sort: function (a, b) {
      if (a.value < b.value) return -1;
      if (a.value > b.value) return 1;
      return 0;
    },
    maxItems: maxLength,
    autoFirst: true,
    item: function (text, input) {
      // render item with highlighted text
      let html, highlighted;
      if (input.trim() === '') html = text.label;
      // no changes
      else {
        // make sure we only replace in contents, not markup
        highlighted = text.value.replace(
          RegExp(Awesomplete.$.regExpEscape(input.trim()), 'gi'),
          '<mark>$&</mark>'
        );
        html = text.label.replace(text.value, highlighted);
      }
      // create DOM element, see Awesomplete documentation
      return Awesomplete.$.create('li', {
        innerHTML: html,
        'aria-selected': 'false'
      });
    }
  });

  input.addEventListener('focus', () => {
    select.evaluate();
    select.open();
  });
  input.addEventListener('awesomplete-selectcomplete', evt => {
    input.blur();
    onSubmit(evt);
  });
};
