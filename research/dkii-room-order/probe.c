#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
static int tick;
int get_tick(void) { return tick; }
void record(int x,int y) { printf(" (%d,%d)",x,y); }
__attribute__((naked)) void build_hook(void) { __asm__("pushal; push %esi; push 0x38(%esp); call record; add $8,%esp; popal; push $0x4c7782; ret"); }
__attribute__((naked)) void sell_hook(void) { __asm__("pushal; push %esi; push %ebp; call record; add $8,%esp; popal; push $0x4c7292; ret"); }
void jump(uint32_t a,void *f) { *(uint8_t*)a=0xe9; *(int32_t*)(a+1)=(uintptr_t)f-a-5; }
static void put(void *p,int o,int v) { memcpy((char*)p+o,&v,4); }
void run(int sell,int sx,int sy,int ex,int ey) {
 unsigned char player[0x1100]={0};
 int off=sell?0xe97:0x1c2; player[off]=5;
 put(player,off+1,sx);put(player,off+5,sy);put(player,off+9,ex);put(player,off+13,ey);put(player,off+21,0);
 printf("%s (%d,%d)->(%d,%d)\n",sell?"sell":"build",sx,sy,ex,ey);
 for(tick=0;tick<=4;tick++) { printf(" tick %d:",tick); void *f=(void*)(sell?0x4c7160:0x4c7380); __asm__ volatile("mov %0,%%ecx; call *%1"::"r"(player),"r"(f):"eax","ecx","edx","memory","cc"); puts(""); }
}
int main(int argc,char **argv) {
 FILE *f=fopen(argv[1],"rb"); fseek(f,0,SEEK_END); long len=ftell(f);rewind(f);unsigned char *b=malloc(len);fread(b,1,len,f);fclose(f);
 mmap((void*)0x400000,0x400000,7,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED,-1,0);
 uint32_t pe=*(uint32_t*)(b+0x3c);int n=*(uint16_t*)(b+pe+6),opt=*(uint16_t*)(b+pe+20);
 for(int i=0;i<n;i++){unsigned char *s=b+pe+24+opt+i*40; memcpy((void*)(0x400000+*(uint32_t*)(s+12)),b+*(uint32_t*)(s+20),*(uint32_t*)(s+16));}
 static unsigned char world[0xb00]; put(world,0x9e6,100);put(world,0x9ea,100);*(void**)0x6e5054=world;
 jump(0x517490,get_tick);jump(0x4c74c4,build_hook);jump(0x4c727b,sell_hook);
 run(0,10,10,12,12);run(0,12,12,10,10);run(0,10,12,12,10);run(0,12,10,10,12);run(1,10,10,12,12);
}
