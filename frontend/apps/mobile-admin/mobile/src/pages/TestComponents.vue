<template>
  <div class="test-components">
    <h1>UI Components Validation Test</h1>
    
    <!-- Button Component -->
    <div class="component-section">
      <h2>Button</h2>
      <van-button type="primary" @click="handleButtonClick">Primary Button</van-button>
      <van-button type="success" @click="handleButtonClick">Success Button</van-button>
      <van-button type="danger" @click="handleButtonClick">Danger Button</van-button>
      <van-button type="warning" @click="handleButtonClick">Warning Button</van-button>
      <van-button type="default" @click="handleButtonClick">Default Button</van-button>
    </div>

    <!-- Input Component -->
    <div class="component-section">
      <h2>Input</h2>
      <van-field v-model="inputValue" label="Input" placeholder="Enter text" />
      <van-field v-model="textareaValue" label="Textarea" type="textarea" placeholder="Enter multiline text" rows="3" />
    </div>

    <!-- Switch Component -->
    <div class="component-section">
      <h2>Switch</h2>
      <van-switch v-model="switchValue" />
    </div>

    <!-- Radio Group -->
    <div class="component-section">
      <h2>Radio Group</h2>
      <van-radio-group v-model="radioValue">
        <van-radio name="1">Option 1</van-radio>
        <van-radio name="2">Option 2</van-radio>
        <van-radio name="3">Option 3</van-radio>
      </van-radio-group>
    </div>

    <!-- Checkbox Group -->
    <div class="component-section">
      <h2>Checkbox Group</h2>
      <van-checkbox-group v-model="checkboxValues">
        <van-checkbox name="a">Option A</van-checkbox>
        <van-checkbox name="b">Option B</van-checkbox>
        <van-checkbox name="c">Option C</van-checkbox>
      </van-checkbox-group>
    </div>

    <!-- Slider -->
    <div class="component-section">
      <h2>Slider</h2>
      <van-slider v-model="sliderValue" />
    </div>

    <!-- Progress -->
    <div class="component-section">
      <h2>Progress</h2>
      <van-progress :percentage="progressValue" />
    </div>

    <!-- Loading -->
    <div class="component-section">
      <h2>Loading</h2>
      <van-loading size="24px">Loading...</van-loading>
    </div>

    <!-- Toast -->
    <div class="component-section">
      <h2>Toast</h2>
      <van-button @click="showToast">Show Toast</van-button>
    </div>

    <!-- Dialog -->
    <div class="component-section">
      <h2>Dialog</h2>
      <van-button @click="showDialog">Show Dialog</van-button>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import { showToast, showDialog } from 'vant';

export default {
  name: 'TestComponents',
  setup() {
    const inputValue = ref('');
    const textareaValue = ref('');
    const switchValue = ref(false);
    const radioValue = ref('1');
    const checkboxValues = ref(['a']);
    const sliderValue = ref(50);
    const progressValue = ref(75);

    const handleButtonClick = () => {
      console.log('Button clicked');
    };

    const showToast = () => {
      showToast('This is a toast message');
    };

    const showDialog = () => {
      showDialog({
        title: 'Dialog Title',
        message: 'This is a dialog message'
      });
    };

    return {
      inputValue,
      textareaValue,
      switchValue,
      radioValue,
      checkboxValues,
      sliderValue,
      progressValue,
      handleButtonClick,
      showToast,
      showDialog
    };
  }
};
</script>

<style scoped>
.test-components {
  padding: 20px;
}

.component-section {
  margin-bottom: 30px;
}

.component-section h2 {
  margin-bottom: 15px;
  font-size: 18px;
  color: #333;
}
</style>