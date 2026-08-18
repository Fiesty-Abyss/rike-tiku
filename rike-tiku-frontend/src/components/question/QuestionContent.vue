<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import http from '../../api/http'
import ScientificText from './ScientificText.vue'

export interface QuestionAttachment { id:number; position:string; type:string; fileName:string; objectMarker?:string; status?:string; renderStatus?:string; contentUrl?:string }
const props=withDefaults(defineProps<{content:string; attachments?:QuestionAttachment[]; position:string}>(),{attachments:()=>[]})
const marker=/〔(?:图片|公式)对象\s+([IF]\d{3})〕/g
const pieces=computed(()=>{const result:Array<{text?:string;attachment?:QuestionAttachment;marker?:string}>=[];let last=0;for(const match of props.content.matchAll(marker)){if(match.index!>last)result.push({text:props.content.slice(last,match.index)});const value=match[0];result.push({marker:value,attachment:props.attachments.find(item=>item.position===props.position&&item.objectMarker===match[1])});last=match.index!+value.length}if(last<props.content.length)result.push({text:props.content.slice(last)});return result})
const sources=ref<Record<number,string>>({})
const failed=ref<Set<number>>(new Set())
async function load(attachment:QuestionAttachment){if(!attachment.contentUrl||attachment.type!=='IMAGE'||attachment.renderStatus==='MISSING'||sources.value[attachment.id]||failed.value.has(attachment.id))return;try{const response=await http.get(attachment.contentUrl.replace('/api/v1',''),{responseType:'blob'});sources.value[attachment.id]=URL.createObjectURL(response.data)}catch{failed.value=new Set([...failed.value,attachment.id])}}
watch(pieces,(items)=>{for(const item of items)if(item.attachment)void load(item.attachment)},{immediate:true,deep:true})
onBeforeUnmount(()=>Object.values(sources.value).forEach(value=>URL.revokeObjectURL(value)))
</script>
<template><span class="question-content"><template v-for="(piece,index) in pieces" :key="index"><ScientificText v-if="piece.text" :content="piece.text" /><img v-else-if="piece.attachment&&sources[piece.attachment.id]" class="question-content__image" :src="sources[piece.attachment.id]" :alt="piece.attachment.fileName" /><el-tag v-else type="info" effect="plain" class="question-content__placeholder">{{ piece.attachment?.type==='IMAGE'?'图片附件暂不可用':'附件暂不支持在线显示' }}</el-tag></template></span></template>
<style scoped>.question-content{white-space:pre-wrap;line-height:1.75}.question-content__image{display:block;max-width:min(100%,720px);max-height:420px;object-fit:contain;margin:10px 0;border:1px solid var(--el-border-color-lighter)}.question-content__placeholder{margin:0 4px}</style>
