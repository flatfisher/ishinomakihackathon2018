<template lang='pug'>
.vue-input-modal
    .modal(:class='{ "is-active": isActive }')
        .modal-background
        .modal-card
            header.modal-card-head
                p.modal-card-title 入力画面
            section.modal-card-body
                b-field(label='タグ')
                    b-taginput(v-model='modalItem.tags' :data='labels' field='ja' autocomplete icon='label' placeholder='Add a tag' @typing='getFilteredTags')
                b-field(label='メッセージ')
                    b-input(type='textarea' v-model='modalItem.message' maxlength='100')
            footer.arai-modal-card-foot
                button.button(type='button' @click='closeModal') close
                button.button.is-primary(@click='store') post
        button.modal-close.is-large(aria-label='close' @click='closeModal')
</template>

<script lang='ts'>
import { Vue, Component, Prop } from 'vue-property-decorator';
import { modalItemOptions } from '@/components/entry/Index.vue';
import labelData from '@/resources/JSON/labels.json';
import * as moji from 'moji';
import firebaseApp, { db } from '@/scripts/firebase/firebaseApp';
import { CommonError } from '@/scripts/model/error/CommonError';

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
    protected store() {
        const selectedLabel = this.modalItem.tags.map(item => item.en);
        this.storeData('hoge', this.modalItem.message, this.modalItem.pinPosition, selectedLabel);
        this.closeModal();
    }

    protected storeData(name: string, message: string, location: {lat: number,lng: number} , tags: string[]) {
        try {
            if (location.lat == 0 || location.lng == 0) {
                throw new CommonError('invalid location value');
            }
            const dataRef = db.collection('items').doc();
            dataRef.set({
                name,
                msg: message,
                location,
                tags
            });
        } catch (e) {
            if (e instanceof CommonError) {
                this.$dialog.alert({ message: e.message });
            } else {
                this.$dialog.alert({ message: e.response.data.error.message });
            }
        }
    }
}
</script>

<style lang='sass' scoped>
@import 'entry/variable'

.vue-input-modal
</style>
