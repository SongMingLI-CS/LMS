<template>
<section v-if="currentPage === 'manageBooks'">
                            <div class="glass-card rounded-2xl p-4 mb-5 flex flex-col md:flex-row gap-4 md:items-center justify-between">
                                <div class="relative flex-1 max-w-md">
                                    <ion-icon name="search" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"></ion-icon>
                                    <input type="text" v-model="bookSearchQuery" :placeholder="$t('books.searchPh')" class="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl outline-none">
                                </div>
                                <div v-if="currentUser.role!=='user'" class="flex gap-2.5 flex-wrap">
                                    <button @click="openBookModal(null)" class="btn-primary px-4 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5"><ion-icon name="add"></ion-icon>{{ $t('books.addNew') }}</button>
                                    <button @click="modalType='importBooks'; isModalOpen=true" class="px-4 py-2.5 bg-white/80 border border-slate-200 text-slate-600 rounded-xl text-sm font-medium hover:bg-white hover:text-indigo-600 transition-colors flex items-center gap-1.5 backdrop-blur"><ion-icon name="cloud-upload-outline"></ion-icon>{{ $t('books.bulkImport') }}</button>
                                </div>
                            </div>
                            <div class="glass-card rounded-2xl overflow-hidden">
                                <div class="overflow-x-auto">
                                <table class="w-full text-left text-sm">
                                    <thead class="bg-slate-100/70 text-slate-500 uppercase text-xs"><tr><th class="px-6 py-3.5">{{ $t('books.colBook') }}</th><th class="px-6 py-3.5">{{ $t('books.colAuthor') }}</th><th class="px-6 py-3.5">{{ $t('books.colStock') }}</th><th class="px-6 py-3.5 text-right">{{ $t('books.colActions') }}</th></tr></thead>
                                    <tbody class="divide-y divide-slate-100">
                                    <tr v-for="book in filteredBooks" :key="book.id" class="hover:bg-indigo-50/40 transition-colors">
                                        <td class="px-6 py-3.5">
                                            <div class="flex items-center">
                                                <div class="w-10 h-14 bg-slate-100 rounded-md mr-3.5 overflow-hidden ring-1 ring-slate-200 flex-shrink-0"><img :src="book.cover" class="w-full h-full object-cover" @error="hideBrokenImg"></div>
                                                <div class="min-w-0">
                                                    <p class="font-semibold text-slate-800 truncate">{{ book.title }}</p>
                                                    <p v-if="book.category" class="text-[11px] text-slate-400 mt-0.5">{{ book.category }}</p>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="px-6 py-3.5"><p class="text-slate-700">{{ book.author }}</p><p class="text-slate-400 text-xs mt-0.5 font-mono">{{ book.isbn }}</p></td>
                                        <td class="px-6 py-3.5"><span :class="['inline-flex px-2.5 py-1 rounded-full text-xs font-semibold', book.available>0 ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-500']">{{ book.available>0? $t('books.remaining', { n: book.available }) : $t('books.outOfStock') }}</span></td>
                                        <td class="px-6 py-3.5 text-right whitespace-nowrap">
                                            <span class="inline-flex items-center gap-1.5">
                                                <button v-if="book.available>0 && !isBookBorrowedByUser(book)" @click="handleBorrow(book)" class="px-3 py-1.5 rounded-full bg-indigo-50 text-indigo-600 text-xs font-semibold hover:bg-indigo-100 transition-colors">{{ $t('books.doBorrow') }}</button>
                                                <button v-if="book.available===0 && !isBookBorrowedByUser(book) && !isBookReservedByUser(book)" @click="handleReserve(book)" class="px-3 py-1.5 rounded-full bg-amber-50 text-amber-600 text-xs font-semibold hover:bg-amber-100 transition-colors">{{ $t('books.doReserve') }}</button>
                                                <span v-if="isBookBorrowedByUser(book)" class="px-3 py-1.5 rounded-full bg-slate-100 text-slate-500 text-xs font-medium">{{ $t('books.alreadyBorrowed') }}</span>
                                                <template v-if="currentUser.role!=='user'">
                                                    <button @click="openBookModal(book)" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"><ion-icon name="create-outline"></ion-icon></button>
                                                    <button @click="handleDeleteBook(book)" class="w-8 h-8 inline-flex items-center justify-center rounded-lg text-slate-400 hover:text-rose-500 hover:bg-rose-50 transition-colors"><ion-icon name="trash-outline"></ion-icon></button>
                                                </template>
                                            </span>
                                        </td>
                                    </tr>
                                    <tr v-if="filteredBooks.length===0"><td colspan="4" class="px-6 py-14 text-center text-slate-400 text-sm">{{ $t('books.empty') }}</td></tr>
                                    </tbody>
                                </table>
                                </div>
                            </div>
                        </section>
</template>

<script lang="ts">
import { storeToRefs } from 'pinia'
import { useLms } from '../lms'
export default {
  name: 'BooksView',
  setup() {
    const s = useLms()
    const hideBrokenImg = (e: Event) => {
      const el = e.target as HTMLImageElement | null
      if (el) el.style.display = 'none'
    }
    return { ...s, ...storeToRefs(s), hideBrokenImg }
  }
}
</script>
