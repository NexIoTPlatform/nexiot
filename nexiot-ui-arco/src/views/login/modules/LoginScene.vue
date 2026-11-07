<!--
Eye tracking and blinking interaction adapted from https://github.com/arsh342/careercompass
src/components/ui/animated-characters.tsx

MIT License

Copyright (c) 2025 arsh342

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
-->
<template>
  <section class="login-scene">
    <div class="login-scene__brand">
      <img v-if="iconUrl" :src="iconUrl" alt="" class="login-scene__logo">
      <span v-if="platformName">{{ platformName }}</span>
    </div>

    <div class="login-scene__stage" aria-hidden="true">
      <svg
        ref="charactersRef"
        class="login-scene__characters"
        :class="{ 'login-scene__characters--typing': isTyping && !privateMode }"
        viewBox="0 24 480 400"
        fill="none"
      >
        <g class="login-scene__body" :transform="bodyTransform('hub')">
          <path d="M201 81V106" stroke="#6c72cd" stroke-width="8" stroke-linecap="round" />
          <circle cx="201" cy="73" r="8" fill="#6c72cd" />
          <circle cx="201" cy="73" r="2.5" fill="#d8dcff" />
          <g class="login-scene__signal-lines" stroke="#9a9ed5" stroke-width="2.5" stroke-linecap="round">
            <path d="M186 67C184 71 184 75 186 79" />
            <path d="M216 67C218 71 218 75 216 79" />
          </g>
          <rect x="111" y="104" width="180" height="210" rx="36" fill="#6c72cd" />
          <g :transform="faceTransform('hub')" class="login-scene__face">
            <g v-if="!eyesClosed('hub')">
              <ellipse cx="173" cy="161" rx="12" ry="14" fill="#f6f7ff" />
              <ellipse cx="229" cy="161" rx="12" ry="14" fill="#f6f7ff" />
              <g :transform="pupilTransform('hub', 173, 161)">
                <circle cx="173" cy="162" r="6" fill="#303747" />
                <circle cx="171" cy="159" r="1.7" fill="#fff" />
              </g>
              <g :transform="pupilTransform('hub', 229, 161)">
                <circle cx="229" cy="162" r="6" fill="#303747" />
                <circle cx="227" cy="159" r="1.7" fill="#fff" />
              </g>
            </g>
            <g v-else stroke="#e5e9ff" stroke-width="4" stroke-linecap="round">
              <path d="M164 162Q173 168 182 162" />
              <path d="M220 162Q229 168 238 162" />
            </g>
            <path d="M192 196Q201 202 210 196" stroke="#414b95" stroke-width="3.5" stroke-linecap="round" />
          </g>
          <g stroke="#bfc6f1" stroke-width="2.5" stroke-linecap="round">
            <path d="M224 257L243 245M224 257L243 269" />
            <circle cx="221" cy="257" r="4" fill="#bfc6f1" stroke="none" />
            <circle cx="246" cy="243" r="4" fill="#d1e3db" stroke="none" />
            <circle cx="246" cy="271" r="4" fill="#bfc6f1" stroke="none" />
          </g>
          <rect x="144" y="261" width="28" height="5" rx="2.5" fill="#aab4e5" />
          <rect x="144" y="272" width="19" height="5" rx="2.5" fill="#aab4e5" />
        </g>

        <g class="login-scene__body" :transform="bodyTransform('camera')">
          <path d="M436 269L458 257C463 254 466 256 466 262V320C466 326 463 328 458 325L436 312V269Z" fill="#8eafa0" />
          <rect x="252" y="242" width="190" height="135" rx="30" fill="#aacdbb" />
          <rect x="274" y="256" width="19" height="5" rx="2.5" fill="#88ad9b" />
          <g :transform="faceTransform('camera')" class="login-scene__face">
            <circle cx="339" cy="295" r="36" fill="#86aa99" />
            <circle cx="339" cy="295" r="27" fill="#f4faf5" />
            <g v-if="!eyesClosed('camera')" :transform="pupilTransform('camera', 339, 295)">
              <circle cx="339" cy="296" r="13" fill="#34433f" />
              <circle cx="335" cy="291" r="3.5" fill="#f4faf5" />
              <circle cx="344" cy="302" r="1.8" fill="#f4faf5" />
            </g>
            <path v-else d="M325 296Q339 306 353 296" stroke="#34433f" stroke-width="4" stroke-linecap="round" />
            <path d="M330 347Q339 352 348 346" stroke="#668b79" stroke-width="3.5" stroke-linecap="round" />
          </g>
          <circle cx="413" cy="350" r="3" fill="#6f9781" />
        </g>

        <g class="login-scene__body" :transform="bodyTransform('sensor')">
          <circle cx="93" cy="325" r="60" fill="#f0cc75" />
          <g :transform="faceTransform('sensor')" class="login-scene__face">
            <g v-if="!eyesClosed('sensor')">
              <g :transform="pupilTransform('sensor', 73, 317)">
                <ellipse cx="73" cy="317" rx="5.5" ry="7" fill="#554b37" />
              </g>
              <g :transform="pupilTransform('sensor', 111, 317)">
                <ellipse cx="111" cy="317" rx="5.5" ry="7" fill="#554b37" />
              </g>
            </g>
            <g v-else stroke="#554b37" stroke-width="3.5" stroke-linecap="round">
              <path d="M68 317Q73 321 78 317" />
              <path d="M106 317Q111 321 116 317" />
            </g>
            <path d="M85 343Q93 349 101 343" stroke="#aa8844" stroke-width="3.5" stroke-linecap="round" />
          </g>
        </g>
      </svg>
    </div>

    <div class="login-scene__caption">
      <div class="login-scene__symbols" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M3 9C8 4.5 16 4.5 21 9M6.5 12.5C9.5 9.8 14.5 9.8 17.5 12.5M10 16C11.2 14.9 12.8 14.9 14 16" />
          <circle cx="12" cy="19" r="0.8" fill="currentColor" stroke="none" />
        </svg>
        <svg viewBox="0 0 24 24" fill="none">
          <rect x="3" y="6" width="12" height="12" rx="3" />
          <path d="M15 10L21 7.5V16.5L15 14" />
        </svg>
      </div>
      <span>物联 · 设备</span>
    </div>
  </section>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoginScene' })

