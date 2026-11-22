/* Original research harness: maps the user's executable privately at runtime.
 * No executable bytes, native tables or extracted fixtures are included. */
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <setjmp.h>
static jmp_buf done;
static unsigned char door[0x200], owner[0x1200], observer_owner[0x1200], creature[0x500], definition[0x700];
static unsigned char world[0x1100], navigation[0x20], tiles[16*16*16], nav_tiles[16*16], frame_storage[256];
static uint16_t player_head;
void *door_record=door, *local_frame=frame_storage+128;
static int sight, nav_writes, notifications, viewer;
static uintptr_t world_vtable[0x400/4];
static unsigned char visibility_tile[16];
static int __attribute__((thiscall)) get_viewer(void *self) {(void)self;return viewer;}
static void * __attribute__((thiscall)) get_tile(void *self,void *point) {(void)self;(void)point;return tiles+(8*16+8)*16;}
static int __attribute__((thiscall)) generic_view(void *self) {(void)self;return 1;}
__attribute__((naked)) static void enter(void) {__asm__("mov door_record,%esi; mov local_frame,%ebp; mov $0x4a64a3,%eax; jmp *%eax");}
void finish_c(void) {longjmp(done,1);}
static void put(void *p,int offset,uint32_t value) {memcpy((char *)p+offset,&value,4);}
static void jump(uintptr_t address,void *destination) {
    unsigned char opcode=0xe9;
    uint32_t displacement=(uintptr_t)destination-address-5;
    memcpy((void *)address,&opcode,1); memcpy((void *)(address+1),&displacement,4);
}
/* World services are controlled inputs/captures, not replacements for discovery predicates. */
static int __attribute__((thiscall)) los(void *self,void *from,void *to,int a,int b) {
    (void)self;(void)from;(void)to;(void)a;(void)b;return sight;
}
static void __attribute__((thiscall)) notify(void *self,void *event) {
    (void)self;(void)event;notifications++;
}
static void __attribute__((thiscall)) refresh(void *self,int x,int y) {(void)self;(void)x;(void)y;}
static void __attribute__((thiscall)) write_nav(void *self,int x,int y,int value) {
    (void)self; nav_tiles[y*16+x]=value; nav_writes++;
}
int main(int argc,char **argv) {
    if(argc!=2) return 2;
    FILE *file = fopen(argv[1], "rb");
    if (!file) return 3;
    fseek(file, 0, SEEK_END);
    long length = ftell(file);
    rewind(file);
    unsigned char *bytes = malloc(length);
    if (!bytes || fread(bytes, 1, length, file) != (size_t)length) return 4;
    fclose(file);

    /* Private PE image mapping. The executable on disk remains untouched. */
    void *image = mmap((void *)0x400000, 0x400000, PROT_READ | PROT_WRITE | PROT_EXEC,
                      MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED_NOREPLACE, -1, 0);
    if (image == MAP_FAILED) { perror("mmap"); return 5; }
    uint32_t pe;
    uint16_t count, optional_size;
    memcpy(&pe, bytes + 0x3c, 4);
    memcpy(&count, bytes + pe + 6, 2);
    memcpy(&optional_size, bytes + pe + 20, 2);
    for (int i = 0; i < count; i++) {
        unsigned char *section = bytes + pe + 24 + optional_size + i * 40;
        uint32_t address, raw, size;
        memcpy(&address, section + 12, 4);
        memcpy(&size, section + 16, 4);
        memcpy(&raw, section + 20, 4);
        if ((uint64_t)raw + size > (uint64_t)length ||
            (uint64_t)address + size > 0x400000) return 6;
        memcpy((char *)image + address, bytes + raw, size);
    }
    free(bytes);

    /* The original initializer builds state categories from native code at runtime. */
    ((void (*)(void))0x47bbe0)();
    jump(0x4a6661,finish_c);
    jump(0x4512f0,los);
    jump(0x517480,notify);
    jump(0x4522a0,refresh);
    jump(0x4d1930,write_nav);
    put((void *)0x6e5050,0,(uintptr_t)world);
    put((void *)0x6ee540,4,(uintptr_t)owner);
    put((void *)0x6ee540,8,(uintptr_t)observer_owner);
    put((void *)0x6ee540,12,(uintptr_t)creature);
    put(owner,4,1); owner[0x10fc]=1;
    put(observer_owner,4,2); observer_owner[0x10fc]=2;
    put(observer_owner,6,3);
    player_head=2;
    put(owner,0x10fd,(uintptr_t)&player_head);
    put(door,4,4); put(door,0x28,1); put(door,0x2a,(uintptr_t)owner); put(door,0xf7,(uintptr_t)definition);
    put(door,0x1a,8*4096+2048); put(door,0x1e,8*4096+2048);
    put(world,0x9e2,(uintptr_t)world); put(world,0x9e6,16); put(world,0x9ea,16); put(world,0x9ee,(uintptr_t)tiles);
    put(world,0x9f2,(uintptr_t)navigation); put(navigation,8,16); put(navigation,16,(uintptr_t)nav_tiles);
    put(local_frame,-0x24,8); put(local_frame,-0x1c,8);
    put(creature,0x28,2); put(creature,0x2a,(uintptr_t)observer_owner); put(creature,0x370,(uintptr_t)definition);
    put(creature,0x32d,1); creature[0x46f]=1;
    int states[]={1,6,76,83,126,136,153,199,201,259};
    int distances[]={0,4096,4*4096-1,4*4096,5*4096};
    unsigned char *tile=tiles+(8*16+8)*16;
    puts("relation,open,blueprint,secret,distance,state,willing,sight,hidden,nav,notifications");
    for(int relation=0;relation<3;relation++)
    for(int open=0;open<2;open++)
    for(int blueprint=0;blueprint<2;blueprint++)
    for(int secret=0;secret<2;secret++)
    for(unsigned d=0;d<sizeof(distances)/sizeof(*distances);d++)
    for(unsigned st=0;st<sizeof(states)/sizeof(*states);st++)
    for(int willing=0;willing<2;willing++)
    for(sight=0;sight<2;sight++) {
        int state=states[st],distance=distances[d];
        creature[0x46f]=willing;
        *(uint16_t *)(observer_owner+4)=relation==0?1:2;
        observer_owner[0x1af]=relation==1;
        put(door,0x10d,open); put(door,0x109,blueprint); definition[0x8b]=secret?0x20:0;
        put(creature,0x1a,8*4096+2048+distance); put(creature,0x1e,8*4096+2048);
        put(creature,0x25e,state); put(creature,0x3f9,0); put(creature,0x3e6,0);
        tile[11]=1; nav_tiles[8*16+8]=0; nav_writes=0; notifications=0;
        if(!setjmp(done)) enter();
        int eligible=state==1||state==6;
        int expected=relation==2&&open&&!blueprint&&secret&&distance<4*4096&&eligible&&willing&&sight;
        if((tile[11]&1)!=!expected||notifications!=expected||nav_writes!=expected) {
            fprintf(stderr,"discovery mismatch: relation=%d open=%d blueprint=%d secret=%d distance=%d state=%d sight=%d hidden=%d notifications=%d navWrites=%d\n",relation,open,blueprint,secret,distance,state,sight,tile[11]&1,notifications,nav_writes);return 7;
        }
        printf("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",relation,open,blueprint,secret,distance,state,willing,sight,tile[11]&1,nav_tiles[8*16+8],notifications);
    }

    /* Closing and ownership refresh retain the shared marker; route encoding reads it live. */
    typedef void (__attribute__((thiscall)) *update_fn)(void *,int);
    update_fn update=(update_fn)0x4a66f0;
    typedef int (*route_fn)(unsigned int,unsigned int);
    route_fn route=(route_fn)0x4c8bd0;
    *(uint32_t *)0x6ec9d0=1; *(uint32_t *)0x6ec9d4=1; *(uint32_t *)0x6ec9e0=0;
    definition[0x8b]=0x20; put(door,0x111,0);
    tile[11]=1; owner[0x10fc]=1;
    update(door,1);
    if(nav_tiles[8*16+8]!=0x12||route(0,nav_tiles[8*16+8])!=3) return 8;
    *(uint32_t *)0x6ec9d0=2;
    if(route(0,nav_tiles[8*16+8])!=0) return 9;
    /* An already discovered door stays known after closing and owner changes. */
    tile[11]=0; put(door,0x10d,0); update(door,1);
    if(nav_tiles[8*16+8]!=0x10||route(0,nav_tiles[8*16+8])!=1) return 10;
    owner[0x10fc]=3; update(door,1);
    if(nav_tiles[8*16+8]!=0x30||tile[11]!=0) return 11;
    /* Original view predicate: a foreign owner cannot draw an undiscovered secret door. */
    jump(0x4b34f0,generic_view);
    world_vtable[0x264/4]=(uintptr_t)get_viewer;
    world_vtable[0x1d4/4]=(uintptr_t)get_tile;
    put(world,0,(uintptr_t)world_vtable);
    visibility_tile[11]=1; put(door,0x6c,(uintptr_t)visibility_tile); door[0xf3]=1;
    typedef int (__attribute__((thiscall)) *draw_fn)(void *);
    draw_fn draw=(draw_fn)0x4a6e10;
    viewer=1; tile[11]=1;
    if(draw(door)!=1) return 12;
    viewer=2;
    if(draw(door)!=0) return 13;
    observer_owner[0x1af]=1;
    if(draw(door)!=0) return 14;
    tile[11]=0;
    if(draw(door)!=1) return 15;
    fprintf(stderr,"Verified 4800 native discovery cases, persistent route encoding and owner/foreign/allied view predicates.\n");
    return 0;
}
