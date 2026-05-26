import ARDialog from './ARDialog.vue';
import type { App } from 'vue';

ARDialog.install = (app: App): void => {
  app.component(ARDialog.name || 'ARDialog', ARDialog);
};

export { ARDialog };
export default ARDialog;