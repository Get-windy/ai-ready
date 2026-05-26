import ARInput from './ARInput.vue';
import type { App } from 'vue';

ARInput.install = (app: App): void => {
  app.component(ARInput.name || 'ARInput', ARInput);
};

export { ARInput };
export default ARInput;