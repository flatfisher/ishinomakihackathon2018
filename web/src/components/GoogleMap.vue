<template lang="pug">
.vue-google-map
    p hoge
    .map-container
        GmapMap(:center='center' :zoom='zoom' style='width:60vw;  height: 60vh;')
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import GmapMap, { gmapApi } from 'vue2-google-maps';
import { Position } from '@/model/Position';
declare const google: any;

@Component
export default class GoogleMap extends Vue {
    private center: Position = { lat: 51.555, lng: -0.155 };
    private zoom: number = 12;
}
</script>

<style scoped lang="sass">
</style>
