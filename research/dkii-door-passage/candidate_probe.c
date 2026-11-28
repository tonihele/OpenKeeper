/* Original research harness. Loads the user's executable at runtime.
 * Contains no copied executable bytes or extracted game fixtures.
 * Use run_probe.py to check the executable version before running. */
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <setjmp.h>
static jmp_buf return_point;
volatile int candidate;
void *door_record, *local_frame;
__attribute__((naked)) void enter_native(void){__asm__("mov door_record,%esi; mov local_frame,%ebp; mov $0x4a5e85,%eax; jmp *%eax");}
void finish_c(void){longjmp(return_point,1);}
__attribute__((naked)) void finished(void){__asm__("mov %ebx,candidate; jmp finish_c");}

static unsigned char door[0x200];
static uint16_t tile_head;

static void put(void *p, int offset, uint32_t value) {
    memcpy((char *)p + offset, &value, sizeof(value));
}

int main(int argc, char **argv) {
    if (argc != 2) return 2;
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


    /* Execute the original same-tile candidate block from door tick.
     * Its entry/exit bounds are checked against IDA and native disassembly.
     * We stop before the neighboring-tile scan: this is not the complete tick. */
    void *endpoint=(void *)0x4a5f37;
    unsigned char jump[5]={0xe9};
    uint32_t displacement=(uintptr_t)finished-(uintptr_t)endpoint-5;
    memcpy(jump+1,&displacement,4);memcpy(endpoint,jump,5);
    static unsigned char creature[0x500],keeper[0x400],other_keeper[0x1200],creature_definition[0x700];
    static unsigned char locals[256];
    put((void *)0x6ee540,4,(uintptr_t)creature);
    put((void *)0x6ee540,12,(uintptr_t)other_keeper);
    put(door,0x2a,(uintptr_t)keeper);
    put(keeper,4,2);
    other_keeper[0x10fc]=1;
    put(creature,0x370,(uintptr_t)creature_definition);
    *(uint16_t *)0x6f2548=0;
    tile_head=1;
    void *frame=locals+128;
    put(frame,-0x2c,(uintptr_t)&tile_head);
    puts("relation,lock,worker,excluded,opens");
    for(int relation=0;relation<3;relation++)
    for(int lock=0;lock<4;lock++)
    for(int worker=0;worker<2;worker++)
    for(int excluded=0;excluded<2;excluded++) {
        put(creature,0x28,relation==0?2:3);
        keeper[0x1af]=relation==1;
        put(door,0x111,lock);
        creature_definition[0x686]=worker;
        put(creature,0x3e6,excluded?0x200000:0);
        candidate=-1;
        if(!setjmp(return_point)) {
            door_record=door; local_frame=frame; enter_native();
        }
        int expected=relation!=2&&!excluded&&(lock==0||(lock==1&&!worker)||(lock==2&&worker));
        if(candidate!=expected) {fprintf(stderr,"mismatch %d %d %d %d got=%d\n",relation,lock,worker,excluded,candidate);return 7;}
        printf("%d,%d,%d,%d,%d\n",relation,lock,worker,excluded,candidate);
    }
    return 0;
}
