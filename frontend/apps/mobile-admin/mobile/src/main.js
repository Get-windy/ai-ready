import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { Button, Field, Switch, Radio, RadioGroup, Checkbox, CheckboxGroup, Slider, Progress, Loading, Toast, Dialog } from 'vant';
import 'vant/lib/index.css';

const app = createApp(App);

// Register Vant components
app.use(Button)
  .use(Field)
  .use(Switch)
  .use(Radio)
  .use(RadioGroup)
  .use(Checkbox)
  .use(CheckboxGroup)
  .use(Slider)
  .use(Progress)
  .use(Loading)
  .use(Toast)
  .use(Dialog);

app.use(router).mount('#app');