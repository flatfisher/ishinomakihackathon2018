import { Vue } from 'vue-property-decorator';
import { aswait } from '@/scripts/util/AsyncTimeout';

export default abstract class RootVue extends Vue {
    public abstract title: string;
    public abstract subtitle: string;
    public loading: boolean = false;

    public async executeLoading(func: () => Promise<void>): Promise<void> {
        await aswait(0, async () => {
            this.$data.loading = true;
        });

        await func().catch(async (e) => {
            // throwし直し
            await aswait(0, async () => {
                this.$data.loading = false;
            });

            throw e;
        });

        await aswait(0, async () => {
            this.$data.loading = false;
        });
    }

    protected reactiveTitle(): void {
        document.title = `${this.subtitle} | ${this.title}`;
    }
}
