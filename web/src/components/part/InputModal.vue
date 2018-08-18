<template lang='pug'>
.vue-input-modal
    .modal-card
        header.modal-card-head
            p.modal-card-title post
        section.modal-card-body
            b-field(label='タグを追加')
                b-taginput(v-model='modalItem.tags' :data='labels' field='ja' autocomplete icon='label' placeholder='Add a tag' @typing='getFilteredTags')
            b-field(label='メッセージ')
                b-input(type='textarea' v-model='modalItem.message' maxlength='100')
        footer.arai-modal-card-foot
            button.button(type='button' @click='$parent.close()') close
            button.button.is-primary() post
    button.modal-close.is-large(aria-label='close')
</template>

<script lang='ts'>
import { Vue, Component, Prop } from 'vue-property-decorator';
import { modalItemOptions } from '@/components/entry/Index.vue';
import labelData from '@/resources/JSON/labels.json';
import * as moji from 'moji';

export interface labelLang {
    en: string;
    ja: string;
}
/**
 * Vue Component
 */
@Component
export default class InputModal extends Vue {
    @Prop({ type: Boolean, default: () => false })
    protected isActive!: boolean;
    @Prop({ type: Object, default: () => {}, required: true })
    public modalItem!: modalItemOptions;

    protected labels = labelData;

    protected getFilteredTags(text: string) {
        this.labels = labelData.filter((option: labelLang) => {
            // 入力テキストが全てひらがなの場合
            if (moji(text).filter('HG').toString() == text) {
                const existence = option.ja
                    .toString()
                    .toLowerCase()
                    .indexOf(moji(text).convert('HG', 'KK').toString()) >= 0;
                if (existence) {
                    return existence;
                } else {
                    return option.ja
                        .toString()
                        .toLowerCase()
                        .indexOf(text.toLowerCase()) >= 0;
                }
            }
            // 入力テキストが全てカタカナの場合
            if (moji(text).filter('KK').toString() == text) {
                const existence = option.ja
                    .toString()
                    .toLowerCase()
                    .indexOf(moji(text).convert('KK', 'HG').toString()) >= 0;
                if (existence) {
                    return existence;
                } else {
                    return option.ja
                        .toString()
                        .toLowerCase()
                        .indexOf(text.toLowerCase()) >= 0;
                }
            }
        });
    }
    protected closeModal(): void {
        this.$emit('update:isActive', false);
    }
}
</script>

<style lang='sass' scoped>
@import 'entry/variable'

.vue-input-modal
</style>
