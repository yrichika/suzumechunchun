import { Wrapper } from '@vue/test-utils'
/**
 * Vue wrapper type for testing
 */
export type VueFlexTypeWrapper = Wrapper<Vue & { [key: string]: any }>
