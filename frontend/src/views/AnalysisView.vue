<template>
<section v-if="currentPage === 'analysis'">
                            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                                <div class="glass-card rounded-2xl p-6">
                                    <div class="flex justify-between items-center mb-5">
                                        <h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-indigo-500 to-fuchsia-500 inline-block"></span>热门图书 Top 5</h3>
                                        <button @click="handleExportAnalysis('popular-books','pop.xlsx')" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="download-outline"></ion-icon></button>
                                    </div>
                                    <ul class="space-y-4">
                                        <li v-if="popularBooks.length===0" class="text-slate-400 text-sm text-center py-6">暂无数据</li>
                                        <li v-for="(book, idx) in popularBooks" :key="idx" class="text-sm">
                                            <div class="flex items-center justify-between mb-1.5 gap-3">
                                                <span class="flex items-center gap-2.5 min-w-0">
                                                    <span class="w-5 h-5 rounded-md text-[11px] font-bold flex items-center justify-center flex-shrink-0" :class="idx===0 ? 'bg-gradient-to-br from-amber-300 to-orange-400 text-white shadow-sm' : 'bg-indigo-50 text-indigo-600'">{{ idx+1 }}</span>
                                                    <span class="text-slate-700 font-medium truncate">{{ book.title }}</span>
                                                </span>
                                                <span class="text-slate-500 font-mono text-xs flex-shrink-0">{{ book.borrowCount }} 次</span>
                                            </div>
                                            <div class="w-full h-2 bg-slate-200/70 rounded-full overflow-hidden"><div class="h-full rounded-full bg-gradient-to-r from-indigo-500 to-fuchsia-500" :style="{width: (book.borrowCount / (popularBooks[0]?.borrowCount || 1) * 100) + '%'}"></div></div>
                                        </li>
                                    </ul>
                                </div>

                                <div class="glass-card rounded-2xl p-6">
                                    <div class="flex justify-between items-center mb-4"><h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-violet-500 to-fuchsia-500 inline-block"></span>图书分类分布</h3><button @click="handleExportAnalysis('categories','cats.xlsx')" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="download-outline"></ion-icon></button></div>
                                    <div class="flex flex-col items-center">
                                        <div class="w-40 h-40 rounded-full border-4 border-white shadow-[0_12px_32px_-12px_rgba(99,102,241,0.45)] mb-5" :style="{ background: pieChartStyle }"></div>
                                        <div class="flex flex-wrap justify-center gap-2">
                                            <div v-if="categoryStats.length===0" class="text-slate-400 text-xs py-2">暂无数据</div>
                                            <span v-for="(cat, i) in categoryStats" :key="i" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/60 border border-slate-200/70 text-xs text-slate-600"><span class="w-2 h-2 rounded-full" :style="{background: chartColors[i%chartColors.length]}"></span>{{ cat.category }} · {{ cat.count }} 册</span>
                                        </div>
                                    </div>
                                </div>

                                <div class="glass-card rounded-2xl p-6 lg:col-span-2">
                                    <div class="flex justify-between items-center mb-6"><h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-sky-400 to-indigo-500 inline-block"></span>借阅高峰时段</h3><button @click="handleExportAnalysis('peak-times','peak.xlsx')" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="download-outline"></ion-icon></button></div>
                                    <div class="h-52 flex items-end justify-between gap-2 pt-8">
                                        <div v-if="peakTimeStats.length===0" class="w-full text-center text-slate-400 text-sm">暂无数据</div>
                                        <div v-for="slot in peakTimeStats" :key="slot.hourSlot" class="flex-1 flex flex-col items-center group h-full">
                                            <div class="relative mt-auto w-full max-w-[26px] rounded-t-lg bg-gradient-to-t from-indigo-500 via-violet-500 to-fuchsia-400 opacity-75 group-hover:opacity-100 transition-opacity flex-shrink-0" :style="{ height: (slot.count * 10 + 5) + 'px', maxHeight: '100%' }">
                                                <span class="absolute -top-5 left-1/2 -translate-x-1/2 text-[11px] font-bold text-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">{{ slot.count }}</span>
                                            </div>
                                            <span class="text-[10px] text-slate-400 mt-2">{{ slot.hourSlot }}</span>
                                        </div>
                                    </div>
                                </div>

                                <div class="glass-card rounded-2xl p-6">
                                    <div class="flex justify-between items-center mb-4"><h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-amber-400 to-rose-400 inline-block"></span>滞销图书 Top 10</h3><button @click="handleExportAnalysis('stagnant-books','stagnant.xlsx')" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="download-outline"></ion-icon></button></div>
                                    <ul class="text-sm">
                                        <li v-for="(b,i) in stagnantBooks" :key="i" class="py-2.5 flex justify-between items-center gap-2 border-b border-slate-100 last:border-0">
                                            <span class="flex items-center gap-2.5 min-w-0"><span class="w-5 h-5 rounded-md bg-slate-100 text-slate-500 text-[11px] font-bold flex items-center justify-center flex-shrink-0">{{ i+1 }}</span><span class="text-slate-600 truncate">{{ b.title }}</span></span>
                                            <span class="text-[11px] text-slate-400 bg-white/70 border border-slate-200/70 px-2.5 py-0.5 rounded-full whitespace-nowrap">{{ b.category }}</span>
                                        </li>
                                        <li v-if="stagnantBooks.length===0" class="py-6 text-center text-slate-400">暂无数据</li>
                                    </ul>
                                </div>
                                <div class="glass-card rounded-2xl p-6">
                                    <div class="flex justify-between items-center mb-4"><h3 class="font-bold text-slate-900 flex items-center gap-2"><span class="w-1 h-4 rounded bg-gradient-to-b from-emerald-400 to-teal-500 inline-block"></span>活跃读者 Top 5</h3><button @click="handleExportAnalysis('active-users','active.xlsx')" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="download-outline"></ion-icon></button></div>
                                    <ul class="text-sm">
                                        <li v-for="(u,i) in activeUsers" :key="i" class="py-2.5 flex justify-between items-center gap-2 border-b border-slate-100 last:border-0">
                                            <span class="flex items-center gap-2.5 min-w-0"><span class="w-5 h-5 rounded-md bg-gradient-to-br from-indigo-400 to-fuchsia-500 text-white text-[11px] font-bold flex items-center justify-center flex-shrink-0">{{ i+1 }}</span><span class="text-slate-700 font-medium truncate">{{ u.name }}</span></span>
                                            <span class="text-indigo-600 font-bold font-mono text-xs flex-shrink-0">{{ u.borrowCount }} 次</span>
                                        </li>
                                        <li v-if="activeUsers.length===0" class="py-6 text-center text-slate-400">暂无数据</li>
                                    </ul>
                                </div>
                            </div>
                        </section>
</template>

<script>
import { inject } from 'vue'
export default {
  name: 'AnalysisView',
  setup() {
    return { ...inject('lms') }
  }
}
</script>