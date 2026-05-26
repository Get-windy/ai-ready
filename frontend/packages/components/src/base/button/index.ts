import ARButton from './ARButton.vue';
import type { App } from 'vue';

ARButton.install = (app: App): void => {
  app.component(ARButton.name || 'ARButton', ARButton);
};

export { ARButton };
export default ARButton;