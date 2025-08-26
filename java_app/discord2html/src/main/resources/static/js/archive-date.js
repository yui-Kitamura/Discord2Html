(function () {
  'use strict';

  // - d-YYYYMMDD のID値から`yyyy-mm-dd`形式文字列に変換する
  function extractFromId(id) {
    if (!id) return null;
    const s = id.trim();
    let m = /^d-(\d{4})(\d{2})(\d{2})$/.exec(s);
    if (m) {
        const yNum = Number(m[1]);
        const mNum = Number(m[2]);
        const dNum = Number(m[3]);
        if (!Number.isFinite(yNum) || !Number.isFinite(mNum) || !Number.isFinite(dNum)){ return null; }
        const dt = new Date(Date.UTC(yNum, mNum - 1, dNum));
        if (dt.getUTCFullYear() !== yNum || dt.getUTCMonth() + 1 !== mNum || dt.getUTCDate() !== dNum){
            return null;
        }
        const mm = String(mNum).padStart(2, '0');
        const dd = String(dNum).padStart(2, '0');
        return `${yNum}-${mm}-${dd}`;
    }
    return null;
  }

  function collectDateAnchors(scope) {
    const dateMap = new Map(); // key: YYYY-MM-DD, value: element

    const anchorsWithId = scope.querySelectorAll('a[id]');
    anchorsWithId.forEach(a => {
      // d-YYYYMMDD を持つaタグを収集する
      const key = extractFromId(a.id);
      if (key && !dateMap.has(key)) {
        dateMap.set(key, a);
      }
    });
    return dateMap;
  }

  function computeMinMax(dateKeys) {
    if (!dateKeys.length){ return { min: null, max: null, sorted:[] };}
    const sorted = dateKeys.slice().sort();
    return { min: sorted[0], max: sorted[sorted.length - 1], sorted: sorted };
  }

  function scrollToAnchor(el) {
    if (!el){ return; }
    try {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (_e) {
      el.scrollIntoView();
    }
  }

  function init(selectorRoot) {
    const root = selectorRoot || document;
    const input = root.querySelector('#archive-date-input');
    const btn = root.querySelector('#archive-date-go');

    const dateMap = collectDateAnchors(document);
    const keys = Array.from(dateMap.keys());
    
    const { min, max, sorted } = computeMinMax(keys);

    if (min && max) {
      input.min = min;
      input.max = max;
      if (!input.value){
        // default to latest
        input.value = max;
      } 
    }

    function findBestKey(target) {
      if (!sorted || sorted.length === 0) {
        return null;
      }
      if (dateMap.has(target)){ return target; }
      const idx = sorted.findIndex(k => (k >= target));
      if (idx === -1) {
        // all entries are less than target -> pick the latest
        return sorted[sorted.length - 1];
      }
      if (sorted[idx] === target) {
        return sorted[idx];
      }
      return idx > 0 ? sorted[idx - 1] : sorted[0];
    }

    btn.addEventListener('click', function () {
      const val = (input.value || '').trim();
      if (!val) return;
      const key = findBestKey(val);
      if (!key) return;
      scrollToAnchor(dateMap.get(key));
    });

    input.addEventListener('keydown', function (ev) {
      if (ev.key === 'Enter') {
        ev.preventDefault();
        btn.click();
      }
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    try { init(document); } catch (e) { /* noop */ }
  });
})();
