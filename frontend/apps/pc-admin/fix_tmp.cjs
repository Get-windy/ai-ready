const fs = require('fs');
const f = 'components/BillTableList/BillTableList.vue';
let t = fs.readFileSync(f, 'utf8');
const bad = "storageKey: { type: String as PropType<string>, default:  },";
const good = "storageKey: { type: String as PropType<string>, default: '' },";
if (t.includes(bad)) {
  t = t.replace(bad, good);
  fs.writeFileSync(f, t);
  console.log('fixed default to empty string');
} else {
  console.log('bad pattern not found, current line:');
  const m = t.match(/storageKey: \{[^\n]*/);
  console.log(m ? m[0] : 'not found');
}