const props = withDefaults(defineProps<{
  iconUrl?: string
  platformName?: string
  privateMode?: boolean
  isTyping?: boolean
}>(), {
  iconUrl: '',
  platformName: '',
  privateMode: false,
  isTyping: false
})

const characters = {
  hub: { x: 201, y: 161, originY: 314, pupilDistance: 5 },
  sensor: { x: 93, y: 317, originY: 385, pupilDistance: 3 },
  camera: { x: 339, y: 295, originY: 377, pupilDistance: 6 }
} as const

type Character = keyof typeof characters

const charactersRef = useTemplateRef<SVGSVGElement>('charactersRef')
const pointer = reactive({ x: 0, y: 0, active: false })
const reducedMotion = ref(false)
const supportsPointer = ref(false)
const isLookingAtEachOther = ref(false)
const blinking = reactive<Record<Character, boolean>>({ hub: false, sensor: false, camera: false })
const blinkTimers = new Set<number>()
let mounted = false
let motionQuery: MediaQueryList | undefined
let pointerQuery: MediaQueryList | undefined
let typingTimer: number | undefined

const canTrack = computed(() => supportsPointer.value && !reducedMotion.value)

function positionFor(character: Character) {
  if (!pointer.active || !canTrack.value) return { x: 0, y: 0, lean: 0 }
  const center = characters[character]
  const deltaX = pointer.x - center.x
  const deltaY = pointer.y - center.y
  return {
    x: Math.max(-4, Math.min(4, deltaX / 55)),
    y: Math.max(-3, Math.min(3, deltaY / 70)),
    lean: Math.max(-2.5, Math.min(2.5, deltaX / 150))
  }
}

function bodyTransform(character: Character) {
  const center = characters[character]
  let lean = positionFor(character).lean
  let rise = 0
  if (props.privateMode) {
    lean = -1.5
  } else if (props.isTyping && !reducedMotion.value) {
    lean += character === 'hub' ? 2 : -1
    rise = character === 'hub' ? -4 : 0
  }
  if (reducedMotion.value) lean = 0
  return `translate(0 ${rise}) rotate(${lean} ${center.x} ${center.originY})`
}

function faceTransform(character: Character) {
  if (props.privateMode) return 'translate(-5 -1)'
  if (isLookingAtEachOther.value) {
    if (character === 'hub') return 'translate(3 2)'
    if (character === 'sensor') return 'translate(4 -2)'
    return 'translate(-3 -2)'
  }
  const position = positionFor(character)
  return `translate(${position.x} ${position.y})`
}

function pupilTransform(character: Character, x: number, y: number) {
  if (isLookingAtEachOther.value) {
    return character === 'camera' ? 'translate(-4 -3)' : 'translate(3 -1)'
  }
  if (!pointer.active || !canTrack.value) return 'translate(0 0)'
  const deltaX = pointer.x - x
  const deltaY = pointer.y - y
  const distance = Math.min(Math.hypot(deltaX, deltaY), characters[character].pupilDistance)
  const angle = Math.atan2(deltaY, deltaX)
  return `translate(${Math.cos(angle) * distance} ${Math.sin(angle) * distance})`
}

function eyesClosed(character: Character) {
  return props.privateMode || blinking[character]
}

