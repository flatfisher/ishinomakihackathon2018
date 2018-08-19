<template lang='pug'>
.vue-index
    .reactive-title {{ reactiveTitle() }}
    common-hero.is-dark.is-small.medium(:title='title' :subtitle='subtitle')

    .columns.is-gapless
        .column.is-9
            .map(ref='map')
        .column.is-3
            .manager.box.tile.is-vertical.spread-out
                .latlng
                    p.title.is-4 緯度 :
                    p.subtitle {{ (pinPosition != null) ? pinPosition.lat : 'null' }}
                    hr
                    p.title.is-4 経度 :
                    p.subtitle {{ (pinPosition != null) ? pinPosition.lng : 'null' }}
                    hr
                b-tooltip.is-danger(label='まずはマップにピンをセットしよう' position='is-top' size='is-large' multilined=true :active='isButtonActive == false')
                    button.button.is-large.is-success.is-fullwidth(:disabled='isButtonActive == false' @click='isComponentModalActive = !isComponentModalActive')
                        b-icon(icon='pen')
                        span メッセージ入力
    b-modal(:active.sync='isComponentModalActive')
        input-modal(:modalItem.sync='modalItem')
</template>

<script lang='ts'>
import { Vue, Component } from 'vue-property-decorator';
import Buefy from 'buefy';
import RootVue from '@/components/base/RootVue';
import { CommonError } from '@/scripts/model/error/CommonError';
import { LatLng } from '@/scripts/model/map/LatLng';
import { labelLang } from '@/components/part/InputModal.vue';
import CommonNavbar from '@/components/common/CommonNavbar.vue';
import CommonHero from '@/components/common/CommonHero.vue';
import InputModal from '@/components/part/InputModal.vue';
import { aswait } from '@/scripts/util/AsyncTimeout';
import VueFire from 'vuefire';

Vue.use(VueFire);
Vue.use(Buefy);

export interface modalItemOptions {
    message: string;
    tags: labelLang[];
    pinPosition: LatLng;
}
/**
 * Vue Component
 */
@Component({
    components: {
        CommonHero, InputModal
    }
})
export default class Index extends RootVue {
    public title = 'Arai';
    public subtitle = 'AR × AI';

    protected map: google.maps.Map | null = null;
    protected pinPosition: LatLng | null = null;
    protected currentPosition: LatLng | null = null;
    protected marker: google.maps.Marker | null = null;

    protected isComponentModalActive = false;
    protected isButtonActive = false;
    protected modalItem: modalItemOptions = {
        message: '',
        tags: [],
        pinPosition: {lat: 0, lng: 0}
    }

    protected async getCurrentPosition(): Promise<void> {
        if (navigator.geolocation) {
            await navigator.geolocation.getCurrentPosition(position => {
                const currentLatLng = { lat: position.coords.latitude, lng: position.coords.longitude };
                this.currentPosition = currentLatLng;
                this.initMapComponent();
            }, error => {
                switch(error.code) {
                    case 1: //PERMISSION_DENIED
                        this.$dialog.alert("位置情報の利用が許可されていません");
                        break;
                    case 2: //POSITION_UNAVAILABLE
                        this.$dialog.alert("現在位置が取得できませんでした");
                        break;
                    case 3: //TIMEOUT
                        this.$dialog.alert("タイムアウトになりました");
                        break;
                    default:
                        this.$dialog.alert("その他のエラー(エラーコード:"+error.code+")");
                        break;
                }
            })
        } else {
            this.$dialog.alert('この端末では位置情報が取得できません');
        }
    }
    protected async initMapComponent(): Promise<void> {
        try {
            if (this.currentPosition == null) {
                throw new CommonError('位置情報が取れないためマップを表示できません');
            }
            const canvas = (this.$refs['map'] as HTMLElement);
            const latlng = new google.maps.LatLng(this.currentPosition.lat, this.currentPosition.lng);
            // const latlng = new google.maps.LatLng(38, 140);
            const mapOptions: google.maps.MapOptions = {
                zoom: 13,
                center: {
                    lat: latlng.lat(),
                    lng: latlng.lng()
                }
            }
            this.map = new google.maps.Map(canvas, mapOptions);
            this.map.addListener('click', e => {
                this.pinPosition = { lat: e.latLng.lat(), lng: e.latLng.lng() };
                this.setMarker(e.latLng);
                this.modalItem.pinPosition = { lat: e.latLng.lat(), lng: e.latLng.lng() };
                this.isButtonActive = true;
            });
        } catch (e) {
            this.$dialog.alert(e.message);
        }
    }

    protected setMarker(latlng: google.maps.LatLng) {
        if (this.marker != null) {
            this.marker.setMap(null);
        }
        this.marker = new google.maps.Marker({
            map: this.map as google.maps.Map,
            position: latlng
        });
    }

    protected mounted(): void {
        this.getCurrentPosition();
    }
}
</script>

<style lang='sass'>
@import 'entry/all'

$hero-height-medium: 8rem
.vue-index
    .medium
        height: $hero-height-medium

    .map, .manager
        height: calc(100vh - #{$hero-height-medium})

    .spread-out
        justify-content: space-evenly

    .tooltip
        display: flex
</style>
