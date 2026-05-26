import ARTable from './ARTable.vue';
import ARTableColumn from './ARTableColumn.vue';
import type { App } from 'vue';

const components = [ARTable, ARTableColumn];

const install = (app: App): void => {
  components.forEach(component => {
    app.component(component.name || component.__name || 'Unknown', component);
  });
};

export { ARTable, ARTableColumn };
export default {
  install,
  ARTable,
  ARTableColumn
};