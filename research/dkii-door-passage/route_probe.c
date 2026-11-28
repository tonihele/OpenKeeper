/* Original research harness. Loads the user's executable at runtime.
 * Contains no copied executable bytes or extracted game fixtures.
 * Use run_probe.py to check the executable version before running. */
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <setjmp.h>
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
    typedef int (*route_fn)(unsigned int, unsigned int);
    route_fn route=(route_fn)0x4c8bd0;
    *(uint32_t *)0x6ec9d0=1; /* moving keeper's synthetic slot */
    puts("ownerSlot,hidden,locked,routeBit17,routeBit19,cost");
    for(int owner=1;owner<=2;owner++)
    for(int hidden=0;hidden<2;hidden++)
    for(int locked=0;locked<2;locked++)
    for(int bit17=0;bit17<2;bit17++)
    for(int bit19=0;bit19<2;bit19++) {
        *(uint32_t *)0x6ec9d4=bit17;
        *(uint32_t *)0x6ec9e0=bit19;
        unsigned int tile=(owner<<4)|(hidden?2:0)|(locked?4:0);
        int cost=route(0,tile);
        int expected=(hidden&&owner!=1)||(bit19&&(owner!=1||locked))||
                     (!bit19&&!bit17&&owner==1&&locked)?0:(hidden?3:1);
        if(cost!=expected) {fprintf(stderr,"native route mismatch\n");return 7;}
        printf("%d,%d,%d,%d,%d,%d\n",owner,hidden,locked,bit17,bit19,cost);
    }
    return 0;
}
