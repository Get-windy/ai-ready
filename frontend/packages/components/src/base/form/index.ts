import ARForm from './ARForm.vue';
import ARFormItem from './ARFormItem.vue';
import ARSelect from './ARSelect.vue';
import type { App } from 'vue';

const components = [ARForm, ARFormItem, ARSelect];

const install = (app: App): void => {
  components.forEach(component => {
    app.component(component.name || component.__name || 'Unknown', component);
  });
};

export { ARForm, ARFormItem, ARSelect };
export default {
  install,
  ARForm,
  ARFormItem,
  ARSelect
};