function resetPointer() {
  pointer.active = false
}

function handlePointerMove(event: PointerEvent) {
  if (!canTrack.value || event.pointerType !== 'mouse') return
  const bounds = charactersRef.value?.getBoundingClientRect()
  if (!bounds?.width) return
  pointer.x = ((event.clientX - bounds.left) / bounds.width) * 480
  pointer.y = ((event.clientY - bounds.top) / bounds.height) * 400 + 24
  pointer.active = true
}

function clearBlinkTimers() {
  blinkTimers.forEach((timer) => window.clearTimeout(timer))
  blinkTimers.clear()
  blinking.hub = false
  blinking.sensor = false
  blinking.camera = false
}

function scheduleBlink(character: Character) {
  if (!mounted || reducedMotion.value) return
  const timer = window.setTimeout(() => {
    blinkTimers.delete(timer)
    blinking[character] = true
    const endTimer = window.setTimeout(() => {
      blinkTimers.delete(endTimer)
      blinking[character] = false
      scheduleBlink(character)
    }, 140)
    blinkTimers.add(endTimer)
  }, Math.random() * 4000 + 3200)
  blinkTimers.add(timer)
}

function updateTypingState() {
  window.clearTimeout(typingTimer)
  isLookingAtEachOther.value = mounted && props.isTyping && !reducedMotion.value && !props.privateMode
  if (!isLookingAtEachOther.value) return
  typingTimer = window.setTimeout(() => {
    isLookingAtEachOther.value = false
  }, 850)
}

function updateMotionPreference() {
  reducedMotion.value = Boolean(motionQuery?.matches)
  supportsPointer.value = Boolean(pointerQuery?.matches)
  resetPointer()
  clearBlinkTimers()
  scheduleBlink('hub')
  scheduleBlink('sensor')
  scheduleBlink('camera')
  updateTypingState()
}

onMounted(() => {
  mounted = true
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  pointerQuery = window.matchMedia('(hover: hover) and (pointer: fine)')
  updateMotionPreference()
  motionQuery.addEventListener('change', updateMotionPreference)
  pointerQuery.addEventListener('change', updateMotionPreference)
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  window.addEventListener('resize', resetPointer)
  window.addEventListener('blur', resetPointer)
  document.documentElement.addEventListener('pointerleave', resetPointer)
})

watch(() => [props.isTyping, props.privateMode], updateTypingState)

onBeforeUnmount(() => {
  mounted = false
  clearBlinkTimers()
  window.clearTimeout(typingTimer)
  motionQuery?.removeEventListener('change', updateMotionPreference)
  pointerQuery?.removeEventListener('change', updateMotionPreference)
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('resize', resetPointer)
  window.removeEventListener('blur', resetPointer)
  document.documentElement.removeEventListener('pointerleave', resetPointer)
})
</script>

<style lang="scss" scoped>
.login-scene {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  height: 100dvh;
  min-height: 600px;
  padding: 48px;
  overflow: hidden;
  color: #343543;
  background: linear-gradient(145deg, #edecf1 0%, #e2e4eb 100%);

  &__brand {
    display: flex;
    flex: 0 0 auto;
    gap: 9px;
    align-items: center;
    min-height: 32px;
    font-size: 17px;
    font-weight: 600;
    letter-spacing: 0.5px;
  }

  &__logo {
    box-sizing: border-box;
    display: block;
    width: 32px;
    height: 32px;
    padding: 3px;
    object-fit: contain;
    background: rgb(255 255 255 / 48%);
    border-radius: 9px;
  }

  &__stage {
    display: flex;
    flex: 1 1 auto;
    align-items: center;
    justify-content: center;
    min-width: 0;
    min-height: 0;
  }

  &__characters {
    display: block;
    width: min(100%, 480px);
    height: auto;
    max-height: 430px;
    aspect-ratio: 6 / 5;
    overflow: visible;
  }

  &__body {
    transition: transform 500ms ease-out;
  }

  &__face {
    transition: transform 180ms ease-out;
  }

  &__signal-lines {
    opacity: 0.65;
    transition: opacity 300ms ease;
  }

  &__characters--typing &__signal-lines {
    opacity: 1;
  }

  &__caption {
    display: flex;
    flex: 0 0 auto;
    gap: 16px;
    align-items: center;
    min-height: 20px;
    font-size: 12px;
    font-weight: 400;
    color: #858493;
    letter-spacing: 1px;
  }

  &__symbols {
    display: flex;
    gap: 9px;
    align-items: center;

    svg {
      display: block;
      width: 17px;
      height: 17px;
      stroke: currentcolor;
      stroke-width: 1.5;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-scene {
    &__body,
    &__face,
    &__signal-lines {
      transition: none;
    }
  }
}
</style>
