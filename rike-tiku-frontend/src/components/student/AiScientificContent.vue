<script setup lang="ts">
import { computed } from 'vue'
import MathFragment from '../question/MathFragment.vue'
import { parseAiScientificContent, type AiInline } from './aiScientificContent'

const props = defineProps<{ content: string }>()
const blocks = computed(() => parseAiScientificContent(props.content))
</script>

<template>
  <div class="ai-scientific-content">
    <template v-for="(block, blockIndex) in blocks" :key="blockIndex">
      <component :is="block.type === 'ordered-list' ? 'ol' : 'ul'" v-if="block.type.endsWith('list')">
        <li v-for="(item, itemIndex) in block.items" :key="itemIndex">
          <template v-for="(token, tokenIndex) in item" :key="tokenIndex">
            <strong v-if="token.type === 'strong'">{{ token.value }}</strong>
            <code v-else-if="token.type === 'code'">{{ token.value }}</code>
            <MathFragment v-else-if="token.type === 'math'" :expression="token.value" :display="token.display" />
            <span v-else>{{ token.value }}</span>
          </template>
        </li>
      </component>
      <p v-else>
        <template v-for="(token, tokenIndex) in (block.content as AiInline[])" :key="tokenIndex">
          <strong v-if="token.type === 'strong'">{{ token.value }}</strong>
          <code v-else-if="token.type === 'code'">{{ token.value }}</code>
          <MathFragment v-else-if="token.type === 'math'" :expression="token.value" :display="token.display" />
          <span v-else>{{ token.value }}</span>
        </template>
      </p>
    </template>
  </div>
</template>

<style scoped>
.ai-scientific-content{min-width:0;overflow-wrap:anywhere;line-height:1.75}.ai-scientific-content p{margin:0 0 .6em;white-space:pre-wrap}.ai-scientific-content p:last-child{margin-bottom:0}.ai-scientific-content ul,.ai-scientific-content ol{margin:.25em 0 .65em;padding-left:1.5em}.ai-scientific-content li+li{margin-top:.25em}.ai-scientific-content code{padding:.1em .35em;border-radius:5px;background:var(--el-fill-color);font-family:ui-monospace,SFMono-Regular,Consolas,monospace}.ai-scientific-content :deep(.math-fragment--display){display:block;max-width:100%;overflow-x:auto;overflow-y:hidden;padding:.25em 0}.ai-scientific-content :deep(.katex-display){margin:.35em 0}
</style>
