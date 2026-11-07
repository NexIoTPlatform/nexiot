<template>
  <div class="login-left" aria-hidden="true">
    <div class="signal-scene">
      <span class="signal-scene__frame signal-scene__frame--outer"></span>
      <span class="signal-scene__frame signal-scene__frame--inner"></span>

      <span
        v-for="lane in lanes"
        :key="lane.top"
        class="signal-scene__lane"
        :style="{
          'top': lane.top,
          'left': lane.left,
          'width': lane.width,
          '--signal-delay': lane.delay,
          '--signal-duration': lane.duration,
        }"
      >
        <i class="signal-scene__beam"></i>
      </span>

      <span class="signal-scene__cursor"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoginLeft' })

const lanes = [
  { top: '22%', left: '5%', width: '78%', delay: '-3.1s', duration: '8.2s' },
  { top: '39%', left: '0%', width: '88%', delay: '-5.7s', duration: '9.4s' },
  { top: '61%', left: '8%', width: '80%', delay: '-1.9s', duration: '7.6s' },
  { top: '78%', left: '3%', width: '84%', delay: '-6.8s', duration: '10.2s' }
] as const
</script>

<style lang="scss">
.login-left {
  --signal-bg: #f2f3f3;
  --signal-line: #d9dcdb;
  --signal-line-strong: #c8cdca;
  --signal-point: #7b827e;
  --signal-surface: #f8f9f8;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 640px;
  overflow: hidden;
  background: var(--signal-bg);
  border-right: 1px solid var(--signal-line);
}

.signal-scene {
  position: relative;
  width: min(78%, 760px);
  height: min(55vh, 430px);
  min-height: 320px;

  &__frame {
    position: absolute;
    display: block;
    pointer-events: none;
    border: 1px solid var(--signal-line);
    border-radius: 26px;
    transform-origin: 50% 50%;
  }

  &__frame--outer {
    top: 15%;
    left: 18%;
    width: 64%;
    height: 70%;
    transform: rotate(-8deg);
    animation: signal-frame-outer 16s ease-in-out infinite;
  }

  &__frame--inner {
    top: 26%;
    left: 27%;
    width: 46%;
    height: 48%;
    border-color: var(--signal-line-strong);
    border-radius: 18px;
    transform: rotate(6deg);
    animation: signal-frame-inner 12s ease-in-out -3s infinite;
  }

  &__lane {
    position: absolute;
    display: block;
    height: 1px;
    background: var(--signal-line);
  }

  &__beam {
    position: absolute;
    top: -1px;
    left: 0;
    display: block;
    width: 42px;
    height: 2px;
    background: var(--signal-point);
    border-radius: 2px;
    opacity: 0.72;
    animation: signal-beam var(--signal-duration) cubic-bezier(0.4, 0, 0.2, 1) var(--signal-delay) infinite;
  }

  &__cursor {
    position: absolute;
    top: calc(50% - 5px);
    left: calc(50% - 5px);
    z-index: 3;
    display: block;
    width: 8px;
    height: 8px;
    background: var(--signal-point);
    border-radius: 2px;
    box-shadow: 0 0 0 4px var(--signal-bg), 0 0 0 5px var(--signal-line-strong);
    animation: signal-cursor 6.5s ease-in-out -2.1s infinite;
  }
}

body[arco-theme='dark'] .login-left {
  --signal-bg: #1d1f1f;
  --signal-line: #3a3e3d;
  --signal-line-strong: #505654;
  --signal-point: #a4aaa7;
  --signal-surface: #262929;
}

@keyframes signal-beam {
  0%,
  8% {
    left: 0;
    opacity: 0;
  }

  20% {
    opacity: 0.72;
  }

  78% {
    opacity: 0.72;
  }

  92%,
  100% {
    left: calc(100% - 42px);
    opacity: 0;
  }
}

@keyframes signal-frame-outer {
  0%,
  100% {
    transform: rotate(-8deg) translate3d(0, 0, 0);
  }

  50% {
    transform: rotate(-5deg) translate3d(7px, -4px, 0);
  }
}

@keyframes signal-frame-inner {
  0%,
  100% {
    transform: rotate(6deg) translate3d(0, 0, 0);
  }

  50% {
    transform: rotate(3deg) translate3d(-6px, 5px, 0);
  }
}

@keyframes signal-cursor {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  24% {
    transform: translate3d(64px, 0, 0);
  }

  48% {
    transform: translate3d(64px, 46px, 0);
  }

  72% {
    transform: translate3d(-42px, 46px, 0);
  }
}

@media (width <= 1180px) {
  .signal-scene {
    width: min(80%, 620px);
    height: 350px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .signal-scene__beam,
  .signal-scene__frame--outer,
  .signal-scene__frame--inner,
  .signal-scene__cursor {
    animation: none;
  }
}
</style>
