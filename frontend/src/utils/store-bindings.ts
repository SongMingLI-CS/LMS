import { storeToRefs, type StoreGeneric } from 'pinia'

/** storeToRefs 的返回类型（Pinia 未直接导出该类型名，这里从函数签名推导） */
type RefsOf<SS extends StoreGeneric> = ReturnType<typeof storeToRefs<SS>>

/**
 * 去掉 Pinia 内部保留前缀（$ / _）后的 state + getters（仍为 Ref）。
 */
type PublicRefs<SS extends StoreGeneric> = Omit<RefsOf<SS>, `$${string}` | `_${string}`>

/**
 * 去掉 Pinia 内部保留前缀后的 store 节点（保留 actions 的签名）。
 */
type PublicStore<SS extends StoreGeneric> = Omit<SS, `$${string}` | `_${string}`>

/**
 * setup() 返回值的类型：state/getter 以 Ref 暴露（模板中自动解包），
 * action 保持原函数签名。state/getter 优先于 store 根节点上的同名属性。
 */
export type StoreBindings<SS extends StoreGeneric> = PublicRefs<SS> &
    Omit<PublicStore<SS>, keyof PublicRefs<SS>>

/**
 * 包装 store 动作：以箭头函数转发调用，使模板中直接调用时不把组件实例当作 this，
 * 避免 Pinia wrappedAction 读取 this.$id 触发 Vue 的 reserved-prefix 告警。
 */
function wrapAction(key: string, fn: (...args: unknown[]) => unknown) {
    const wrapper = (...args: unknown[]) => fn(...args)
    Object.defineProperty(wrapper, 'name', { value: `store$${key}` })
    return wrapper
}

/**
 * 组装组件 setup 所需的“公开绑定”：
 * - state / getters 以 ref 暴露（保持响应式）
 * - 函数、响应式对象等原样暴露
 *
 * 必须剔除 Pinia 以 $ / _ 开头的内部属性（$state、$patch、$id…），
 * 否则 setup() return 会触发 Vue “reserved prefix”告警。
 */
export function bindStore<SS extends StoreGeneric>(s: SS): StoreBindings<SS> {
    const refs = storeToRefs(s) as Record<string, unknown>
    for (const key of Object.keys(refs)) {
        if (key.startsWith('$') || key.startsWith('_')) delete refs[key]
    }
    const store = s as unknown as Record<string, unknown>
    const extras: Record<string, unknown> = {}
    for (const key of Object.keys(store)) {
        if (key.startsWith('$') || key.startsWith('_')) continue
        if (key in refs) continue
        const value = store[key]
        // 动作需要显式包裹：模板里直接调用 store 动作时 this 会指向组件实例，
        // Pinia 的 wrappedAction 随即读取 this.$id，触发 Vue
        // “Property "$id" was accessed during render but is not defined on instance.” 告警。
        // 用箭头函数转发可让 this 保持 undefined，Pinia 会自动改用 store 自身。
        extras[key] = typeof value === 'function'
            ? wrapAction(key, value as (...a: unknown[]) => unknown)
            : value
    }
    return { ...refs, ...extras } as StoreBindings<SS>
}
